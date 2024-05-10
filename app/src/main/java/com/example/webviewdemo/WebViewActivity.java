package com.example.webviewdemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

public class WebViewActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private WebView webView;
    private TextureView textureView;
    private MediaPlayer mediaPlayer;

    private ValueCallback<Uri[]> mFilePathCallback;
    private final static int FILECHOOSER_RESULTCODE = 1;

    private boolean shouldAllowUnload = false;

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.d("WebView", "Trim Memory Level: " + level);
    }

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
        boolean noCache = "1".equals(uri.getQueryParameter("nocache"));
        boolean inspect = "1".equals(uri.getQueryParameter("inspect"));

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
            // 隐藏导航栏
            toolbar.setVisibility(View.GONE);

            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);

            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                webView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @NonNull
                    @Override
                    public WindowInsets onApplyWindowInsets(@NonNull View view, @NonNull WindowInsets windowInsets) {
                        int bottomInset = windowInsets.getSystemWindowInsetBottom();
                        view.setPadding(0, 0, 0, 0);
                        return windowInsets.consumeSystemWindowInsets();
                    }
                });
            }
        }


        // 设置状态栏图标为深色
        if (useLightStatusBar) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }


        // 开启debug功能
        WebView.setWebContentsDebuggingEnabled(true);


        // 启用JavaScript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webSettings.setLoadWithOverviewMode(true);  // 缩放至屏幕的大小
        webSettings.setUseWideViewPort(true);  // 将图片调整到适合 WebView 的大小

//        webSettings.setBuiltInZoomControls(false);
//        webSettings.setSupportZoom(false);

        // 禁用缓存
        if (noCache) {
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }

        webSettings.setMediaPlaybackRequiresUserGesture(true);

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
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d("WebView", "Page loading started: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("WebView", "Page loading finished: " + url);
                String title = view.getTitle();
                getSupportActionBar().setTitle(title);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.e("WebView", "Error loading page: " + error.getDescription());
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                try {
                    URL url = new URL(request.getUrl().toString());
                    String method = request.getMethod();

                    // 构建 JSON 对象
                    JSONObject json = new JSONObject();
                    json.put("URL", request.getUrl().toString());
                    json.put("Method", method);

                    if(!inspect) {
                        Log.d("WebView_Request", json.toString());
                        return null;
                    } else {
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestProperty("Accept-Encoding", "gzip");
                        connection.setInstanceFollowRedirects(true);
                        connection.connect();

                        int responseCode = connection.getResponseCode();

                        Map<String, List<String>> responseHeaders = connection.getHeaderFields();

                        InputStream inputStream = connection.getInputStream();
                        String contentType = connection.getContentType();
                        if (contentType == null) {
                            contentType = "text/html; charset=utf-8";
                        }
                        String encoding = connection.getContentEncoding();
                        if (encoding == null) {
                            encoding = "utf-8"; // 设置默认编码
                        }

                        json.put("Status Code", responseCode);
                        json.put("Content-Type", contentType);
                        json.put("Content-Encoding", encoding);

                        if (responseHeaders.containsKey("Content-Length")) {
                            json.put("Content-Length", responseHeaders.get("Content-Length").get(0));
                        }

                        // 使用 Log.d 打印 JSON 字符串
                        Log.d("WebView_Request", json.toString());

                        // 检查内容是否为 gzip 压缩
                        if ("gzip".equalsIgnoreCase(encoding)) {
                            // 创建 GZIPInputStream 解压数据
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = gzipInputStream.read(buffer)) != -1) {
                                byteArrayOutputStream.write(buffer, 0, len);
                            }
                            gzipInputStream.close();
                            inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

                            // 设置正确的 Content-Length
                            int contentLength = byteArrayOutputStream.size();
                            responseHeaders.put("Content-Length", Arrays.asList(String.valueOf(contentLength)));

                            // 移除 Content-Encoding，因为内容不再是压缩的
                            responseHeaders.remove("Content-Encoding");
                            encoding = "utf-8";
                        }

                        return new WebResourceResponse(contentType, encoding, inputStream);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("WebView_Console", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                // 确保没有其他文件选择对话框已经打开
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

                // 创建Intent以选择文件
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILECHOOSER_RESULTCODE);
                } catch (ActivityNotFoundException e) {
                    mFilePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl(url);
//        webView.loadUrl("https://vring.kuyin123.com/friend/d0b23f78c27e9078?videoId=1203865234498912256&immer=1&bgvideo=https%3A%2F%2Fvracloss.kuyin123.com%2F11W2MYCO%2Frescloud1%2F688312df2748437b8b2123aa235fae06.mp4%3Frestype%3D2%26a%3Dd0b23f78c27e9078%26resid%3D1203865234498912256%26subtype%3D3#/login");
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
    public void onBackPressed() {
        super.onBackPressed();
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
//            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
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

        // 清除所有的Cookies
        CookieManager cookieManager = CookieManager.getInstance();

        String url = ".diyring.cc";
        String cookies = cookieManager.getCookie(url);

        // 解析cookies字符串，选择性保存
        Map<String, String> savedCookies = new HashMap<>();
        if (cookies != null) {
            String[] cookieArray = cookies.split(";");
            for (String cookie : cookieArray) {
                String[] cookieKV = cookie.split("=");
                savedCookies.put(cookieKV[0], cookieKV[1]);
            }
        }

        // 清除所有Cookies
        cookieManager.removeAllCookies(null);

        // 将需要保存的cookies重新设置回WebView
        for (Map.Entry<String, String> entry : savedCookies.entrySet()) {
            cookieManager.setCookie(url, entry.getKey() + "=" + entry.getValue());
        }

        cookieManager.flush();

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (mFilePathCallback != null) {
                Uri[] results = null;

                // 检查响应是否成功
                if (resultCode == Activity.RESULT_OK) {
                    if (intent == null) {
                        // 如果没有intent，则可能使用了相机
                        // 这里可以处理相机拍摄的图片
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }

                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
            }
        }
    }
}