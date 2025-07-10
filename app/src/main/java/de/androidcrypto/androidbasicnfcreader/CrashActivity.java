package de.androidcrypto.androidbasicnfcreader;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CrashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);

        TextView errorMessageView = findViewById(R.id.error_message);
        String errorMessage = getIntent().getStringExtra("error_message");
        errorMessageView.setText(errorMessage);

        findViewById(R.id.restart_button).setOnClickListener(v -> restartApp());
        findViewById(R.id.quit_button).setOnClickListener(v -> finishAndRemoveTask());
    }

    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
        finish();
    }
}