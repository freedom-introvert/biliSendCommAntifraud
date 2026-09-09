package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.annotation.SuppressLint;

public class PostCommentHookByMaster extends PostCommentHook {

    public PostCommentHookByMaster() {
    }



    @Override
    protected String getBiliCallClassName(ClassLoader classLoader) {
        return "com.bilibili.okretro.call.BiliCall";
    }

    @Override
    protected String getBiliCall_body_MethodName() {
        return "body";
    }

    @Override
    protected String getBiliCall_request_MethodName() {
        return "request";
    }

    @SuppressLint("SdCardPath")
    @Override
    protected String[] getCookieDBFilePaths() {
        return new String[]{
                "/data/data/tv.danmaku.bili/app_webview_tv.danmaku.bili_web/Default/Cookies",
                "/data/data/tv.danmaku.bili/app_webview_tv.danmaku.bili/Default/Cookies"};
    }


}
