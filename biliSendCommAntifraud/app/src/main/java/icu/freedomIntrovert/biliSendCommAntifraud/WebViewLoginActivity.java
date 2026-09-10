package icu.freedomIntrovert.biliSendCommAntifraud;

import static android.view.KeyEvent.KEYCODE_BACK;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class WebViewLoginActivity extends AppCompatActivity {
    WebView webView;
    public Context context;
    Config config;
    ProgressBar progressBar;

    public static final int RESULT_CODE_SUCCEED = 1;
    public static final int RESULT_CODE_CANCELED = 2;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view_login);
        progressBar = findViewById(R.id.progressBar);
        context = this;
        config = Config.getInstance(context);
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
                if (url.equals("https://www.bilibili.com/") && cookieStr != null && !isFinishing()) {
                    Log.i("Cookies", "Url = " + url);
                    Log.i("Cookies", "Cookies = " + cookieStr);
                    if (cookieStr.contains("bili_jct=")){
                        new AlertDialog.Builder(context).setTitle("获取到cookie!")
                                .setMessage(cookieStr)
                                .setPositiveButton("选择此cookie", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent();
                                        intent.putExtra("cookie",cookieStr);
                                        WebViewLoginActivity.this.setResult(RESULT_CODE_SUCCEED,intent);
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