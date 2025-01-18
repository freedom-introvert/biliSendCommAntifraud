package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

public abstract class BaseHook {

    public abstract void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException;
}
