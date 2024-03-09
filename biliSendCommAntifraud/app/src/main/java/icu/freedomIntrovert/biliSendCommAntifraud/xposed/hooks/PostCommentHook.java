package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliApiService;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.VideoInfo;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.ServiceGenerator;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;

public class PostCommentHook extends BaseHook {
    AtomicReference<Context> currentContext;
    AtomicReference<String> currentOid;
    AtomicReference<String> currentDynId;
    AtomicReference<String> currentAreaType;
    AtomicReference<String> currentComment;
    AtomicReference<String> currentPictures;

    public PostCommentHook() {
        currentContext = new AtomicReference<>();
        currentOid = new AtomicReference<>();
        currentDynId = new AtomicReference<>();
        currentAreaType = new AtomicReference<>();
        currentComment = new AtomicReference<>();
        currentPictures = new AtomicReference<>();
    }

    @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {
        //获取哔哩动态的ID。很多版本都是这样，不做特定版本适配
        XposedHelpers.findAndHookMethod("com.bilibili.lib.ui.ComposeActivity", classLoader, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Method getIntentMethod = param.thisObject.getClass().getMethod("getIntent");
                Intent intent = (Intent) getIntentMethod.invoke(param.thisObject);
                Bundle fragment_args = intent.getExtras().getBundle("fragment_args");
                XposedBridge.log("动态ID:"+fragment_args.getString("oid"));
                currentDynId.set(fragment_args.getString("oid"));
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });
        if (appVersionCode <= 7270300) {//适配版本: ? - 7.25.0 - 7.27.0
            XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.model.b", classLoader, "A", classLoader.loadClass("com.bilibili.app.comm.comment2.CommentContext"), java.lang.String.class, long.class, long.class, long.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Object commentContext = param.args[0];
                    Method getOid = commentContext.getClass().getMethod("getOid");
                    Method getType = commentContext.getClass().getMethod("getType");
                    currentComment.set((String) param.args[5]);
                    currentPictures.set(null);
                    currentOid.set(String.valueOf(getOid.invoke(commentContext)));
                    currentAreaType.set(String.valueOf(getType.invoke(commentContext)));
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });

            XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.inputv2.CommentPublisher", classLoader, "t", classLoader.loadClass("com.bilibili.app.comm.opus.lightpublish.page.comment.c"), classLoader.loadClass("com.bilibili.okretro.GeneralResponse"), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    toCheck(param,1);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });
        } else if(appVersionCode <= 7330300){ //适配版本 7.28.0 - 7.33.0
            XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.model.b", classLoader, "z", classLoader.loadClass("com.bilibili.app.comm.comment2.CommentContext"), java.lang.String.class, long.class, long.class, long.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Object commentContext = param.args[0];
                    Method getOid = commentContext.getClass().getMethod("getOid");
                    Method getType = commentContext.getClass().getMethod("getType");
                    currentComment.set((String) param.args[5]);
                    currentPictures.set((String) param.args[8]);
                    currentOid.set(String.valueOf(getOid.invoke(commentContext)));
                    currentAreaType.set(String.valueOf(getType.invoke(commentContext)));
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });

            XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.inputv2.CommentPublisher", classLoader, "t", classLoader.loadClass("com.bilibili.app.comm.opus.lightpublish.page.comment.c"), classLoader.loadClass("com.bilibili.okretro.GeneralResponse"), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    toCheck(param,1);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });
        } else if (appVersionCode <= 7340200){ //适配版本：7.34.0
            XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.model.b", classLoader, "z", classLoader.loadClass("com.bilibili.app.comm.comment2.CommentContext"), java.lang.String.class, long.class, long.class, long.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, boolean.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Object commentContext = param.args[0];
                    Method getOid = commentContext.getClass().getMethod("getOid");
                    Method getType = commentContext.getClass().getMethod("getType");
                    currentComment.set((String) param.args[5]);
                    currentPictures.set((String) param.args[8]);
                    currentOid.set(String.valueOf(getOid.invoke(commentContext)));
                    currentAreaType.set(String.valueOf(getType.invoke(commentContext)));
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });
            XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.inputv2.CommentPublisher", classLoader, "t", classLoader.loadClass("com.bilibili.app.comm.opus.lightpublish.page.comment.c"), classLoader.loadClass("com.bilibili.okretro.GeneralResponse"), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    toCheck(param,1);
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });
        } else {//适配版本7.35.0 - 7.44.0 - ?
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Context context = (Context) param.thisObject;
                    currentContext.set(context);
                    XposedBridge.log("context:"+context);
                }
            });

            XposedHelpers.findAndHookMethod("com.bilibili.okretro.ServiceGenerator", classLoader, "createService", java.lang.Class.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Object result = param.getResult();
                    if(result != null) {
                        Class<?> resultClass = result.getClass();
                        for (Class<?> classInterface : resultClass.getInterfaces()) {
                            if (classInterface.getCanonicalName().equals("com.bilibili.app.comm.comment2.model.BiliCommentApiService")){
                                XposedHelpers.findAndHookMethod(resultClass, "postComment", java.util.Map.class, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        super.beforeHookedMethod(param);
                                        Map<String,String> arrayMap = (Map<String, String>) param.args[0];
                                        currentComment.set(arrayMap.get("message"));
                                        currentOid.set(arrayMap.get("oid"));
                                        currentAreaType.set(arrayMap.get("type"));
                                        currentPictures.set(arrayMap.get("pictures"));
                                    }
                                });
                            }
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod("com.bilibili.okretro.call.BiliCall", classLoader, "execute", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Object arg = param.getResult();
                    Object body = XposedHelpers.callMethod(arg, "body");
                    if (body != null) {
                        String bodyCanonicalName = body.getClass().getCanonicalName();
                        if (bodyCanonicalName != null && bodyCanonicalName.equals("com.bilibili.okretro.GeneralResponse")){
                            Object data = XposedHelpers.getObjectField(body, "data");
                            if (data != null && data.getClass().getCanonicalName().equals("com.bilibili.app.comm.comment2.model.BiliCommentAddResult")) {
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud", "icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));
                                Class<?> biliCommentAddResultClass = data.getClass();
                                if ((Integer) biliCommentAddResultClass.getField("action").get(data) == 0) {
                                    intent.putExtra("todo", ByXposedLaunchedActivity.TODO_CHECK_COMMENT);
                                    intent.putExtra("message", (String) biliCommentAddResultClass.getField("message").get(data));
                                    intent.putExtra("oid", currentOid.get());
                                    intent.putExtra("type", currentAreaType.get());
                                    intent.putExtra("rpid", String.valueOf((Long) biliCommentAddResultClass.getField("rpid").get(data)));
                                    intent.putExtra("root", String.valueOf((Long) biliCommentAddResultClass.getField("root").get(data)));
                                    intent.putExtra("parent", String.valueOf((Long) biliCommentAddResultClass.getField("parent").get(data)));
                                    intent.putExtra("comment", currentComment.get());
                                    intent.putExtra("dynamic_id", currentDynId.get());
                                    if (Integer.parseInt(currentAreaType.get()) == CommentArea.AREA_TYPE_VIDEO){
                                        intent.putExtra("bvid",getBvidFormAvid(Long.parseLong(currentOid.get())));
                                    }
                                    intent.putExtra("pictures",currentPictures.get());
                                    Object reply = biliCommentAddResultClass.getField("reply").get(data);
                                    long ctime = XposedHelpers.getLongField(reply,"mCtime");
                                    intent.putExtra("ctime",ctime);
                                    XposedBridge.log("bilibili comment add result:" + intent.getExtras().toString());
                                    currentContext.get().startActivity(intent);
                                }
                            } else if (XposedHelpers.getIntField(body,"code") == CommentAddResult.CODE_CONTAIN_SENSITIVE){
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud", "icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));
                                intent.putExtra("todo", ByXposedLaunchedActivity.TODO_SAVE_CONTAIN_SENSITIVE_CONTENT);
                                intent.putExtra("oid", currentOid.get());
                                intent.putExtra("comment", currentComment.get());
                                intent.putExtra("message", (String) XposedHelpers.getObjectField(body,"message"));
                                intent.putExtra("type", currentAreaType.get());
                                intent.putExtra("dynamic_id", currentDynId.get());
                                XposedBridge.log("bilibili comment add result:" + intent.getExtras().toString());
                                currentContext.get().startActivity(intent);
                            }
                        }
                    }
                }
            });
        }
    }

    private void toCheck(XC_MethodHook.MethodHookParam param,int generalResponseLocation ) throws NoSuchFieldException, IllegalAccessException, IOException {
        Object generalResponse = param.args[generalResponseLocation];
        Object biliCommentAddResult = generalResponse.getClass().getField("data").get(generalResponse);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud", "icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));
        Field contextField = param.thisObject.getClass().getDeclaredField("a");
        contextField.setAccessible(true);
        Context context = (Context) contextField.get(param.thisObject);
        Class<?> biliCommentAddResultClass = biliCommentAddResult.getClass();
        if ((Integer) biliCommentAddResultClass.getField("action").get(biliCommentAddResult) == 0) {
            intent.putExtra("todo", ByXposedLaunchedActivity.TODO_CHECK_COMMENT);
            intent.putExtra("message", (String) biliCommentAddResultClass.getField("message").get(biliCommentAddResult));
            intent.putExtra("oid", currentOid.get());
            intent.putExtra("type", currentAreaType.get());
            intent.putExtra("rpid", String.valueOf((Long) biliCommentAddResultClass.getField("rpid").get(biliCommentAddResult)));
            intent.putExtra("root", String.valueOf((Long) biliCommentAddResultClass.getField("root").get(biliCommentAddResult)));
            intent.putExtra("parent", String.valueOf((Long) biliCommentAddResultClass.getField("parent").get(biliCommentAddResult)));
            intent.putExtra("comment", currentComment.get());
            intent.putExtra("dynamic_id", currentDynId.get());
            if (Integer.parseInt(currentAreaType.get()) == CommentArea.AREA_TYPE_VIDEO){
                intent.putExtra("bvid",getBvidFormAvid(Long.parseLong(currentOid.get())));
            }
            intent.putExtra("pictures",currentPictures.get());
            Object reply = XposedHelpers.getObjectField(biliCommentAddResult,"reply");
            long ctime = XposedHelpers.getLongField(reply,"mCtime");
            intent.putExtra("ctime",ctime);
            XposedBridge.log("bilibili comment add result:" + intent.getExtras().toString());
            context.startActivity(intent);
        }
    }
    private String getBvidFormAvid(long avid) throws IOException {
        BiliApiService biliApiService = ServiceGenerator.getBiliApiService();
        GeneralResponse<VideoInfo> body = biliApiService.getVideoInfoByAid(avid).execute().body();
        OkHttpUtil.respNotNull(body);
        return body.data.bvid;
    }

}
