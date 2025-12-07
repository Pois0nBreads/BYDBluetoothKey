package net.pxst.mybluetoothkey;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.byd.jnitest.JNI;
import com.byd.jnitest.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

public class MainActivity extends BlueToothActivity implements View.OnClickListener {

    private MyApplication myApplication = null;
    private JNI jni = null;
    private BluetoothDevice bluetoothDevice = null;
    private CommThread commThread = null;
    private AlertDialog processDialog;
    private AlertDialog connectDialog;

    private String username;
    private String password;
    private String macAddress;
    private String devName;

    @Override
    protected void onStart() {
        myApplication = (MyApplication) getApplication();
        myApplication.setActivity(this);
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        username = mSharedPreferences.getString(PREFERENCES_USERNAME, "");
        password = mSharedPreferences.getString(PREFERENCES_PASSWORD, "");
        macAddress = mSharedPreferences.getString(PREFERENCES_MAC_ADDRESS, "");
        devName = mSharedPreferences.getString(PREFERENCES_DEV_NAME, "");

        processDialog = new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("正在操作...")
                .setCancelable(false)
                .create();
        connectDialog = new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("正在连接设备...\n设备名称:" + devName + "\n设备地址:" + macAddress)
                .setPositiveButton("退出程序", (dialog, which) -> finish())
                .setCancelable(false)
                .create();
        connectDialog.show();
        findViewById(R.id.lock_btn).setOnClickListener(this);
        findViewById(R.id.unlock_btn).setOnClickListener(this);
        findViewById(R.id.truck_btn).setOnClickListener(this);
        findViewById(R.id.power_btn).setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("是否开启后备箱")
                    .setPositiveButton("确认", (dialog, which) -> onClick(v))
                    .setNegativeButton("取消", null)
                    .create().show();
            return true;
        });
        try {
            jni = new JNI(username, password);
        } catch (UnsupportedEncodingException e) {
            finish();
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressLint("MissingPermission")
    protected void hasPermission() {
        super.hasPermission();
        //第一次使用跳转到设置界面
        if (mSharedPreferences.getBoolean(PREFERENCES_FIRST_USE, true)) {
            startActivity(new Intent(this, SettingActivity.class));
            return;
        }
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress);
        if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
            Toast.makeText(myApplication, "设备未配对", Toast.LENGTH_SHORT).show();
            startActivity(new Intent("android.settings.BLUETOOTH_SETTINGS"));
            finish();
        } else if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            //开始连接到蓝牙
            new Thread(() -> {
                try {
                    //循环连接设备
                    while (!isFinishing()) {
                        if (this.commThread != null && this.commThread.isRun()) {
                            connectDialog.dismiss();
                            try {
                                Thread.sleep(1000);
                            } catch (Exception ignored){}
                            continue;
                        }
                        if (!connectDialog.isShowing()) {
                            runOnUiThread(()->Toast.makeText(myApplication, "蓝牙断线,正在重连...", Toast.LENGTH_SHORT).show());
                            connectDialog.show();
                        }
                        try {
                            Method method = bluetoothDevice.getClass().getMethod("createRfcommSocket", int.class);
                            BluetoothSocket bluetoothSocket = (BluetoothSocket) method.invoke(bluetoothDevice, 1);
                            this.commThread = new CommThread(bluetoothSocket);
                            this.commThread.start(); //这行忘写了
                        } catch (IOException e) {
                            runOnUiThread(()->Toast.makeText(myApplication, "连接超时,正在重试...", Toast.LENGTH_SHORT).show());
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    runOnUiThread(()->{
                        Toast.makeText(myApplication, "连接出错", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    e.printStackTrace();
                }
            }).start();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("错误")
                    .setMessage("未知的蓝牙设备状态")
                    .setOnDismissListener(dialog -> finish())
                    .setPositiveButton("OK", null)
                    .create().show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commThread != null)
            commThread.endRun();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        if (this.commThread == null || !this.commThread.isRun()) {
            Toast.makeText(myApplication, "蓝牙未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        processDialog.show();
        byte[] cmd;
        switch (v.getId()) {
            case R.id.lock_btn:
                cmd = jni.buildLockRequest();
                break;
            case R.id.unlock_btn:
                cmd = jni.buildUnLockRequest();
                break;
            case R.id.truck_btn:
                cmd = jni.buildOpenTruckRequest();
                break;
            case R.id.power_btn:
                cmd = jni.controlPower();
                break;
            default:
                cmd = jni.buildInvalidRequest();
                break;
        }
        boolean sendState = commThread.sendData(cmd, (statusCode, errorCode) -> {
            Log.d("MainActivity", "statusCode: " + Utils.byte2HEX((byte) statusCode));
            Log.d("MainActivity", "errorCode: " + Utils.byte2HEX((byte) errorCode));
            runOnUiThread(() -> {
                String msg = CommThread.getMessageByStatusCode(statusCode);
                if (statusCode == CommThread.STATE_ERR)
                    msg += CommThread.getMessageByErrorCode(errorCode);
                Toast.makeText(myApplication, msg, Toast.LENGTH_SHORT).show();
                processDialog.dismiss();
            });
        }, 3000);
        if (!sendState) {
            processDialog.dismiss();
            Toast.makeText(myApplication, "发送失败", Toast.LENGTH_SHORT).show();
        }
    }
}