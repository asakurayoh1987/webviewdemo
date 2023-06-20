package com.example.webviewdemo;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;


import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText urlInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化输入框和确认按钮
        urlInput = findViewById(R.id.url_input);
        Button confirmButton = findViewById(R.id.confirm_button);
        ImageButton clearButton = findViewById(R.id.clear_button);
        ImageButton qrCodeButton = findViewById(R.id.qr_code_button);

        confirmButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);
        qrCodeButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.clear_button) {
            urlInput.setText("");
        } else if (id == R.id.confirm_button) {
            String url = urlInput.getText().toString();

            Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
            intent.putExtra("url", url);
            startActivity(intent);
        } else if (id == R.id.qr_code_button) {
            // 启动二维码扫描器
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setCaptureActivity(CustomCaptureActivity.class);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("请对准二维码进行扫描");
            integrator.setBeepEnabled(true);
            integrator.setCameraId(0);
            integrator.initiateScan();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 处理二维码扫描结果
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String url = result.getContents();
                urlInput.setText(url);
            }
        }
    }
}

