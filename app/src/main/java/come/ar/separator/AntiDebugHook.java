package com.ar.separator;

import android.content.ContentResolver;
import android.provider.Settings;
import java.lang.reflect.Field;
import java.util.HashMap;

public class AntiDebugHook {

    public static void applyStealthHooks() {
        try {
            // 1. Mask Developer Options & USB Debugging
            hookSettingsCache(Settings.Global.class);
            hookSettingsCache(Settings.Secure.class);
            
            // 2. Hide system debug flags
            System.setProperty("ro.debuggable", "0");
            System.setProperty("service.adb.root", "0");
            System.setProperty("ro.secure", "1");
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static void hookSettingsCache(Class<?> settingsClass) {
        try {
            Field cacheField = settingsClass.getDeclaredField("sNameValueCache");
            cacheField.setAccessible(true);
            Object cacheHolder = cacheField.get(null);
            if (cacheHolder != null) {
                Field valuesField = cacheHolder.getClass().getDeclaredField("mValues");
                valuesField.setAccessible(true);
                HashMap<String, String> values = (HashMap<String, String>) valuesField.get(cacheHolder);
                if (values == null) {
                    values = new HashMap<>();
                    valuesField.set(cacheHolder, values);
                }
                // Force return "0" (Disabled) for all security/debug keys
                values.put(Settings.Global.ADB_ENABLED, "0");
                values.put(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, "0");
                values.put(Settings.Secure.ADB_ENABLED, "0");
                values.put(Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, "0");
                values.put(Settings.Secure.ALLOW_MOCK_LOCATION, "0");
                values.put("mock_location", "0");
            }
        } catch (Throwable ignored) {
        }
    }
}
