package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import icu.freedomIntrovert.biliSendCommAntifraud.settings.HookSettings;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

/** App-only service connection. No dependency on framework-provided API classes. */
public final class AntifraudApplication extends Application
        implements XposedServiceHelper.OnServiceListener {
    private static AntifraudApplication instance;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Map<XposedService, String> services = new LinkedHashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile String status = "等待 API 102 框架连接；未连接时仍可使用手动检查。";
    private SharedPreferences local;
    private final SharedPreferences.OnSharedPreferenceChangeListener configListener = (prefs, key) -> {
        if (key == null || HookSettings.isSharedKey(key)) publish();
    };

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        local = Config.getInstance(this).sp;
        local.registerOnSharedPreferenceChangeListener(configListener);
        XposedServiceHelper.registerListener(this);
    }

    public static AntifraudApplication get() { return instance; }
    public String getFrameworkStatus() { return status; }
    public void addStatusListener(Runnable listener) { listeners.add(listener); }
    public void removeStatusListener(Runnable listener) { listeners.remove(listener); }

    @Override public void onServiceBind(XposedService service) {
        worker.execute(() -> {
            services.put(service, "连接中");
            publishNow();
        });
    }

    @Override public void onServiceDied(XposedService service) {
        worker.execute(() -> {
            services.remove(service);
            updateStatus();
        });
    }

    public void publish() { worker.execute(this::publishNow); }

    private void publishNow() {
        for (XposedService service : services.keySet()) {
            try {
                String framework = service.getFrameworkName() + " " + service.getFrameworkVersion();
                if (service.getApiVersion() < 102) {
                    services.put(service, framework + "：需要 API 102");
                    continue;
                }
                if ((service.getFrameworkProperties() & XposedService.PROP_CAP_REMOTE) == 0) {
                    services.put(service, framework + "：不支持远程配置");
                    continue;
                }
                SharedPreferences.Editor editor = service.getRemotePreferences(HookSettings.GROUP).edit();
                for (Map.Entry<String, Boolean> e : HookSettings.snapshot(local.getAll()).entrySet()) {
                    editor.putBoolean(e.getKey(), e.getValue());
                }
                if (!editor.putBoolean(HookSettings.READY, true).commit()) {
                    services.put(service, framework + "：配置同步失败，可重试");
                    continue;
                }
                List<String> scope = service.getScope();
                services.put(service, framework + "（API " + service.getApiVersion() + "）"
                        + "\n国内版：" + (scope.contains("tv.danmaku.bili") ? "已在作用域" : "未在作用域")
                        + "；国际版：" + (scope.contains("com.bilibili.app.in") ? "已在作用域" : "未在作用域")
                        + "\n配置已同步；是否命中 Hook 需查看目标进程日志。");
            } catch (RuntimeException e) {
                services.put(service, "框架通信失败，可重试");
                Log.w("AntifraudService", "event=config_publish_failed type=" + e.getClass().getSimpleName());
            }
        }
        updateStatus();
    }

    private void updateStatus() {
        status = services.isEmpty() ? "框架未连接或已断开；手动检查可用。"
                : String.join("\n\n", services.values());
        main.post(() -> { for (Runnable listener : listeners) listener.run(); });
    }
}
