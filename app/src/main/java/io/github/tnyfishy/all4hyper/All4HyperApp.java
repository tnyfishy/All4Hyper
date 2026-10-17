package io.github.tnyfishy.all4hyper;

import android.app.Application;

/** Connects to LSPosed as soon as the app starts; the service arrives through a binder later. */
public final class All4HyperApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        ServiceHolder.start();
    }
}
