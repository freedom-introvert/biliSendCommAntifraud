package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;

public class FuckFoldPicturesHook extends BaseHook {

    @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {
        XposedHelpers.findAndHookMethod("com.bapis.bilibili.main.community.reply.v1.ReplyControl", classLoader, "getFoldPictures", new XC_MethodHook() {
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
