package net.pxst.mybluetoothkey;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

public class BlueToothActivity extends Activity {

    protected static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    protected static String[] PERMISSION_LIST;
    protected MyApplication myApplication = null;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PERMISSION_LIST = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        else
            PERMISSION_LIST = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
    }

    protected BluetoothAdapter bluetoothAdapter = null;
    protected SharedPreferences mSharedPreferences = null;
    private boolean initFinished = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myApplication = (MyApplication) getApplication();
        myApplication.setActivity(this);
        mSharedPreferences = getSharedPreferences(MyApplication.PREFERENCES_SETTINGS, MODE_PRIVATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (initFinished)
            return;
        initFinished = true;
        if (checkPermissionAllGranted()) {
            hasPermission();
        } else {
            ActivityCompat.requestPermissions(this, PERMISSION_LIST, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean isAllGranted = true;
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }
            if (isAllGranted) {
                // 权限已被授予
                hasPermission();
            } else {
                // 权限被拒绝
                new AlertDialog.Builder(this)
                        .setTitle("无权限")
                        .setMessage("缺少权限")
                        .setOnDismissListener(dialog -> finish())
                        .create().show();
            }
        }
    }

    /**
     * 检查权限列表
     *
     * @return 是否都有权限
     */
    protected boolean checkPermissionAllGranted() {
        for (String permission : BlueToothActivity.PERMISSION_LIST)
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        return true;
    }

    @SuppressLint("MissingPermission")
    protected void hasPermission() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
            Toast.makeText(this, "正在打开蓝牙", Toast.LENGTH_SHORT).show();
        }
    }
}
