package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import de.robv.android.xposed.XposedBridge;

public class HookStater {
    public int appVersionCode;
    public ClassLoader classLoader;

    public HookStater(int appVersionCode, ClassLoader classLoader) {
        this.appVersionCode = appVersionCode;
        this.classLoader = classLoader;
    }

    public void startHook(BaseHook baseHook){
        try {
            baseHook.startHook(appVersionCode,classLoader);
        } catch (Throwable throwable){
            XposedBridge.log(baseHook.getClass().getSimpleName()+"加载失败，异常信息："+throwable);
        }

    }
}
