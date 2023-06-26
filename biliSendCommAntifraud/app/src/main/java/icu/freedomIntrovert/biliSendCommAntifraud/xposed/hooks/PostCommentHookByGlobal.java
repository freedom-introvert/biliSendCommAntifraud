package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;

public class PostCommentHookByGlobal extends BaseHook {
    @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {
        AtomicReference<Context> currentContext = new AtomicReference<>();
        AtomicReference<String> currentOid = new AtomicReference<>();
        AtomicReference<String> currentId = new AtomicReference<>();
        AtomicReference<String> currentAreaType = new AtomicReference<>();
        AtomicReference<String> currentComment = new AtomicReference<>();

        XposedHelpers.findAndHookMethod("com.bilibili.lib.ui.ComposeActivity", classLoader, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
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
                    intent.putExtra("id", currentId.get());
                    intent.putExtra("hasPictures",false);
                    XposedBridge.log("bilibili comment add result:" + intent.getExtras().toString());
                    context.startActivity(intent);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
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
