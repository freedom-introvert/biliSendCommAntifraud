package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;

public class PostDanmakuHook extends BaseHook {

    public void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {
        AtomicLong currentOid = new AtomicLong();
        AtomicLong currentAvid = new AtomicLong();
        AtomicReference<String> currentContent = new AtomicReference<>();

        XposedHelpers.findAndHookMethod("tv.danmaku.biliplayerv2.service.interact.biz.sender.ChronosDanmakuSender", classLoader, "n", classLoader.loadClass("tv.danmaku.biliplayerv2.d"), Context.class, String.class, classLoader.loadClass("tv.danmaku.biliplayerv2.service.interact.core.sender.a"), int.class, String.class, String.class, String.class, String.class, Long.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                StringBuilder sb = new StringBuilder("发送弹幕参数\n");
                for (int i = 0; i < param.args.length; i++) {
                    if (param.args[i] != null) {
                        sb.append(i).append(":").append(param.args[i].getClass().getCanonicalName()).append("  ").append(param.args[i].toString()).append("\n");
                    } else {
                        sb.append(i).append(":null");
                    }
                    currentContent.set((String) param.args[2]);
                    currentOid.set(Long.parseLong((String)param.args[5]));
                    currentAvid.set(Long.parseLong((String) param.args[6]));
                }
                XposedBridge.log(sb.toString());
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });
        XposedHelpers.findAndHookMethod("tv.danmaku.biliplayerv2.service.interact.biz.sender.ChronosDanmakuSender$c", classLoader, "a", classLoader.loadClass("tv.danmaku.biliplayerv2.utils.DanmakuSendHelper$DanmakuSendResponse"), new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                StringBuilder sb = new StringBuilder("发送弹幕response参数\n");
                for (int i = 0; i < param.args.length; i++) {
                    if (param.args[i] != null) {
                        sb.append(i).append(":").append(param.args[i].getClass().getCanonicalName()).append("  ").append(param.args[i].toString()).append("\n");
                    } else {
                        sb.append(i).append(":null");
                    }
                    Class<?> biliAccountsClass = classLoader.loadClass("com.bilibili.lib.accounts.BiliAccounts");
                    Field contextField = param.thisObject.getClass().getDeclaredField("c");
                    contextField.setAccessible(true);
                    Context context = (Context) contextField.get(param.thisObject);
                    XposedBridge.log("弹幕context:" + context.toString());
                    Object biliAccounts = biliAccountsClass.getMethod("get", Context.class).invoke(null, context);
                    String accessKey = (String) biliAccounts.getClass().getMethod("getAccessKey").invoke(biliAccounts);
                    XposedBridge.log("accessKey:" + accessKey);
                    Object danmakuSendResponse = param.args[0];
                    Method dmidMethod = danmakuSendResponse.getClass().getMethod("getDmid");
                    long dmid = (long) dmidMethod.invoke(danmakuSendResponse);
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud", "icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));
                    intent.putExtra("action", ByXposedLaunchedActivity.ACTION_CHECK_DANMAKU);
                    intent.putExtra("oid",currentOid.get());
                    intent.putExtra("dmid",dmid);
                    intent.putExtra("content",currentContent.get());
                    intent.putExtra("accessKey",accessKey);
                    intent.putExtra("avid",currentAvid.get());
                    XposedBridge.log("bilibili danmaku add result:" + intent.getExtras());
                    context.startActivity(intent);
                }
                XposedBridge.log(sb.toString());
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });
    }
}
