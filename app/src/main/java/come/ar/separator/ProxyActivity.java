import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
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
                finish();
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

            // Find main launcher activity of target APK
            ActivityInfo targetActivityInfo = null;
            for (ActivityInfo act : pkgInfo.activities) {
                if (act.name.contains("MainActivity") || act.name.contains("Splash") || act.name.contains("Entry")) {
                    targetActivityInfo = act;
                    break;
                }
            }
            if (targetActivityInfo == null) {
                targetActivityInfo = pkgInfo.activities[0];
            }

            Class<?> targetClass = dexClassLoader.loadClass(targetActivityInfo.name);
            Object targetActivityInstance = targetClass.getDeclaredConstructor().newInstance();

            // Bind context and launch target app UI inside proxy container
            if (targetActivityInstance instanceof Activity) {
                Activity targetAct = (Activity) targetActivityInstance;
                
                // Use reflection to attach base context so target app UI renders correctly
                Method attachBaseContext = Activity.class.getDeclaredMethod("attachBaseContext", Context.class);
                attachBaseContext.setAccessible(true);
                attachBaseContext.invoke(targetAct, new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault));

                Bundle bundle = new Bundle();
                Method onCreate = Activity.class.getDeclaredMethod("onCreate", Bundle.class);
                onCreate.setAccessible(true);
                onCreate.invoke(targetAct, bundle);
            }

        } catch (Throwable e) {
            e.printStackTrace();
            finish();
        }
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

