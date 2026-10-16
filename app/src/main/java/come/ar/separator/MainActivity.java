package com.ar.separator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PICK_APK_REQUEST = 101;
    private LinearLayout sandboxListContainer;
    private File sandboxDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AntiDebugHook.applyStealthHooks();

        sandboxDir = new File(getFilesDir(), "sandbox_apks");
        if (!sandboxDir.exists()) {
            sandboxDir.mkdirs();
        }

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 80, 40, 40);

        TextView title = new TextView(this);
        title.setText("AR Separator - Stealth Sandbox");
        title.setTextSize(22);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 0, 0, 15);
        root.addView(title);

        TextView status = new TextView(this);
        status.setText("Status: Protected (ADB & Developer Mode Masked = 0)");
        status.setTextSize(14);
        status.setTextColor(Color.rgb(0, 130, 0));
        status.setPadding(0, 0, 0, 40);
        root.addView(status);

        // Button 1: Import Installed App (Play Store / Phone Apps)
        Button btnImportInstalled = new Button(this);
        btnImportInstalled.setText("+ Import Installed / Play Store App");
        btnImportInstalled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInstalledAppsDialog();
            }
        });
        root.addView(btnImportInstalled);

        // Button 2: Pick APK File from Storage
        Button btnSelectApk = new Button(this);
        btnSelectApk.setText("+ Select APK File from Storage");
        btnSelectApk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Select APK File"), PICK_APK_REQUEST);
            }
        });
        root.addView(btnSelectApk);

        TextView listHeader = new TextView(this);
        listHeader.setText("\n--- Apps Inside AR Separator Sandbox ---");
        listHeader.setTextSize(16);
        listHeader.setTextColor(Color.DKGRAY);
        listHeader.setPadding(0, 30, 0, 20);
        root.addView(listHeader);

        sandboxListContainer = new LinearLayout(this);
        sandboxListContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(sandboxListContainer);

        scrollView.addView(root);
        setContentView(scrollView);

        refreshSandboxList();
    }

    private void showInstalledAppsDialog() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        final List<ApplicationInfo> userApps = new ArrayList<>();
        List<String> appNames = new ArrayList<>();

        for (ApplicationInfo app : allApps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && !app.packageName.equals(getPackageName())) {
                userApps.add(app);
                appNames.add(pm.getApplicationLabel(app).toString() + "\n(" + app.packageName + ")");
            }
        }

        if (userApps.isEmpty()) {
            Toast.makeText(this, "No user apps found!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select App to Clone into Sandbox");
        builder.setItems(appNames.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ApplicationInfo selected = userApps.get(which);
                importApkFromFile(new File(selected.sourceDir), selected.packageName + ".apk");
            }
        });
        builder.show();
    }

    private void importApkFromFile(File sourceFile, String destName) {
        try {
            File destFile = new File(sandboxDir, destName);
            InputStream in = new FileInputStream(sourceFile);
            FileOutputStream out = new FileOutputStream(destFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            Toast.makeText(this, "Cloned into Sandbox: " + destName, Toast.LENGTH_SHORT).show();
            refreshSandboxList();
        } catch (Exception e) {
            Toast.makeText(this, "Import Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_APK_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    String fileName = "app_" + System.currentTimeMillis() + ".apk";
                    File destFile = new File(sandboxDir, fileName);
                    InputStream in = getContentResolver().openInputStream(uri);
                    FileOutputStream out = new FileOutputStream(destFile);
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                    Toast.makeText(this, "APK Loaded into Sandbox!", Toast.LENGTH_SHORT).show();
                    refreshSandboxList();
                } catch (Exception e) {
                    Toast.makeText(this, "Error loading APK: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void refreshSandboxList() {
        sandboxListContainer.removeAllViews();
        File[] files = sandboxDir.listFiles();
        if (files == null || files.length == 0) {
            TextView empty = new TextView(this);
            empty.setText("No apps in sandbox yet. Tap a button above to add one.");
            sandboxListContainer.addView(empty);
            return;
        }

        PackageManager pm = getPackageManager();
        for (final File apkFile : files) {
            if (!apkFile.getName().endsWith(".apk")) continue;

            PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
            String label = apkFile.getName();
            String pkgName = "Unknown Package";

            if (pkgInfo != null && pkgInfo.applicationInfo != null) {
                pkgInfo.applicationInfo.sourceDir = apkFile.getAbsolutePath();
                pkgInfo.applicationInfo.publicSourceDir = apkFile.getAbsolutePath();
                CharSequence cs = pm.getApplicationLabel(pkgInfo.applicationInfo);
                if (cs != null) label = cs.toString();
                pkgName = pkgInfo.packageName;
            }

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(25, 25, 25, 25);
            itemLayout.setBackgroundColor(Color.rgb(240, 240, 240));

            TextView tvName = new TextView(this);
            tvName.setText(label + " (" + pkgName + ")");
            tvName.setTextSize(16);
            tvName.setTextColor(Color.BLACK);
            itemLayout.addView(tvName);

            Button btnLaunch = new Button(this);
            btnLaunch.setText("Run in Stealth Sandbox");
            btnLaunch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(MainActivity.this, ProxyActivity.class);
                    i.putExtra("apk_path", apkFile.getAbsolutePath());
                    startActivity(i);
                }
            });
            itemLayout.addView(btnLaunch);

            Button btnDelete = new Button(this);
            btnDelete.setText("Remove from Sandbox");
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    apkFile.delete();
                    refreshSandboxList();
                }
            });
            itemLayout.addView(btnDelete);

            View spacer = new View(this);
            spacer.setMinimumHeight(20);

            sandboxListContainer.addView(itemLayout);
            sandboxListContainer.addView(spacer);
        }
    }
}
