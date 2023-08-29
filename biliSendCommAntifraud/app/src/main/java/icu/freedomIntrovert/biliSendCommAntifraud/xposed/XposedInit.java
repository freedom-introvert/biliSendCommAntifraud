package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import android.content.Context;

import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks.PostCommentHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks.PostCommentHookByGlobal;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks.ShowInvisibleCommentHook;

public class XposedInit implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals("tv.danmaku.bili")) {//国内版
            ClassLoader classLoader = loadPackageParam.classLoader;
            int appVersionCode = systemContext().getPackageManager().getPackageInfo(loadPackageParam.packageName, 0).versionCode;
            XposedBridge.log("bilibili version code:" + appVersionCode);
            HookStater hookStater = new HookStater(appVersionCode,classLoader);
            hookStater.startHook(new PostCommentHook());
            //暂时放弃弹幕检查
            //hookStater.startHook(new PostDanmakuHook());
            hookStater.startHook(new ShowInvisibleCommentHook());
        } else if (loadPackageParam.packageName.equals("com.bilibili.app.in")){//国际版
            ClassLoader classLoader = loadPackageParam.classLoader;
            int appVersionCode = systemContext().getPackageManager().getPackageInfo(loadPackageParam.packageName, 0).versionCode;
            XposedBridge.log("global bilibili version code:" + appVersionCode);
            HookStater hookStater = new HookStater(appVersionCode,classLoader);
            hookStater.startHook(new PostCommentHookByGlobal());
        }
    }

    public static Context systemContext() {
        Object obj = null;
        Class<?> findClassIfExists = XposedHelpers.findClass("android.app.ActivityThread", null);
        if (findClassIfExists != null) {
            obj = XposedHelpers.callStaticMethod(findClassIfExists, "currentActivityThread", Arrays.copyOf(new Object[0], 0));
        }
        return (Context) XposedHelpers.callMethod(obj, "getSystemContext", Arrays.copyOf(new Object[0], 0));
    }
}
