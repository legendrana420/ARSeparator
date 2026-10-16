package com.ar.separator;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class ProxyActivity extends Activity {

    private Resources pluginResources;
    private AssetManager pluginAssets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AntiDebugHook.applyStealthHooks();
        super.onCreate(savedInstanceState);

        String inputPath = getIntent().getStringExtra("apk_path");
        if (inputPath == null) {
            finish();
            return;
        }

        try {
            File srcFile = new File(inputPath);
            File codeCacheDir = getCodeCacheDir();
            File safeApkFile = new File(codeCacheDir, "safe_target.apk");

            if (safeApkFile.exists()) {
                safeApkFile.delete();
            }

            InputStream in = new FileInputStream(srcFile);
            FileOutputStream out = new FileOutputStream(safeApkFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();

            safeApkFile.setReadOnly();
            String apkPath = safeApkFile.getAbsolutePath();

            PackageManager pm = getPackageManager();
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);

            if (pkgInfo == null || pkgInfo.activities == null || pkgInfo.activities.length == 0) {
                showErrorScreen("Error: No launcher activity found in this APK.");
                return;
            }

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
            Object targetInstance = targetClass.getDeclaredConstructor().newInstance();

            if (targetInstance instanceof Activity) {
                Activity targetAct = (Activity) targetInstance;
                
                Method attachBaseContext = Activity.class.getDeclaredMethod("attachBaseContext", Context.class);
                attachBaseContext.setAccessible(true);
                attachBaseContext.invoke(targetAct, this);

                Bundle bundle = new Bundle();
                Method onCreate = Activity.class.getDeclaredMethod("onCreate", Bundle.class);
                onCreate.setAccessible(true);
                onCreate.invoke(targetAct, bundle);
                return;
            }

            showErrorScreen("Stealth Sandbox Active!\n\nLoaded Package: " + pkgInfo.packageName + "\nStatus: Protected from Developer Mode & Debugging Checks.");

        } catch (Throwable e) {
            showErrorScreen("Sandbox Execution Error:\n\n" + e.toString());
        }
    }

    private void showErrorScreen(String message) {
        ScrollView sv = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 80, 40, 40);

        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(16);
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
