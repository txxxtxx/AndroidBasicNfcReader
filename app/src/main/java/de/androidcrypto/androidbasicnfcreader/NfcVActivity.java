package de.androidcrypto.androidbasicnfcreader;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NfcVActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private TextView logTextView;
    private Handler mainHandler;

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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        logMessage("检测到 NFC 标签");
        handleNfcIntent(intent);
    }

    private void enableNfcForegroundDispatch() {
        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent, android.app.PendingIntent.FLAG_MUTABLE);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    private void disableNfcForegroundDispatch() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void handleNfcIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
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
                    // 在后台线程处理 NFC 操作，避免阻塞 UI
                    new Thread(() -> handleNfcVTag(tag)).start();
                } else {
                    logMessage("发现不支持的标签类型");
                    showToast("不支持的标签类型，需要 NFCV (ISO 15693) 标签");
                }
            }
        }
    }

    private void handleNfcVTag(Tag tag) {
        NfcV nfcv = NfcV.get(tag);
        try {
            // 连接标签
            nfcv.connect();
            logMessage("已连接到标签");

            // 读取标签 UID
            byte[] uid = readTagUID(nfcv);
            if (uid != null) {
                logMessage("标签 UID: " + bytesToHex(uid));
            }

            // 读取标签信息
            byte[] info = getTagInfo(nfcv);
            if (info != null) {
                logMessage("标签信息: " + bytesToHex(info));
                // 解析标签信息 (取决于具体标签类型)
                parseTagInfo(info);
            }

            // 读取数据块
            int blockAddress = 0; // 从块 0 开始
            int blockCount = 4;   // 读取 4 个块
            for (int i = 0; i < blockCount; i++) {
                byte[] blockData = readBlock(nfcv, blockAddress + i);
                if (blockData != null) {
                    logMessage("块 " + (blockAddress + i) + " 数据: " + bytesToHex(blockData));

                    // 如果是第一个块，尝试写入数据
                    if (i == 0) {
                        // 准备要写入的数据 (示例: "Hello")
                        byte[] dataToWrite = "Hello".getBytes(StandardCharsets.UTF_8);
                        // 确保数据长度不超过块大小 (通常为 4 或 8 字节)
                        byte[] paddedData = Arrays.copyOf(dataToWrite, 4);

                        boolean writeSuccess = writeBlock(nfcv, blockAddress + i, paddedData);
                        if (writeSuccess) {
                            logMessage("成功写入块 " + (blockAddress + i));

                            // 验证写入结果
                            byte[] verifiedData = readBlock(nfcv, blockAddress + i);
                            if (verifiedData != null) {
                                logMessage("验证写入: " + bytesToHex(verifiedData));
                            }
                        } else {
                            logMessage("写入块 " + (blockAddress + i) + " 失败");
                        }
                    }
                }
            }

        } catch (IOException e) {
            logMessage("NFC 操作错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // 关闭连接
                nfcv.close();
                logMessage("已断开与标签的连接");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] readTagUID(NfcV nfcv) throws IOException {
        // ISO 15693 标准命令: 读取 UID
        byte[] cmd = new byte[]{0x00, (byte)0x2B}; // 标志位 + 命令码 0x2B (Get UID)
        byte[] response = nfcv.transceive(cmd);

        if (response != null && response.length > 0 && response[0] == 0x00) {
            // 成功响应: 状态码 (0x00) + UID 数据
            return Arrays.copyOfRange(response, 1, response.length);
        }
        return null;
    }

    private byte[] getTagInfo(NfcV nfcv) throws IOException {
        // ISO 15693 标准命令: 获取标签信息
        byte[] cmd = new byte[]{0x00, (byte)0x2A}; // 标志位 + 命令码 0x2A (Get System Information)
        byte[] response = nfcv.transceive(cmd);

        if (response != null && response.length > 0 && response[0] == 0x00) {
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
        byte[] cmd = new byte[]{0x00, (byte)0x20, (byte)blockAddress}; // 标志位 + 命令码 0x20 + 块地址
        byte[] response = nfcv.transceive(cmd);

        if (response != null && response.length > 0 && response[0] == 0x00) {
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
        cmd[1] = (byte)0x21;     // 命令码 0x21 (Write Single Block)
        cmd[2] = (byte)blockAddress; // 块地址
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