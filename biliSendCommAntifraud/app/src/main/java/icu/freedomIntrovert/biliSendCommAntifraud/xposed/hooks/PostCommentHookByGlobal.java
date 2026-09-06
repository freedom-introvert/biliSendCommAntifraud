package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.annotation.SuppressLint;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.Reflect;

import java.lang.reflect.Method;
import java.util.Map;


public class PostCommentHookByGlobal extends PostCommentHook {



    @Override
    protected String getBiliCallClassName(ClassLoader classLoader) {
        //换了一种更灵活的方式获取BiliCal的类名，以非混淆类站脚
        try {
            Class<?> clazz = Reflect.findClass("com.bilibili.app.comm.comment2.model.BiliCommentApiService", classLoader);
            Method postComment;
            try {
                // 6.4.0: postComment(Map, x-bili-gaia-vtoken)
                postComment = clazz.getDeclaredMethod("postComment", Map.class, String.class);
            } catch (NoSuchMethodException ignored) {
                postComment = clazz.getDeclaredMethod("postComment", Map.class);
            }
            return postComment.getReturnType().getCanonicalName();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        //return "com.bilibili.okretro.call.a";
        //return "rx1.a";
    }

    @Override
    protected Object extractResponseBody(Object response) {
        return HostCallAdapter.responseBodyOf(response, getBiliCall_body_MethodName());
    }

    @Override
    protected Object extractRequest(Object call) {
        return HostCallAdapter.requestOf(call, getBiliCall_request_MethodName());
    }

    @Override
    protected java.util.Map<String, String> extractFormFields(Object request) {
        return HostCallAdapter.formFieldsOf(request);
    }

    @Override
    protected boolean isCommentAddCall(Object call) {
        try {
            String url = HostCallAdapter.urlOf(extractRequest(call));
            return url == null || HostCallAdapter.isReplyAddUrl(url);
        } catch (RuntimeException e) {
            return true;
        }
    }

    @Override
    protected boolean skipNonZeroHostAction() {
        // 6.4.0 success_action=1 means "no celebration card", not 精选.
        return false;
    }

    @Override
    protected String getBiliCall_body_MethodName() {
        return "a";
    }

    @Override
    protected String getBiliCall_request_MethodName() {
        return "h";
    }

    @SuppressLint("SdCardPath")
    @Override
    protected String[] getCookieDBFilePaths() {
        return new String[]{
                "/data/data/com.bilibili.app.in/app_webview_com.bilibili.app.in/Default/Cookies",
                "/data/data/com.bilibili.app.in/app_webview_com.bilibili.app.in_web/Default/Cookies"};
    }
}
