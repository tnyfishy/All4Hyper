package io.github.tnyfishy.all4hyper;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import io.github.libxposed.service.XposedService;

public final class MainActivity extends Activity {
    private TextView status;
    private Switch appOps;
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        appOps = findViewById(R.id.appops_switch);
        appOps.setOnCheckedChangeListener((view, checked) -> {
            if (prefs != null) prefs.edit().putBoolean(Prefs.APPOPS_UNRESTRICT, checked).apply();
        });
        ServiceHolder.observe(this::onService);
    }

    @Override protected void onDestroy() {
        ServiceHolder.observe(null);
        super.onDestroy();
    }

    private void onService(XposedService service) {
        if (isDestroyed()) return;
        prefs = null;
        SharedPreferences current = null;
        boolean scoped = false;
        if (service != null) {
            try {
                current = service.getRemotePreferences(Prefs.GROUP);
                scoped = service.getScope().contains(All4HyperModule.SECURITY_CENTER);
            } catch (Throwable ignored) {
                // Shown below as not connected.
            }
        }
        if (current == null) {
            status.setText(R.string.status_inactive);
            appOps.setEnabled(false);
            return;
        }
        status.setText(getString(scoped ? R.string.status_active : R.string.status_no_scope,
                service.getFrameworkName(), service.getApiVersion()));
        // prefs is still null here, so restoring the saved value writes nothing back.
        appOps.setChecked(current.getBoolean(Prefs.APPOPS_UNRESTRICT, false));
        prefs = current;
        appOps.setEnabled(true);
    }
}
