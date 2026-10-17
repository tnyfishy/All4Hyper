package io.github.tnyfishy.all4hyper

import android.app.Application

/** Starts the LSPosed listener at process start, before any UI asks for it. */
class All4HyperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Connection.start()
    }
}
