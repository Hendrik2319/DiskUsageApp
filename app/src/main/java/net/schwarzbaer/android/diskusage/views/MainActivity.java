package net.schwarzbaer.android.diskusage.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.schwarzbaer.android.diskusage.databinding.ActivityMainBinding;
import net.schwarzbaer.android.diskusage.models.FileSystemScanner;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FileSystemScanner scanner = FileSystemScanner.getInstance();

        String scanLog = scanner.getScanLog();
        binding.txtScanOutput.setText(scanLog);

        updateScanBtn(scanner);
    }

    private void updateScanBtn(FileSystemScanner scanner) {
        binding.btnScanDisk.setText(String.format("Scan Disk%s", scanner.wasScanned() ? " (✔)" : ""));
    }

    public void clickScanBtn(View view) {
        checkPermissionAndStartScan();
    }

    public void clickShowBtn(View view) {
        FileSystemScanner scanner = FileSystemScanner.getInstance();
        if (scanner.hasStorages()) {
            Intent intent = new Intent(this, StorageListActivity.class);
            //intent.putExtra(FileListActivity.activityParam_Path, "[PathRoot]");
            startActivity(intent);
        }
    }

    public void checkPermissionAndStartScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            } else {
                startScan("With granted new permission");
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE }, 1);
            } else {
                startScan("With granted old permission");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan("After old permission granted");
            } else {
                binding.txtScanOutput.setText("<access permission denied> :(");
            }
        }
    }

    private void startScan(String caller) {
        FileSystemScanner scanner = FileSystemScanner.getInstance();
        setUIEnabled(false);
        new Thread(() -> {
            scanner.scan( new UiThreadSafeTextViewWriter( binding.txtScanOutput, this::runOnUiThread ) );
            runOnUiThread(()->{
                updateScanBtn(scanner);
                setUIEnabled(true);
            });
        }).start();
    }

    private void setUIEnabled(boolean enabled)
    {
        binding.btnScanDisk.setEnabled(enabled);
        binding.btnShowDisk.setEnabled(enabled);
    }
}