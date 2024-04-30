package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;

public class PostCommentHookByGlobal extends BaseHook {
    @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {
        Handler handler = new Handler();
        AtomicReference<Context> currentContext = new AtomicReference<>();
        AtomicReference<String> currentDynId = new AtomicReference<>();

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
                if (dynamicId == null) {
                    dynamicId = fragment_args.getString("oid");
                }
                XposedBridge.log("动态ID:" + dynamicId);
                currentDynId.set(dynamicId);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });

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
                XposedBridge.log("context:" + context);
            }
        });

        /*XposedHelpers.findAndHookMethod("retrofit2.Retrofit", classLoader, "b", java.lang.Class.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Object result = param.getResult();
                XposedBridge.log("生成Retrofit调用结果："+result);
                if (result != null) {
                    Class<?> resultClass = result.getClass();
                    for (Class<?> classInterface : resultClass.getInterfaces()) {
                        XposedBridge.log("classInterface："+classInterface);
                        if (classInterface.getCanonicalName().equals("com.bilibili.app.comm.comment2.model.BiliCommentApiService")) {
                            XposedHelpers.findAndHookMethod(resultClass, "postComment", java.util.Map.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    super.beforeHookedMethod(param);
                                    Map<String, String> arrayMap = (Map<String, String>) param.args[0];
                                    XposedBridge.log("评论提交信息："+arrayMap.toString());
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
        });*/

        XposedHelpers.findAndHookMethod("com.bilibili.okretro.call.a", classLoader, "execute", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Object arg = param.getResult();
                if (arg == null) {
                    return;
                }
                Object body = XposedHelpers.callMethod(arg, "a");
                if (body != null) {
                    String bodyCanonicalName = body.getClass().getCanonicalName();
                    if (bodyCanonicalName != null && bodyCanonicalName.equals("com.bilibili.okretro.GeneralResponse")) {
                        Object data = XposedHelpers.getObjectField(body, "data");
                        if (data != null && "com.bilibili.app.comm.comment2.model.BiliCommentAddResult".equals(data.getClass().getCanonicalName())) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud", "icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));

                            Class<?> biliCommentAddResultClass = data.getClass();
                            Object reply = XposedHelpers.getObjectField(data, "reply");
                            Object content = XposedHelpers.getObjectField(reply, "mContent");
                            Integer type = (Integer) XposedHelpers.getObjectField(reply, "mType");
                            Long oid = (Long) XposedHelpers.getObjectField(reply, "mOid");
                            if ((Integer) biliCommentAddResultClass.getField("action").get(data) == 0) {
                                intent.putExtra("todo", ByXposedLaunchedActivity.TODO_CHECK_COMMENT);
                                intent.putExtra("message", (String) biliCommentAddResultClass.getField("message").get(data));
                                intent.putExtra("oid", String.valueOf(oid));
                                intent.putExtra("type", String.valueOf(type));
                                intent.putExtra("rpid", String.valueOf((Long) biliCommentAddResultClass.getField("rpid").get(data)));
                                intent.putExtra("root", String.valueOf((Long) biliCommentAddResultClass.getField("root").get(data)));
                                intent.putExtra("parent", String.valueOf((Long) biliCommentAddResultClass.getField("parent").get(data)));
                                intent.putExtra("comment", (String) XposedHelpers.getObjectField(content, "mMsg"));
                                if (type == CommentArea.AREA_TYPE_VIDEO) {
                                    intent.putExtra("bvid", Utils.getBvidFormAvid(oid));
                                }
                                long ctime = XposedHelpers.getLongField(reply, "mCtime");
                                intent.putExtra("ctime", ctime);
                                intent.putExtra("dynamic_id", currentDynId.get());
                                try {
                                    Field picturesField = content.getClass().getField("pictures");
                                    List<?> pictures = (List<?>) picturesField.get(content);
                                    intent.putExtra("pictures", Utils.picturesObjToString(pictures));
                                } catch (NoSuchFieldException e) {
                                    XposedBridge.log("当前哔哩哔哩版本不支持发送图片");
                                }
                                XposedBridge.log("bilibili comment add result:" + intent.getExtras().toString());
                                currentContext.get().startActivity(intent);
                            }
                        }
                    }
                }
            }
        });

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
}
