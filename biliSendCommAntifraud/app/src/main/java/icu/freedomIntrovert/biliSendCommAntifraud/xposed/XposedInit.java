package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import android.content.Context;

import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks.FuckFoldPicturesHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks.IntentTransferStationHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks.PostCommentHookByGlobal;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks.PostCommentHookByMaster;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks.PostPictureHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks.ShowInvisibleCommentHook;

public class XposedInit implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals("tv.danmaku.bili")) {//国内版
            Config config = Config.getInstanceByXPEnvironment();
            ClassLoader classLoader = loadPackageParam.classLoader;
            int appVersionCode = systemContext().getPackageManager().getPackageInfo(loadPackageParam.packageName, 0).versionCode;
            XposedBridge.log("bilibili version code:" + appVersionCode);
            HookStater hookStater = new HookStater(appVersionCode,classLoader);
            hookStater.startHook(new PostCommentHookByMaster());
            //暂时放弃弹幕检查
            //hookStater.startHook(new PostDanmakuHook());
            hookStater.startHook(new ShowInvisibleCommentHook());
            hookStater.startHook(new IntentTransferStationHook());
            //替换拍照为从相册选择
            if (config.getEnablePostPictureHook()) {
                hookStater.startHook(new PostPictureHook());
            }
            //去他妈的图片折叠
            if (config.getEnableFuckFoldPicturesHook()) {
                hookStater.startHook(new FuckFoldPicturesHook());
            }
        } else if (loadPackageParam.packageName.equals("com.bilibili.app.in")){//国际版
            Config config = Config.getInstanceByXPEnvironment();
            ClassLoader classLoader = loadPackageParam.classLoader;
            int appVersionCode = systemContext().getPackageManager().getPackageInfo(loadPackageParam.packageName, 0).versionCode;
            XposedBridge.log("global bilibili version code:" + appVersionCode);
            HookStater hookStater = new HookStater(appVersionCode,classLoader);
            hookStater.startHook(new IntentTransferStationHook());
            hookStater.startHook(new PostCommentHookByGlobal());

            //去他妈的图片折叠
            if (config.getEnableFuckFoldPicturesHook()) {
                hookStater.startHook(new FuckFoldPicturesHook());
            }
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
