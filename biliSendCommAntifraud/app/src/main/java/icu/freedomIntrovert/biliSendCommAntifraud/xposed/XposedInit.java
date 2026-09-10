package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import androidx.annotation.NonNull;
import icu.freedomIntrovert.biliSendCommAntifraud.BuildConfig;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks.*;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class XposedInit extends XposedModule {
    private String processName;
    private boolean routed;

    @Override public void onModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        processName = param.getProcessName();
        XB.initialize(this);
        XB.log("event=module_loaded module=" + BuildConfig.VERSION_NAME
                + " process=" + processName + " api=" + getApiVersion()
                + " framework=" + getFrameworkName() + " version=" + getFrameworkVersion());
    }

    @Override public synchronized void onPackageReady(@NonNull XposedModuleInterface.PackageReadyParam param) {
        String name = param.getPackageName();
        if (!name.equals("tv.danmaku.bili") && !name.equals("com.bilibili.app.in")) {
            return;
        }
        if (!name.equals(processName)) {
            XB.log("event=route_skipped package=" + name + " process=" + processName + " reason=not_main_process");
            return;
        }
        if (routed) {
            XB.log("event=route_skipped package=" + name + " reason=already_routed");
            return;
        }
        ClassLoader loader = param.getClassLoader();
        HookConfig.initialize(this);
        // PackageReady precedes Application creation. Existing hooks don't branch on version,
        // so install now and use sourceDir identity for diagnostics rather than hidden Context APIs.
        XB.log("event=route package=" + name + " process=" + processName
                + " source=" + param.getApplicationInfo().sourceDir);
        HookStater starter = new HookStater(this, 0, loader, name, processName);
        if (name.equals("tv.danmaku.bili")) {
            starter.startHook(new PostCommentHookByMaster());
            starter.startHook(new ShowInvisibleCommentHook());
            starter.startHook(new PostPictureHook());
        } else {
            starter.startHook(new PostCommentHookByGlobal());
            starter.startHook(new ShowInvisibleCommentHook(false));
        }
        starter.startHook(new MossAddReplyHook());
        starter.startHook(new IntentTransferStationHook());
        starter.startHook(new FuckFoldPicturesHook());
        routed = true;
    }
}
