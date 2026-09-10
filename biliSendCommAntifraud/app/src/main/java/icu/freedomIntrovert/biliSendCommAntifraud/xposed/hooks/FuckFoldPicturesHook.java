package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.HookConfig;

public final class FuckFoldPicturesHook extends BaseHook {
    @Override public void startHook(int version, ClassLoader loader) throws Throwable {
        hook(loader.loadClass("com.bapis.bilibili.main.community.reply.v1.ReplyControl"),
                "getFoldPictures", chain -> {
                    Object original = chain.proceed();
                    return HookConfig.unfoldPictures() ? false : original;
                });
    }
}
