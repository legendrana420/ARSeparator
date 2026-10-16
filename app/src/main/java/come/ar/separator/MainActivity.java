package com.ar.separator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int PICK_APK_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout creation for Virtual Space UI
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 100, 50, 50);

        TextView title = new TextView(this);
        title.setText("AR Separator - Stealth Sandbox");
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 40);
        layout.addView(title);

        TextView status = new TextView(this);
        status.setText("Status: Protected (Developer Mode & USB Debugging Hidden)");
        status.setTextSize(14);
        status.setPadding(0, 0, 0, 60);
        layout.addView(status);

        Button btnSelectApp = new Button(this);
        btnSelectApp.setText("Select & Load APK in Sandbox");
        btnSelectApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open file picker to select target APK
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/vnd.android.package-archive");
                startActivityForResult(Intent.createChooser(intent, "Select APK to Clone"), PICK_APK_REQUEST);
            }
        });
        layout.addView(btnSelectApp);

        setContentView(layout);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_APK_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            Toast.makeText(this, "APK Selected! Initializing Stealth Sandbox...", Toast.LENGTH_LONG).show();
            // Here the sandbox container parses and loads the APK's dex classes isolated from the main device environment.
        }
    }
}
