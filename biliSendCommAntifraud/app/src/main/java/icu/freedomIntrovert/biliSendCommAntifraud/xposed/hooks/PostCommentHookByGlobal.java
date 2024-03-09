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
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.VideoInfo;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.ServiceGenerator;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;

public class PostCommentHookByGlobal extends BaseHook {
    @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {
        AtomicReference<Context> currentContext = new AtomicReference<>();
        AtomicReference<String> currentOid = new AtomicReference<>();
        AtomicReference<String> currentId = new AtomicReference<>();
        AtomicReference<String> currentAreaType = new AtomicReference<>();
        AtomicReference<String> currentComment = new AtomicReference<>();
        AtomicReference<String> currentPictures = new AtomicReference<>();

        XposedHelpers.findAndHookMethod("com.bilibili.lib.ui.ComposeActivity", classLoader, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Method getIntentMethod = param.thisObject.getClass().getMethod("getIntent");
                Intent intent = (Intent) getIntentMethod.invoke(param.thisObject);
                Bundle fragment_args = intent.getExtras().getBundle("fragment_args");
                //新版动态ID获取方法
                String dynamicId = fragment_args.getString("dynamicId");
                //若没有获取到动态ID，则说明是旧版，使用旧版获取方式
                if (dynamicId==null){
                    dynamicId = fragment_args.getString("oid");
                }
                XposedBridge.log("动态ID:"+dynamicId);
                currentId.set(dynamicId);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });
        if(appVersionCode <= 7082100) {
            XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.model.b", classLoader, "z", java.lang.String.class, long.class, int.class, long.class, long.class, int.class, int.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, long.class, int.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, long.class, java.lang.String.class, java.lang.String.class, classLoader.loadClass("java.util.Map"), classLoader.loadClass("no1.a"), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    currentOid.set(String.valueOf(param.args[1]));
                    currentAreaType.set(String.valueOf(param.args[2]));
                    currentComment.set((String) param.args[8]);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });
            XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.input.a", classLoader, "L", classLoader.loadClass("com.bilibili.okretro.GeneralResponse"), classLoader.loadClass("com.bilibili.app.comm.comment2.input.a$e"), classLoader.loadClass("dc.r"), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Object generalResponse = param.args[0];
                    Object biliCommentAddResult = generalResponse.getClass().getField("data").get(generalResponse);
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud", "icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));
                    Field contextField = param.thisObject.getClass().getDeclaredField("a");
                    contextField.setAccessible(true);
                    Context context = (Context) contextField.get(param.thisObject);
                    Class<?> biliCommentAddResultClass = biliCommentAddResult.getClass();
                    if ((Integer) biliCommentAddResultClass.getField("action").get(biliCommentAddResult) == 0) {//不等于0很可能是up开启了评论精选之类的，toast提示你了，检查无意义也误判
                        intent.putExtra("todo", ByXposedLaunchedActivity.TODO_CHECK_COMMENT);
                        intent.putExtra("message", (String) biliCommentAddResultClass.getField("message").get(biliCommentAddResult));
                        intent.putExtra("oid", currentOid.get());
                        intent.putExtra("type", currentAreaType.get());
                        intent.putExtra("rpid", String.valueOf((Long) biliCommentAddResultClass.getField("rpid").get(biliCommentAddResult)));
                        intent.putExtra("root", String.valueOf((Long) biliCommentAddResultClass.getField("root").get(biliCommentAddResult)));
                        intent.putExtra("parent", String.valueOf((Long) biliCommentAddResultClass.getField("parent").get(biliCommentAddResult)));
                        intent.putExtra("comment", currentComment.get());
                        if (Integer.parseInt(currentAreaType.get()) == CommentArea.AREA_TYPE_VIDEO){
                            intent.putExtra("bvid",getBvidFormAvid(Long.parseLong(currentOid.get())));
                        }
                        Object reply = XposedHelpers.getObjectField(biliCommentAddResult,"reply");
                        long ctime = XposedHelpers.getLongField(reply,"mCtime");
                        intent.putExtra("ctime",ctime);
                        intent.putExtra("dynamic_id", currentId.get());
                        XposedBridge.log("bilibili comment add result:" + intent.getExtras().toString());
                        context.startActivity(intent);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });
        } else {
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

            XposedHelpers.findAndHookMethod("retrofit2.Retrofit", classLoader, "b", java.lang.Class.class, new XC_MethodHook() {
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

            XposedHelpers.findAndHookMethod("com.bilibili.okretro.call.a", classLoader, "execute", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Object arg = param.getResult();
                    Object body = XposedHelpers.callMethod(arg, "a");
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
                                    if (Integer.parseInt(currentAreaType.get()) == CommentArea.AREA_TYPE_VIDEO){
                                        intent.putExtra("bvid",getBvidFormAvid(Long.parseLong(currentOid.get())));
                                    }
                                    Object reply = XposedHelpers.getObjectField(data,"reply");
                                    long ctime = XposedHelpers.getLongField(reply,"mCtime");
                                    intent.putExtra("ctime",ctime);
                                    intent.putExtra("dynamic_id", currentId.get());
                                    intent.putExtra("pictures",currentPictures.get());
                                    XposedBridge.log("bilibili comment add result:" + intent.getExtras().toString());
                                    currentContext.get().startActivity(intent);
                                }
                            }
                        }
                    }
                }
            });
        }

        //强制显示invisible评论
        XposedHelpers.findAndHookMethod("com.bapis.bilibili.main.community.reply.v1.ReplyControl", classLoader, "getInvisible", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                param.setResult(false);
            }
        });
    }

    private String getBvidFormAvid(long avid) throws IOException {
        BiliApiService biliApiService = ServiceGenerator.getBiliApiService();
        GeneralResponse<VideoInfo> body = biliApiService.getVideoInfoByAid(avid).execute().body();
        OkHttpUtil.respNotNull(body);
        return body.data.bvid;
    }
}
