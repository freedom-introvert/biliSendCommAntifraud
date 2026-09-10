package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.Reflect;

public final class ShowInvisibleCommentHook extends BaseHook {
    private final boolean markLocation;
    public ShowInvisibleCommentHook() { this(true); }
    public ShowInvisibleCommentHook(boolean markLocation) { this.markLocation = markLocation; }

    @Override public void startHook(int version, ClassLoader loader) throws Throwable {
        Class<?> control = loader.loadClass("com.bapis.bilibili.main.community.reply.v1.ReplyControl");
        hook(control, "getInvisible", chain -> { chain.proceed(); return false; });
        if (markLocation) {
            hook(control, "getLocation", chain -> {
                Object location = chain.proceed();
                boolean invisible = (Boolean) Reflect.getObjectField(chain.getThisObject(), "invisible_");
                return invisible ? String.valueOf(location) + " [隐藏的评论]" : location;
            });
        }
    }
}
