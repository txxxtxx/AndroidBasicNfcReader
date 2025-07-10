package de.androidcrypto.androidbasicnfcreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;


/**
 * //todo:--------------M1卡读写-------------
 **/
public class MainActivity extends BaseNfcActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button).setOnClickListener(this);
        findViewById(R.id.button2).setOnClickListener(this);
        findViewById(R.id.button3).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button) {
            startActivity(new Intent(this, NdefActivity.class));
        } else if (v.getId() == R.id.button2) {//读写MifareClassic格式
            startActivity(new Intent(this, MifareClassicActivity.class));
        } else if (v.getId() == R.id.button3) {//读写MifareClassic格式
            startActivity(new Intent(this, MifareUltralightActivity.class));
        }
    }
}
