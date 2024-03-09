package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;

public class IntentTransferStationHook extends BaseHook {
    @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {
        XposedHelpers.findAndHookMethod("tv.danmaku.bili.MainActivityV2", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Activity activity = (Activity) param.thisObject;
                positioningActivity(activity,activity.getIntent(),classLoader);
            }
        });

        XposedHelpers.findAndHookMethod("tv.danmaku.bili.MainActivityV2", classLoader, "onNewIntent", Intent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                positioningActivity((Activity) param.thisObject, (Intent) param.args[0],classLoader);
            }
        });

        /*XposedHelpers.findAndHookMethod("com.bilibili.video.videodetail.VideoDetailsActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                Intent intent = activity.getIntent();
                XposedBridge.log("intent.getData():"+intent.getData());
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    Set<String> keySet = extras.keySet();
                    for (String s : keySet) {
                        Object o = extras.get(s);
                        XposedBridge.log(s+"("+o.getClass().getCanonicalName()+"):"+o);
                        if (o instanceof Bundle){
                            Bundle bundle = (Bundle) o;
                            Set<String> keySet1 = bundle.keySet();
                            for (String s1 : keySet1) {
                                Object o1 = bundle.get(s1);
                                XposedBridge.log(s1+"("+o1.getClass().getCanonicalName()+"):"+o1);
                            }
                            XposedBridge.log("+++");
                        }
                    }
                }
                super.afterHookedMethod(param);
            }
        });*/
    }

    private void positioningActivity(Activity activity, Intent intent,ClassLoader classLoader) throws ClassNotFoundException {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }
        String transferActivity = extras.getString("TransferActivity");
        if (transferActivity == null){
            return;
        }


        Bundle transferExtras = extras.getBundle("TransferExtras");
        Intent newIntent = new Intent(activity, classLoader.loadClass(transferActivity));

        String transferUri = extras.getString("transferUri",null);
        if (transferUri != null) {
            newIntent.setData(Uri.parse(transferUri));
        }
        if (transferExtras != null){
            newIntent.putExtras(transferExtras);
        }
        activity.startActivity(newIntent);
    }
}
