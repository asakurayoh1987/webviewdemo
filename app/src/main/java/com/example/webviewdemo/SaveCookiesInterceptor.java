package com.example.webviewdemo;

import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.Interceptor;
import okhttp3.Response;
import java.io.IOException;
import java.util.HashSet;

public class SaveCookiesInterceptor implements Interceptor {
    private SharedPreferences sharedPreferences;

    // 构造函数，传入Context来初始化SharedPreferences
    public SaveCookiesInterceptor(Context context) {
        this.sharedPreferences = context.getSharedPreferences("CookiePrefsFile", Context.MODE_PRIVATE);
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());

        if (!originalResponse.headers("Set-Cookie").isEmpty()) {
            HashSet<String> cookies = new HashSet<>();

            for (String header : originalResponse.headers("Set-Cookie")) {
                cookies.add(header);
            }

            // 保存cookies到SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet("cookies", cookies);
            editor.apply();
        }

        return originalResponse;
    }
}
