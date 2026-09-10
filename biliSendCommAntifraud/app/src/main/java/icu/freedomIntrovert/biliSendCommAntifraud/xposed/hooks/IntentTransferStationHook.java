package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.XB;

public final class IntentTransferStationHook extends BaseHook {
    @Override public void startHook(int version, ClassLoader loader) throws Throwable {
        Class<?> main = loader.loadClass("tv.danmaku.bili.MainActivityV2");
        hook(main, "onCreate", chain -> {
            Object result = chain.proceed();
            Activity activity = (Activity) chain.getThisObject();
            positioningActivity(activity, activity.getIntent(), loader);
            return result;
        }, Bundle.class);
        hook(main, "onNewIntent", chain -> {
            Object result = chain.proceed();
            positioningActivity((Activity) chain.getThisObject(), (Intent) chain.getArg(0), loader);
            return result;
        }, Intent.class);
    }

    private void positioningActivity(Activity activity, Intent intent, ClassLoader classLoader) throws ClassNotFoundException {
        if (intent == null) return;
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }
        String transferActivity = extras.getString("TransferActivity");
        if (transferActivity == null) {
            return;
        }

        intent.removeExtra("TransferActivity"); // Consume once; configuration recreation must not replay it.
        XB.log("Activity转发到：" + transferActivity);

        Bundle transferExtras = extras.getBundle("TransferExtras");
        String resolved = HostCallAdapter.resolveTransferActivity(classLoader, transferActivity);
        if (!resolved.equals(transferActivity)) {
            XB.log("event=transfer_activity_fallback from=" + transferActivity + " to=" + resolved);
        }
        Intent newIntent = new Intent(activity, classLoader.loadClass(resolved));

        String transferUri = extras.getString("transferUri", null);
        if (transferUri != null) {
            newIntent.setData(Uri.parse(transferUri));
        }
        if (transferExtras != null) {
            newIntent.putExtras(transferExtras);
        }
        activity.startActivity(newIntent);
    }
}
