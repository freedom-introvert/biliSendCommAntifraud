package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
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
        try {
            XposedHelpers.findAndHookMethod("com.bilibili.lib.ui.ComposeActivity", classLoader, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Method getIntentMethod = param.thisObject.getClass().getMethod("getIntent");
                    Intent intent = (Intent) getIntentMethod.invoke(param.thisObject);
                    Bundle fragment_args = intent.getExtras().getBundle("fragment_args");
                    XposedBridge.log("动态ID:" + fragment_args.getString("oid"));
                    currentDynId.set(fragment_args.getString("oid"));
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log("获取动态ID模块加载失败，请更新哔哩哔哩至适配版本");
        }
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

        XposedHelpers.findAndHookMethod("com.bilibili.okretro.ServiceGenerator", classLoader, "createService", java.lang.Class.class, new XC_MethodHook() {
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
                    if (bodyCanonicalName != null && bodyCanonicalName.equals("com.bilibili.okretro.GeneralResponse")) {
                        Object data = XposedHelpers.getObjectField(body, "data");
                        if (data != null && data.getClass().getCanonicalName().equals("com.bilibili.app.comm.comment2.model.BiliCommentAddResult")) {
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
                                intent.putExtra("rpid", String.valueOf(biliCommentAddResultClass.getField("rpid").get(data)));
                                intent.putExtra("root", String.valueOf(biliCommentAddResultClass.getField("root").get(data)));
                                intent.putExtra("parent", String.valueOf(biliCommentAddResultClass.getField("parent").get(data)));
                                intent.putExtra("comment", (String) XposedHelpers.getObjectField(content, "mMsg"));
                                intent.putExtra("dynamic_id", currentDynId.get());
                                if (type == CommentArea.AREA_TYPE_VIDEO) {
                                    intent.putExtra("bvid", Utils.getBvidFormAvid(oid));
                                }
                                try {
                                    Field picturesField = content.getClass().getField("pictures");
                                    List<?> pictures = (List<?>) picturesField.get(content);
                                    intent.putExtra("pictures", Utils.picturesObjToString(pictures));
                                } catch (NoSuchFieldException e) {
                                    XposedBridge.log("当前哔哩哔哩版本不支持发送图片");
                                }
                                long ctime = XposedHelpers.getLongField(reply, "mCtime");
                                intent.putExtra("ctime", ctime);
                                XposedBridge.log("bilibili comment add result:" + intent.getExtras().toString());
                                currentContext.get().startActivity(intent);
                            }
                        } else if (XposedHelpers.getIntField(body, "code") == CommentAddResult.CODE_CONTAIN_SENSITIVE) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud", "icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));
                            intent.putExtra("todo", ByXposedLaunchedActivity.TODO_SAVE_CONTAIN_SENSITIVE_CONTENT);
                            intent.putExtra("oid", currentOid.get());
                            intent.putExtra("comment", currentComment.get());
                            intent.putExtra("message", (String) XposedHelpers.getObjectField(body, "message"));
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
