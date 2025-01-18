package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

public class PostCommentHookByMaster extends PostCommentHook {

    public PostCommentHookByMaster() {
    }

   /* @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {

        *//*XposedHelpers.findAndHookMethod("com.bilibili.okretro.ServiceGenerator", classLoader, "createService", java.lang.Class.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Object result = param.getResult();
                if (result != null) {
                    Class<?> resultClass = result.getClass();
                    for (Class<?> classInterface : resultClass.getInterfaces()) {
                        if (classInterface.getCanonicalName().equals("com.bilibili.app.comm.comment2.model.BiliCommentApiService")) {
                            XposedHelpers.findAndHookMethod(resultClass, "postComment", java.util.Map.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    super.beforeHookedMethod(param);
                                    Map<String, String> arrayMap = (Map<String, String>) param.args[0];
                                    currentComment.set(arrayMap.get("message"));
                                    currentOid.set(arrayMap.get("oid"));
                                    currentAreaType.set(arrayMap.get("type"));
                                    //   currentPictures.set(arrayMap.get("pictures"));
                                }
                            });
                        }
                    }
                }
            }
        });*//*



    }*/


    @Override
    public void hook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {

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

    @Override
    protected String[] getCookieDBFilePaths() {
        return new String[]{
                "/data/data/tv.danmaku.bili/app_webview_tv.danmaku.bili_web/Default/Cookies",
                "/data/data/tv.danmaku.bili/app_webview_tv.danmaku.bili/Default/Cookies"};
    }


}
