package de.androidcrypto.androidbasicnfcreader;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NfcVActivity extends AppCompatActivity {
    private static final String TAG = "NfcVActivity";
    private NfcAdapter nfcAdapter;
    private TextView logTextView;
    private Handler mainHandler;
    private Tag tag;
    private Boolean isProcess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcv);

        logTextView = findViewById(R.id.logTextView);
        mainHandler = new Handler(Looper.getMainLooper());

        // 获取 NFC 适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // 检查设备是否支持 NFC
        if (nfcAdapter == null) {
            showToast("此设备不支持 NFC");
            finish();
            return;
        }

        // 检查 NFC 是否已启用
        if (!nfcAdapter.isEnabled()) {
            showToast("请在设置中启用 NFC");
        }

        TextView textAddress = findViewById(R.id.edit_text_address);
        TextView textValue = findViewById(R.id.edit_text_value);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tag == null) {
                    showToast("连接已断开");
                    return;
                } else if (isProcess) {
                    showToast("正在处理其他任务，请稍后再试");
                    return;
                } else if (TextUtils.isEmpty(textAddress.getText())) {
                    showToast("输入地址");
                    return;
                } else if (TextUtils.isEmpty(textValue.getText())) {
                    showToast("输入值");
                    return;
                }
                int address = Integer.parseInt(textAddress.getText().toString());
                String message = textValue.getText().toString();
                new Thread(() -> writeToTag(address, message)).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 启用前台调度系统，确保本应用优先处理 NFC 标签
        enableNfcForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 禁用前台调度系统
        disableNfcForegroundDispatch();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        logMessage("检测到 NFC 标签");
        handleNfcIntent(intent);
    }

    private void enableNfcForegroundDispatch() {
        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_MUTABLE);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    private void disableNfcForegroundDispatch() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void handleNfcIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                // 检查标签是否支持 NfcV 技术
                String[] techList = tag.getTechList();
                boolean hasNfcV = false;
                for (String tech : techList) {
                    if (tech.equals(NfcV.class.getName())) {
                        hasNfcV = true;
                        break;
                    }
                }

                if (hasNfcV) {
                    logMessage("发现 NFCV (ISO 15693) 标签");
                    if (isProcess) {
                        showToast("操作太频繁，请稍后再试");
                        return;
                    }
                    // 在后台线程处理 NFC 操作，避免阻塞 UI
                    new Thread(() -> handleNfcVTag(0)).start();
                } else {
                    logMessage("发现不支持的标签类型");
                    showToast("不支持的标签类型，需要 NFCV (ISO 15693) 标签");
                }
            }
        }
    }

    private static final int MAX_RETRIES = 3;

    private void handleNfcVTag(int retries) {
        if (retries >= MAX_RETRIES) return;
        try (NfcV nfcv = NfcV.get(tag)) {
            // 连接标签
            nfcv.connect();
            isProcess = true;
            logMessage("已连接到标签");

            NfcVUtil nfcVUtil = new NfcVUtil(nfcv);

            // 读取标签 UID
            String uid = nfcVUtil.getUID();
            if (!TextUtils.isEmpty(uid)) {
                logMessage("标签 UID: " + uid);
            }

            // 读取标签信息
            logMessage("标签信息: " + bytesToHex(nfcVUtil.getInfoRmation()));
            logMessage("标签容量: " + nfcVUtil.getBlockNumber() + " 块, 每块 " + nfcVUtil.getOneBlockSize() + " 字节");
            logMessage("系统信息: " + nfcVUtil.getAFI());

            // 读取数据块
            int blockAddress = 0; // 从块 0 开始
            int blockCount = nfcVUtil.getBlockNumber();   // 读取 4 个块
            for (int i = blockAddress; i < blockCount; i++) {
                String blockMsg = nfcVUtil.readOneBlock(blockAddress + i);
                logMessage("地址 " + (blockAddress + i) * 4 + " 数据: " + blockMsg);
            }

        } catch (IOException e) {
            logMessage("NFC 操作错误: " + e.getMessage());
            e.printStackTrace();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> handleNfcVTag(retries + 1), 500);
        } finally {
            isProcess = false;
            logMessage("已断开与标签的连接");
        }
    }

    private void writeToTag(int address, String message) {
        NfcV nfcv = NfcV.get(tag);
        try {
            nfcv.connect();
            isProcess = true;
            logMessage("已连接到标签");

            NfcVUtil nfcVUtil = new NfcVUtil(nfcv);

            byte[] cmd = new byte[4];
            int position = address / 4;
            int index = address % 4;
            cmd[index] = (byte) Integer.parseInt(message);
            nfcVUtil.writeBlock(position, cmd);

            showToast("写入成功");

        } catch (IOException e) {
            Log.e(TAG, "写入标签失败", e);
            showToast("写入标签失败: " + e.getMessage());
        } finally {
            try {
                nfcv.close();
                isProcess = false;
                logMessage("已断开与标签的连接");
            } catch (IOException e) {
                Log.e(TAG, "关闭连接失败", e);
            }
        }
    }

    private byte[] readTagUID(NfcV nfcv) throws IOException {
        // ISO 15693 标准命令: 读取 UID
        byte[] cmd = new byte[]{0, 43}; // 标志位 + 命令码 0x2B (Get UID)
        byte[] response = nfcv.transceive(cmd);

        if (response != null && response.length > 0 && response[0] == 0) {
            // 成功响应: 状态码 (0x00) + UID 数据
            return Arrays.copyOfRange(response, 1, response.length);
        }
        return null;
    }

    private byte[] getTagInfo(NfcV nfcv) throws IOException {
        // ISO 15693 标准命令: 获取标签信息
//        byte[] cmd = new byte[]{0, 42}; // 标志位 + 命令码 0x2A (Get System Information)
        byte[] cmd = new byte[10];
        cmd[0] = (byte) 0x22;
        cmd[1] = (byte) 0x2B;
        NfcVUtil nfcVUtil = new NfcVUtil(nfcv);
        System.arraycopy(nfcVUtil.getID(), 0, cmd, 2, nfcVUtil.getID().length);
        byte[] response = nfcv.transceive(cmd);

        if (response != null && response.length > 0 && response[0] == 0) {
            // 成功响应: 状态码 (0x00) + 标签信息
            return Arrays.copyOfRange(response, 1, response.length);
        }
        return null;
    }

    private void parseTagInfo(byte[] info) {
        if (info == null || info.length < 7) return;

        // 解析标签信息 (格式取决于具体标签类型)
        // 通常格式: 块数, 块大小, 访问条件, 可选信息...
        int blockCount = info[0] & 0xFF;
        int blockSize = (info[1] & 0xFF) + 1; // 块大小 = 值 + 1
        int systemInfo = info[2] & 0xFF;

        logMessage("标签容量: " + blockCount + " 块, 每块 " + blockSize + " 字节");
        logMessage("系统信息: " + Integer.toHexString(systemInfo));
    }

    private byte[] readBlock(NfcV nfcv, int blockAddress) throws IOException {
        // ISO 15693 标准命令: 读取单个块
        byte[] cmd = new byte[]{0, 32, (byte) blockAddress}; // 标志位 + 命令码 0x20 + 块地址
        byte[] response = nfcv.transceive(cmd);

        if (response != null && response.length > 0 && response[0] == 0) {
            // 成功响应: 状态码 (0x00) + 块数据
            return Arrays.copyOfRange(response, 1, response.length);
        }
        return null;
    }

    private boolean writeBlock(NfcV nfcv, int blockAddress, byte[] data) throws IOException {
        if (data == null || data.length != 4) { // 假设块大小为 4 字节
            return false;
        }

        // ISO 15693 标准命令: 写入单个块
        byte[] cmd = new byte[6];
        cmd[0] = 0x00;           // 标志位
        cmd[1] = (byte) 0x21;     // 命令码 0x21 (Write Single Block)
        cmd[2] = (byte) blockAddress; // 块地址
        System.arraycopy(data, 0, cmd, 3, 4); // 数据

        byte[] response = nfcv.transceive(cmd);
        // 成功响应: 0x00
        return response != null && response.length == 1 && response[0] == 0x00;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b));
        }
        return result.toString();
    }

    private void logMessage(final String message) {
        mainHandler.post(() -> {
            logTextView.append(message + "\n");
            // 滚动到底部
//            logTextView.setSelection(logTextView.getText().length());
        });
    }

    private void showToast(final String message) {
        mainHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}