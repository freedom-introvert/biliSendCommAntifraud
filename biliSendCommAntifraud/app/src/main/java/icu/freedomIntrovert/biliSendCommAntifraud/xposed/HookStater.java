package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import java.util.HashSet;
import java.util.Set;
import io.github.libxposed.api.XposedInterface;

public final class HookStater {
    private final XposedInterface api;
    private final int version;
    private final ClassLoader loader;
    private final Set<Class<?>> installed = new HashSet<>();

    private final String packageName;
    private final String processName;

    public HookStater(XposedInterface api, int version, ClassLoader loader,
                      String packageName, String processName) {
        this.api = api;
        this.version = version;
        this.loader = loader;
        this.packageName = packageName;
        this.processName = processName;
    }

    public synchronized void startHook(BaseHook feature) {
        if (installed.contains(feature.getClass())) return;
        try {
            feature.install(api, version, loader);
            installed.add(feature.getClass());
        } catch (Throwable failure) {
            XB.error("event=hook_failed feature=" + feature.getClass().getSimpleName()
                    + " package=" + packageName + " process=" + processName, failure);
        }
    }
}
