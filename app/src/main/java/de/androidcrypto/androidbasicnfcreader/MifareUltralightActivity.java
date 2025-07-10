package de.androidcrypto.androidbasicnfcreader;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MifareUltralightActivity extends AppCompatActivity {
    private static final String TAG = "MifareActivity";
    private NfcAdapter nfcAdapter;
    private Tag tag;
    private TextView textView;
    private EditText editTextAddress, editTextValue;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcaactivity);
        textView = findViewById(R.id.textView);
        editTextAddress = findViewById(R.id.edit_text_address);
        editTextValue = findViewById(R.id.edit_text_value);
        button = findViewById(R.id.button);

        // 获取NFC适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "设备不支持NFC", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "请开启NFC", Toast.LENGTH_SHORT).show();
        }

        button.setOnClickListener(view -> {
            if (tag == null) {
                Toast.makeText(MifareUltralightActivity.this, "NFC已断开", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(editTextAddress.getText())) {
                Toast.makeText(MifareUltralightActivity.this, "输入地址", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(editTextValue.getText())) {
                Toast.makeText(MifareUltralightActivity.this, "输入值", Toast.LENGTH_SHORT).show();
            }
            writeToTag(tag, Integer.parseInt(editTextAddress.getText().toString()), editTextValue.getText().toString());
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 设置NFC前台调度系统
        setupForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 停止NFC前台调度系统
        stopForegroundDispatch();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        handleNfcIntent(intent);
    }

    private void setupForegroundDispatch() {
        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_MUTABLE // Android 12+ 需要使用FLAG_MUTABLE
        );
        nfcAdapter.enableForegroundDispatch(
                this,
//                nfcAdapter.createPendingIntent(this, 0, intent, 0)
                pendingIntent,
                null,
                null
        );
    }

    private void stopForegroundDispatch() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void handleNfcIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                readMifareUltralight(tag);
            }
        }
    }

    private void readMifareUltralight(Tag tag) {
        new Thread(() -> {
            MifareUltralight mifare = MifareUltralight.get(tag);
            try {
                mifare.connect();

                // 读取UID
                byte[] uid = tag.getId();
                String uidString = bytesToHex(uid);

                // 读取标签类型
                int type = mifare.getType();
                String typeString = getTypeString(type);

                // 读取页数（实际容量可能因标签而异）
                int pageCount = 20; // Mifare Ultralight通常有16页
                StringBuilder dataBuilder = new StringBuilder();

                // 读取数据（从第4页开始，前3页通常是只读的厂商数据）
                for (int i = 0; i < pageCount; i += 4) {
                    try {
                        byte[] pageData = mifare.readPages(i);
                        for (int j = 0; j < pageData.length; j += 4) {
                            byte[] sub = Arrays.copyOfRange(pageData, j, j + 4);
                            int iplusj = i * 4 + j;
                            if (iplusj < 10) {
                                dataBuilder.append("地址   ").append(iplusj).append(": ");
                            } else if (iplusj > 99) {
                                dataBuilder.append("地址 ").append(iplusj).append(": ");
                            } else {
                                dataBuilder.append("地址  ").append(iplusj).append(": ");
                            }
                            dataBuilder.append(bytesToHex(sub)).append("\n");
//                            dataBuilder.append(bytesToHex(pageData)).append("\n");
                        }

                        // 尝试将数据解析为文本
                        String textData = new String(pageData, StandardCharsets.UTF_8).trim();
                        if (!textData.isEmpty()) {
                            dataBuilder.append("文本: ").append(textData).append("\n\n");
                        } else {
                            dataBuilder.append("\n");
                        }
                    } catch (Exception e) {
                        dataBuilder.append("地址 ").append(i * 4).append(": 读取失败\n");
                    }
                }

                final String result = "UID: " + uidString + "\n" +
                        "类型: " + typeString + "\n\n" +
                        "数据:\n" + dataBuilder.toString();

                runOnUiThread(() -> {
                    textView.setText(result);
                    Toast.makeText(MifareUltralightActivity.this, "读取成功", Toast.LENGTH_SHORT).show();
                });

            } catch (IOException e) {
                Log.e(TAG, "读取标签失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(MifareUltralightActivity.this, "读取标签失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                try {
                    mifare.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭连接失败", e);
                }
            }
        }).start();
    }

    private String getTypeString(int type) {
        switch (type) {
            case MifareUltralight.TYPE_ULTRALIGHT:
                return "MIFARE Ultralight";
            case MifareUltralight.TYPE_ULTRALIGHT_C:
                return "MIFARE Ultralight C";
            default:
                return "未知类型 (" + type + ")";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b));
        }
        return result.toString();
    }

    // 写入示例方法（需要在适当的地方调用）
    private void writeToTag(Tag tag, int address, String message) {
        new Thread(() -> {
            MifareUltralight mifare = MifareUltralight.get(tag);
            try {
                mifare.connect();

                // 准备数据（每4字节一页）
                byte[] data = message.getBytes(StandardCharsets.UTF_8);
                int page = address / 4;
                int index = address % 4;
                byte[] pageData = mifare.readPages(page);
                int intMessage = Integer.parseInt(message);
                byte hexByte = (byte) (intMessage & 0xFF);
//                pageData[index] = hexByte;
                pageData[index] = (byte) intMessage;
                byte[] sub = Arrays.copyOfRange(pageData, 0, 4);
                String hexString = bytesToHex(sub);
//                System.arraycopy(data, 0, pageData, index, 1);

                byte[] writeData =
                        hexToByte("03000000");
//                        Arrays.copyOfRange(pageData,0,4);

                mifare.writePage(page, sub);

                runOnUiThread(() -> {
                    Toast.makeText(MifareUltralightActivity.this, "写入成功", Toast.LENGTH_SHORT).show();
                });

            } catch (IOException e) {
                Log.e(TAG, "写入标签失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(MifareUltralightActivity.this, "写入标签失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                try {
                    mifare.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭连接失败", e);
                }
            }
        }).start();
    }

    /**
     * 将十六进制字符串转换为字节数组
     * @param hex 十六进制字符串（如："0123ABCD"）
     * @return 对应的字节数组
     * @throws IllegalArgumentException 如果输入不是有效的十六进制字符串
     */
    public static byte[] hexToByte(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }

        // 检查长度是否为偶数
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("十六进制字符串长度必须为偶数");
        }

        // 验证是否为合法十六进制字符
        for (char c : hex.toCharArray()) {
            if (!((c >= '0' && c <= '9') ||
                    (c >= 'A' && c <= 'F') ||
                    (c >= 'a' && c <= 'f'))) {
                throw new IllegalArgumentException("无效的十六进制字符: " + c);
            }
        }

        // 转换逻辑
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            result[i / 2] = (byte) ((high << 4) | low);
        }
        return result;
    }

    // 辅助方法：将16进制字符串转换为字节数组
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private void writeToTag(Tag tag, String message) {
        new Thread(() -> {
            MifareUltralight mifare = MifareUltralight.get(tag);
            try {
                mifare.connect();

                // 准备数据（每4字节一页）
                byte[] data = message.getBytes(StandardCharsets.UTF_8);
                int page = 4; // 从第4页开始写入

                for (int i = 0; i < data.length; i += 4) {
                    byte[] pageData = new byte[4];
                    System.arraycopy(data, i, pageData, 0, Math.min(4, data.length - i));

                    mifare.writePage(page++, pageData);
                }

                runOnUiThread(() -> {
                    Toast.makeText(MifareUltralightActivity.this, "写入成功", Toast.LENGTH_SHORT).show();
                });

            } catch (IOException e) {
                Log.e(TAG, "写入标签失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(MifareUltralightActivity.this, "写入标签失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                try {
                    mifare.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭连接失败", e);
                }
            }
        }).start();
    }
}