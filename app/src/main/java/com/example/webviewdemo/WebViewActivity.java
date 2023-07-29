package com.example.webviewdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import java.util.HashMap;
import java.util.Map;

public class WebViewActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private WebView webView;
    private TextureView textureView;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取传递的URL
        String url = getIntent().getStringExtra("url");
        Uri uri = Uri.parse(url);

        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("Referer", url);

        // 是否开启沉浸式
        boolean useImmerse = "1".equals(uri.getQueryParameter("immer"));
        boolean useLightStatusBar = "1".equals(uri.getQueryParameter("lightbar"));
        String bgVideo = uri.getQueryParameter("bgvideo");

        setContentView(R.layout.activity_web_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        webView = findViewById(R.id.webview);
        webView.setBackgroundColor(0x00000000);

        if (null != bgVideo) {
            textureView = findViewById(R.id.textureView);
            textureView.setSurfaceTextureListener(this);

            Uri videoUri = Uri.parse(bgVideo);
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(this, videoUri);
                mediaPlayer.setLooping(true);
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    adjustAspectRatio(mp.getVideoWidth(), mp.getVideoHeight());
                }
            });
        }


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
//        webView.loadUrl("https://vring.kuyin123.com/friend/d0b23f78c27e9078?videoId=1203865234498912256&immer=1&bgvideo=https%3A%2F%2Fvracloss.kuyin123.com%2F11W2MYCO%2Frescloud1%2F688312df2748437b8b2123aa235fae06.mp4%3Frestype%3D2%26a%3Dd0b23f78c27e9078%26resid%3D1203865234498912256%26subtype%3D3#/login");
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
//            webView.goBack();
//            return true;
//        }
//        return super.onKeyDown(keyCode, event);
//    }

    @Override
    public void onBackPressed() {
        webviewGoBack();
    }

    @Override
    public boolean onSupportNavigateUp() {
        webviewGoBack();
        return true;
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

    private void webviewGoBack() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    private void webviewGoBackWithJS() {
        webView.evaluateJavascript("typeof (window.KuYin && window.KuYin.ine && window.KuYin.ine.goBack) === 'function'", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                boolean isMethodExists = "true".equals(value);
                if (isMethodExists) {
                    webView.loadUrl("javascript:KuYin.ine.goBack()");
                } else if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        Surface surface = new Surface(surfaceTexture);
        mediaPlayer.setSurface(surface);
        surface.release();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }

    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();

        float scaleX = (float) viewWidth / videoWidth;
        float scaleY = (float) viewHeight / videoHeight;
        float pivotX = viewWidth * 0.5f;
        float pivotY = viewHeight * 0.5f;

        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, pivotX, pivotY);
        textureView.setTransform(matrix);
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
        public void backToClient() {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}