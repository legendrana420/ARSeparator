package com.ar.separator;

import android.content.ContentResolver;
import android.provider.Settings;

public class AntiDebugHook {

    // Bypass USB Debugging & Developer Options Checks
    public static int getIntHook(ContentResolver resolver, String name, int def) {
        if (Settings.Global.ADB_ENABLED.equals(name) || 
            Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED.equals(name)) {
            // Force return 0 (Disabled) to hide flags from cloned apps
            return 0;
        }
        return Settings.Secure.getInt(resolver, name, def);
    }

    // Bypass Debugger Connection Check
    public static boolean isDebuggerConnectedHook() {
        // Always return false so apps never detect active debuggers
        return false;
    }
}
