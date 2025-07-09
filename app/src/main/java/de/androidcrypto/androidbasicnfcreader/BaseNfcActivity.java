package de.androidcrypto.androidbasicnfcreader;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by gyg on 2017/6/5.
 */

public class BaseNfcActivity extends AppCompatActivity {

    protected NfcAdapter mNfcAdapter;//nfc适配器对象
    protected PendingIntent mPendingIntent;//延迟Intent
    protected Tag mTag;//nfc标签对象

    //启动activity,界面可见时
    @Override
    protected void onStart() {
        super.onStart();
        NfcManager nfcManager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        mNfcAdapter = nfcManager.getDefaultAdapter();
        if (mNfcAdapter == null) {//判断设备是否支持NFC功能
            Toast.makeText(this, "设备不支持NFC功能!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mNfcAdapter.isEnabled()) {//判断设备NFC功能是否打开
            Toast.makeText(this, "请到系统设置中打开NFC功能!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            startActivity(intent);
            return;
        }
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()), PendingIntent.FLAG_MUTABLE);//创建PendingIntent对象,当检测到一个Tag标签就会执行此Intent
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);//获取到Tag标签对象
        String[] techList = mTag.getTechList();
        System.out.println("标签支持的tachnology类型：");
        for (String tech : techList) {
            System.out.println(tech);
        }
    }

    //页面获取到焦点
    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);//打开前台发布系统，使页面优于其它nfc处理.当检测到一个Tag标签就会执行mPendingItent
        }
    }

    //页面失去焦点
    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);//关闭前台发布系统
        }
    }

}
