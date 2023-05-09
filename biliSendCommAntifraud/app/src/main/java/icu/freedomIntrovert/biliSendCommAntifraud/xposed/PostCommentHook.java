package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class PostCommentHook implements IXposedHookLoadPackage {
    //public static Context CurrentContext;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals("tv.danmaku.bili")){
            AtomicReference<Context> currentContext = new AtomicReference<>();
            AtomicReference<String> currentOid = new AtomicReference<>();
            AtomicReference<String> currentId = new AtomicReference<>();
            AtomicReference<String> currentAreaType = new AtomicReference<>();
            AtomicReference<String> currentComment = new AtomicReference<>();

            XposedHelpers.findAndHookMethod("com.bilibili.lib.ui.ComposeActivity", loadPackageParam.classLoader, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Method getIntentMethod = param.thisObject.getClass().getMethod("getIntent");
                    Intent intent = (Intent) getIntentMethod.invoke(param.thisObject);
                    Bundle fragment_args = intent.getExtras().getBundle("fragment_args");
                    currentId.set(fragment_args.getString("oid"));
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });

            XposedHelpers.findAndHookConstructor("com.bilibili.app.comm.comment2.inputv2.CommentPublisher", loadPackageParam.classLoader, android.content.Context.class, loadPackageParam.classLoader.loadClass("com.bilibili.app.comm.comment2.CommentContext"), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Context context = (Context) param.args[0];
                    currentContext.set(context);
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });

            XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.model.b", loadPackageParam.classLoader, "A", loadPackageParam.classLoader.loadClass("com.bilibili.app.comm.comment2.CommentContext"), java.lang.String.class, long.class, long.class, long.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Object commentContext = param.args[0];
                    Method getOid = commentContext.getClass().getMethod("getOid");
                    Method getType = commentContext.getClass().getMethod("getType");
                    currentComment.set((String) param.args[5]);
                    currentOid.set(String.valueOf(getOid.invoke(commentContext)));
                    currentAreaType.set(String.valueOf(getType.invoke(commentContext)));
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });

            XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.inputv2.CommentPublisher", loadPackageParam.classLoader, "t", loadPackageParam.classLoader.loadClass("com.bilibili.app.comm.opus.lightpublish.page.comment.c"), loadPackageParam.classLoader.loadClass("com.bilibili.okretro.GeneralResponse"), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Object generalResponse = param.args[1];
                    Object biliCommentAddResult = generalResponse.getClass().getField("data").get(generalResponse);
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud","icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));
                    Field contextField = param.thisObject.getClass().getDeclaredField("a");
                    contextField.setAccessible(true);
                    Context context = (Context) contextField.get(param.thisObject);
                    Class<?> biliCommentAddResultClass = biliCommentAddResult.getClass();
                    if ((Integer) biliCommentAddResultClass.getField("action").get(biliCommentAddResult) == 0) {
                        intent.putExtra("message", (String) biliCommentAddResultClass.getField("message").get(biliCommentAddResult));
                        intent.putExtra("oid", currentOid.get());
                        intent.putExtra("type", currentAreaType.get());
                        intent.putExtra("rpid", String.valueOf((Long) biliCommentAddResultClass.getField("rpid").get(biliCommentAddResult)));
                        intent.putExtra("root", String.valueOf((Long) biliCommentAddResultClass.getField("root").get(biliCommentAddResult)));
                        intent.putExtra("parent", String.valueOf((Long) biliCommentAddResultClass.getField("parent").get(biliCommentAddResult)));
                        intent.putExtra("comment", currentComment.get());
                        intent.putExtra("id", currentId.get());
                        XposedBridge.log(intent.getExtras().toString());
                        context.startActivity(intent);
                    }
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });
        }

    }
}
