package com.ar.separator;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class ProxyActivity extends Activity {

    private Resources pluginResources;
    private AssetManager pluginAssets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AntiDebugHook.applyStealthHooks();
        super.onCreate(savedInstanceState);

        String apkPath = getIntent().getStringExtra("apk_path");
        if (apkPath == null) {
            finish();
            return;
        }

        try {
            PackageManager pm = getPackageManager();
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);

            if (pkgInfo == null || pkgInfo.activities == null || pkgInfo.activities.length == 0) {
                showStatus("Error: No launchable Activity found inside APK.");
                return;
            }

            // Initialize custom AssetManager for the cloned APK
            AssetManager am = AssetManager.class.newInstance();
            Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
            addAssetPath.invoke(am, apkPath);
            pluginAssets = am;

            Resources superRes = super.getResources();
            pluginResources = new Resources(am, superRes.getDisplayMetrics(), superRes.getConfiguration());

            File dexOptDir = getDir("dex_opt", MODE_PRIVATE);
            DexClassLoader dexClassLoader = new DexClassLoader(
                    apkPath,
                    dexOptDir.getAbsolutePath(),
                    null,
                    getClassLoader()
            );

            ActivityInfo targetActivityInfo = pkgInfo.activities[0];
            Class<?> targetClass = dexClassLoader.loadClass(targetActivityInfo.name);

            showStatus("Stealth Sandbox Active!\n\n" +
                    "Loaded Package: " + pkgInfo.packageName + "\n" +
                    "Entry Activity: " + targetClass.getName() + "\n" +
                    "ADB_ENABLED Check: " + android.provider.Settings.Global.getInt(getContentResolver(), android.provider.Settings.Global.ADB_ENABLED, 0) + " (Hidden)\n" +
                    "DEV_MODE Check: " + android.provider.Settings.Global.getInt(getContentResolver(), android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) + " (Hidden)\n" +
                    "Debugger Connected: " + android.os.Debug.isDebuggerConnected());

        } catch (Throwable e) {
            showStatus("Sandbox Execution Info:\n" + e.toString());
        }
    }

    private void showStatus(String message) {
        ScrollView sv = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 80, 40, 40);

        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(15);
        tv.setTextColor(Color.BLACK);
        layout.addView(tv);

        sv.addView(layout);
        setContentView(sv);
    }

    @Override
    public Resources getResources() {
        return pluginResources != null ? pluginResources : super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        return pluginAssets != null ? pluginAssets : super.getAssets();
    }
}
