package net.schwarzbaer.android.diskusage;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FileSystemScanner scanner = FileSystemScanner.getInstance();

        TextView txtScanOutput = findViewById(R.id.txtScanOutput);
        String scanLog = scanner.getScanLog();
        txtScanOutput.setText(scanLog);

        Button btnScanDisk = findViewById(R.id.btnScanDisk);
        btnScanDisk.setText("Scan Disk"+(scanner.wasScanned() ? " (✔)" : ""));
    }

    public void clickScanBtn(View view) {
        checkPermissionAndStartScan();
    }

    public void clickShowBtn(View view) {
//        Intent intent = new Intent(this, FileListActivity.class);
//        intent.putExtra(FileListActivity.activityParam_Path, "[PathRoot]");
//        startActivity(intent);
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan("After old permission granted");
            } else {
                TextView txtScanOutput = findViewById(R.id.txtScanOutput);
                txtScanOutput.setText("<access permission denied> :(");
            }
        }
    }

    private void startScan(String caller) {
        TextView txtScanOutput = findViewById(R.id.txtScanOutput);
        FileSystemScanner.getInstance().scan(new TextViewWriter(txtScanOutput));
    }
}