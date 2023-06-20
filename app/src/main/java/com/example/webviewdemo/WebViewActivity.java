package com.example.webviewdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WebViewActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取传递的URL
        String url = getIntent().getStringExtra("url");
        Uri uri = Uri.parse(url);

        Map<String, String> extraHeaders = new HashMap<String, String>();
        extraHeaders.put("Referer", url);

        // 是否开启沉浸式
        boolean useImmerse = "1".equals(uri.getQueryParameter("immer"));
        boolean useLightStatusBar = "1".equals(uri.getQueryParameter("lightbar"));

        setContentView(R.layout.activity_web_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        webView = findViewById(R.id.webview);

        if (useImmerse) {
            // 设置全屏展示，状态栏颜色为透明
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                window.setStatusBarColor(Color.TRANSPARENT);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // 调整webview的位置，使其与状态栏重合
                int statusBarHeight = getStatusBarHeight();
                Log.d("DEBUG", "status bar height:" + statusBarHeight);
                webView.setPadding(0, statusBarHeight, 0, 0);
            }

            // 隐藏导航栏
            toolbar.setVisibility(View.GONE);
        }

        // 设置状态栏图标为深色
        if (useLightStatusBar) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 开启debug功能
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // 启用JavaScript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // 禁用缓存
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // javascript方法注入
        WebViewActivity.WebViewJsInject inject = new WebViewActivity.WebViewJsInject();
        webView.addJavascriptInterface(inject, "KuYinExt");
        webView.addJavascriptInterface(inject, "JsInterface");

        // 在WebView中加载网页
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url, extraHeaders);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                String title = view.getTitle();
                getSupportActionBar().setTitle(title);
            }
        });

        webView.loadUrl(url);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        } else {
            finish();
            return true;
        }
    }

    // 获取状态栏高度
    private int getStatusBarHeight() {
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    public class WebViewJsInject {
        @JavascriptInterface
        public void closeWindow() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    finish();  // 关闭当前的 WebView 所在的 Activity
                }
            });
        }

        @JavascriptInterface
        public void goBack(String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    finish();  // 关闭当前的 WebView 所在的 Activity
                }
            });
        }
    }
}