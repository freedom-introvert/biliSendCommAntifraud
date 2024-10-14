package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;

public class ShowInvisibleCommentHook extends BaseHook {
    @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {
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
        //如果是invisible评论，在IP属地信息那标记是[隐藏评论]
        XposedHelpers.findAndHookMethod("com.bapis.bilibili.main.community.reply.v1.ReplyControl", classLoader, "getLocation", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Object thisObject = methodHookParam.thisObject;
                Field invisibleField = thisObject.getClass().getDeclaredField("invisible_");
                invisibleField.setAccessible(true);
                boolean invisible = invisibleField.getBoolean(thisObject);
                Field locationField = thisObject.getClass().getDeclaredField("location_");
                locationField.setAccessible(true);
                String location = (String) locationField.get(thisObject);
                if (invisible) {
                    return location + " [隐藏的评论]";
                } else {
                    return location;
                }
            }
        });
    }
}
