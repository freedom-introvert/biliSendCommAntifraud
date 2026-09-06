package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import io.github.libxposed.api.XposedInterface;

public abstract class BaseHook {
    private XposedInterface api;
    private final List<XposedInterface.HookHandle> handles = new ArrayList<>();

    final void install(XposedInterface api, int version, ClassLoader loader) throws Throwable {
        this.api = api;
        try { startHook(version, loader); }
        catch (Throwable failure) {
            // Roll back partial installation; a failed feature must not leave half its hooks active.
            for (XposedInterface.HookHandle handle : handles) {
                try { handle.unhook(); } catch (Throwable ignored) {}
            }
            handles.clear();
            throw failure;
        }
    }

    protected final void hook(Class<?> type, String name, XposedInterface.Hooker callback,
                              Class<?>... parameters) throws NoSuchMethodException {
        Method method = Reflect.method(type, name, parameters);
        hook(method, callback);
    }

    protected final void hook(Method method, XposedInterface.Hooker callback) {
        method.setAccessible(true);
        handles.add(api.hook(method)
                .setId(getClass().getSimpleName() + ":" + method.toGenericString())
                .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                .intercept(callback));
        XB.log("event=hook_registered target=" + method.toGenericString());
    }

    public abstract void startHook(int appVersionCode, ClassLoader classLoader) throws Throwable;
}
