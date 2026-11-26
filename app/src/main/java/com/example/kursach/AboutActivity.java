package com.example.kursach;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.aboutRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.aboutToolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        TextView buildNumber = findViewById(R.id.aboutBuildNumber);
        TextView buildVersion = findViewById(R.id.aboutBuildVersion);
        TextView packageName = findViewById(R.id.aboutPackageName);
        packageName.setText(getPackageName());
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            buildNumber.setText(Long.toString(PackageInfoCompat.getLongVersionCode(info)));
            if (info.versionName != null) buildVersion.setText(info.versionName);
        } catch (PackageManager.NameNotFoundException ignored) {
            buildNumber.setText(R.string.about_unknown);
        }

        findViewById(R.id.aboutCopy).setOnClickListener(view -> {
            String details = getString(R.string.about_copy_details, buildNumber.getText(),
                    buildVersion.getText(), packageName.getText());
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.support_about), details));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(this, R.string.about_copied, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.aboutCheckUpdates).setOnClickListener(view ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.about_updates)
                        .setMessage(R.string.about_updates_unavailable)
                        .setPositiveButton(android.R.string.ok, null)
                        .show());
    }
}
