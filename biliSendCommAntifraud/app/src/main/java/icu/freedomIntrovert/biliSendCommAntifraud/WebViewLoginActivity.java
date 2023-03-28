package icu.freedomIntrovert.biliSendCommAntifraud;

import static android.view.KeyEvent.KEYCODE_BACK;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class WebViewLoginActivity extends AppCompatActivity {
    WebView webView;
    Context context;
    SharedPreferences sp_config;
    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view_login);
        progressBar = findViewById(R.id.progressBar);
        context = this;
        sp_config = getSharedPreferences("config", Context.MODE_PRIVATE);
        webView = findViewById(R.id.web_view);
        webView.loadUrl("https://www.bilibili.com");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager cookieManager = CookieManager.getInstance();
                String cookieStr = cookieManager.getCookie(url);
                if (cookieStr != null) {
                    Log.i("Cookies", "Cookies = " + cookieStr);
                    if (cookieStr.contains("bili_jct=")){
                        new AlertDialog.Builder(context).setTitle("获取到cookie!")
                                .setMessage(cookieStr)
                                .setPositiveButton("设置并返回", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        sp_config.edit().putString("cookie",cookieStr).commit();
                                        finish();
                                    }
                                })
                                .setNegativeButton("不使用该cookie",new VoidDialogInterfaceOnClickListener())
                                .setNeutralButton("清除浏览器cookie", (dialog, which) -> {
                                    cookieManager.removeAllCookies(value -> {
                                        
                                    });
                                    finish();
                                })
                                .show();

                    }
                }
                super.onPageFinished(view, url);
            }
        });
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
                if (newProgress == 100){
                    progressBar.setProgress(0);
                }
            }
        });
    }
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KEYCODE_BACK) && webView.canGoBack()) {
            if (webView.canGoBack()){
                webView.goBack();
                return true;
            } else {
                finish();
                return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //工具栏返回上一级按钮
        if (item.getItemId() == 16908332) {
            finish();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.clearHistory();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}