package net.pxst.mybluetoothkey;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.byd.jnitest.JNI;
import com.byd.jnitest.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

public class MainActivity extends BlueToothActivity implements View.OnClickListener {

    private final static String TAG = "MainActivity";

    private final JNI jni = new JNI();
    private BluetoothDevice bluetoothDevice = null;
    private final BleCommunicator commThread;

    private AlertDialog processDialog;
    private AlertDialog connectDialog;
    private DriverDialog driverDialog;
    private UserPassDialog userPassDialog;
    private RegisterDialog registerDialog;
    private ChangeBtNameDialog changeBtNameDialog;
    private ChangeBtPinDialog changeBtPinDialog;

    private Thread mBluetoothThread;
    private Intent settingIntent;

    private TextView currutUserTT;

    private String macAddress;
    private String devName;
    private boolean isShowing = false;

    {
        this.commThread = new BleCommunicator(() -> runOnUiThread(() -> {
            connectDialog.dismiss();
        }),() -> runOnUiThread(() -> {
            if (isDestroyed())
                return;
            userPassDialog.dismiss();
            processDialog.dismiss();
            driverDialog.dismiss();
            registerDialog.dismiss();
            changeBtNameDialog.dismiss();
            changeBtPinDialog.dismiss();
            connectDialog.show();
            Toast.makeText(myApplication, "蓝牙断线,正在重连...", Toast.LENGTH_SHORT).show();
        }));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settingIntent = new Intent(this, SettingActivity.class);
        settingIntent.putExtra(MyApplication.INTENT_FROM_WHAT, MyApplication.INTENT_FROM_MAIN);

        macAddress = mSharedPreferences.getString(MyApplication.PREFERENCES_MAC_ADDRESS, null);
        devName = mSharedPreferences.getString(MyApplication.PREFERENCES_DEV_NAME, "NaN Name");

        initView();
        initConnectThread();
        setBleUserPass(mSharedPreferences.getString(MyApplication.PREFERENCES_USERNAME, ""),
                mSharedPreferences.getString(MyApplication.PREFERENCES_PASSWORD, ""));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("切换蓝牙设备").setOnMenuItemClickListener(item -> {
            startActivity(settingIntent);
            return false;
        });
        menu.add("注册蓝牙钥匙").setOnMenuItemClickListener(item -> {
            registerDialog.show();
            return false;
        });
        menu.add("修改车辆蓝牙名称").setOnMenuItemClickListener(item -> {
            changeBtNameDialog.show();
            return false;
        });
        menu.add("修改车辆蓝牙PIN").setOnMenuItemClickListener(item -> {
            changeBtPinDialog.show();
            return false;
        });
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 设置蓝牙用户名密码
     */
    @SuppressLint("SetTextI18n")
    private void setBleUserPass(String username, String password) {
        try {
            jni.setUserPass(username, password);
            currutUserTT.setText("当前登录用户：" + username);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException:" + e.getMessage());
            finish();
        }
    }

    /**
     * 测试蓝牙用户名密码
     */
    private void testBleUserPass(String username, String password) {
        if (!this.commThread.isRun()) {
            Toast.makeText(myApplication, "蓝牙未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        processDialog.show();
        byte[] cmd = null;
        try {
            cmd = jni.buildLoginRequest(username.getBytes(Utils.CODE_MAP_2132), password.getBytes(Utils.CODE_MAP_2132));
        } catch (UnsupportedEncodingException e) {
            processDialog.dismiss();
            Toast.makeText(myApplication, "测试失败:" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        boolean sendState = commThread.sendData(cmd, (statusCode, errorCode) -> {
            Log.d("MainActivity", "statusCode: " + Utils.byte2HEX((byte) statusCode));
            Log.d("MainActivity", "errorCode: " + Utils.byte2HEX((byte) errorCode));
            runOnUiThread(() -> {
                String msg = BleCommunicator.getMessageByStatusCode(statusCode);
                if (statusCode == BleCommunicator.STATE_ERR)
                    msg += BleCommunicator.getMessageByErrorCode(errorCode);
                Toast.makeText(myApplication, msg, Toast.LENGTH_SHORT).show();
                processDialog.dismiss();
            });
        }, 3000);
        if (!sendState) {
            processDialog.dismiss();
            Toast.makeText(myApplication, "命令发送失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 初始化界面
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        driverDialog = new DriverDialog(this, commThread, jni);
        userPassDialog = new UserPassDialog(this, this::setBleUserPass, this::testBleUserPass);
        registerDialog = new RegisterDialog(this, commThread, this::setBleUserPass);
        changeBtNameDialog = new ChangeBtNameDialog(this, commThread, jni);
        changeBtPinDialog = new ChangeBtPinDialog(this, commThread, jni);

        processDialog = new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("正在操作...")
                .setCancelable(false)
                .create();
        connectDialog = new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("正在连接设备...\n设备名称:" + devName + "\n设备地址:" + macAddress)
                .setPositiveButton("退出程序", (dialog, which) -> finish())
                .setNeutralButton("切换蓝牙设备", (dialog, which) -> startActivity(settingIntent))
                .setCancelable(false)
                .create();

        currutUserTT = findViewById(R.id.main_activity_user_tt);
        findViewById(R.id.main_activity_change_user_btn).setOnClickListener(v -> userPassDialog.show());

        findViewById(R.id.lock_btn).setOnClickListener(this);
        findViewById(R.id.unlock_btn).setOnClickListener(this);
        findViewById(R.id.driver_btn).setOnLongClickListener(v -> {
            driverDialog.show();
            return true;
        });
        findViewById(R.id.truck_btn).setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("是否开启后备箱")
                    .setPositiveButton("确认", (dialog, which) -> onClick(v))
                    .setNegativeButton("取消", null)
                    .create().show();
            return true;
        });
        findViewById(R.id.power_btn).setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("是否启动/熄火\n启动会打开空调")
                    .setPositiveButton("确认", (dialog, which) -> onClick(v))
                    .setNegativeButton("取消", null)
                    .create().show();
            return true;
        });
    }

    /**
     * 初始化蓝牙连接线程
     */
    private void initConnectThread() {
        this.mBluetoothThread = new Thread(() -> {
            try {
                //循环连接设备
                Method method = BluetoothDevice.class.getMethod("createRfcommSocket", int.class);
                do {
                    if (!this.isShowing)
                        continue;
                    if (this.commThread.isRun())
                        continue;
                    try {
                        BluetoothSocket bluetoothSocket = (BluetoothSocket) method.invoke(bluetoothDevice, 1);
                        this.commThread.start(bluetoothSocket); //这行忘写了
                    } catch (IOException e) {
                        if (isDestroyed())
                            return;
                        runOnUiThread(() -> Toast.makeText(myApplication, "连接超时,正在重试...", Toast.LENGTH_SHORT).show());
                        Log.e(TAG, Thread.currentThread().getName() + ": 连接超时,正在重试...", e);
                    } finally {
                        if (isDestroyed()) {
                            this.commThread.stop();
                            Log.i(TAG, Thread.currentThread().getName() + ": 程序已退出，结束不该有的commThread");
                        }
                    }
                } while (!isDestroyed());
            } catch (Exception e) {
                Log.e(TAG, "连接出错", e);
                if (!isDestroyed())
                    runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("错误")
                        .setMessage("连接出错")
                        .setOnDismissListener(dialog -> finish())
                        .setPositiveButton("OK", null)
                        .create().show());
            }
        });
    }

    /**
     * 获得蓝牙权限
     */
    @Override
    @SuppressLint("MissingPermission")
    protected void hasPermission() {
        super.hasPermission();
        //第一次使用跳转到设置界面
        if (macAddress == null) {
            startActivity(settingIntent);
            return;
        }
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress);
        connectDialog.show();
        if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
            Toast.makeText(myApplication, "设备未配对", Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(this)
                    .setTitle("错误")
                    .setMessage("设备未配对")
                    .setOnDismissListener(dialog -> {
                        startActivity(new Intent("android.settings.BLUETOOTH_SETTINGS"));
                        finish();
                    })
                    .setPositiveButton("OK", null)
                    .create().show();
        } else if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            //开始连接到蓝牙
            if (!mBluetoothThread.isAlive())
                mBluetoothThread.start();
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
    protected void onStart() {
        isShowing = true;
        super.onStart();
    }

    @Override
    protected void onPause() {
        driverDialog.dismiss();
        userPassDialog.dismiss();
        registerDialog.dismiss();
        changeBtNameDialog.dismiss();
        changeBtPinDialog.dismiss();
        super.onPause();
    }

    @Override
    protected void onStop() {
        isShowing = false;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        new Thread(commThread::destroy).start();
        super.onDestroy();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        if (!this.commThread.isRun()) {
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
        new Thread() {
            @Override
            public void run() {
                boolean sendState = commThread.sendData(cmd, (statusCode, errorCode) -> {
                    Log.d("MainActivity", "statusCode: " + Utils.byte2HEX((byte) statusCode));
                    Log.d("MainActivity", "errorCode: " + Utils.byte2HEX((byte) errorCode));
                    runOnUiThread(() -> {
                        String msg = BleCommunicator.getMessageByStatusCode(statusCode);
                        if (statusCode == BleCommunicator.STATE_ERR)
                            msg += BleCommunicator.getMessageByErrorCode(errorCode);
                        Toast.makeText(myApplication, msg, Toast.LENGTH_SHORT).show();
                        processDialog.dismiss();
                    });
                }, 3000);
                if (!sendState) {
                    runOnUiThread(() -> {
                        processDialog.dismiss();
                        Toast.makeText(myApplication, "发送失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }.start();
    }

}