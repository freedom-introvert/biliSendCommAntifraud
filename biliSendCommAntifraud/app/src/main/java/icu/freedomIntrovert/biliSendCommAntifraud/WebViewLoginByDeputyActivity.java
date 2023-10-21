package icu.freedomIntrovert.biliSendCommAntifraud;

public class WebViewLoginByDeputyActivity extends WebViewLoginActivity {
    @Override
    protected void onCookieSet(String cookie) {
        config.setDeputyCookie(cookie);
    }
}
