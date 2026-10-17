package io.github.tnyfishy.all4hyper;

import android.os.Handler;
import android.os.Looper;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;
import java.util.function.Consumer;

/** The LSPosed service connection; registered once per process, reported on the main thread. */
final class ServiceHolder {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static XposedService service;
    private static Consumer<XposedService> observer;
    private static boolean registered;

    static synchronized void start() {
        if (registered) return;
        registered = true;
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override public void onServiceBind(XposedService value) { update(value); }
            @Override public void onServiceDied(XposedService value) { update(null); }
        });
    }

    /** Called right away with the current service (null until LSPosed binds), then on every change. */
    static synchronized void observe(Consumer<XposedService> value) {
        observer = value;
        XposedService current = service;
        if (value != null) MAIN.post(() -> value.accept(current));
    }

    private static synchronized void update(XposedService value) {
        service = value;
        Consumer<XposedService> target = observer;
        if (target != null) MAIN.post(() -> target.accept(value));
    }

    private ServiceHolder() { }
}
