package net.pxst.mybluetoothkey;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class SettingActivity extends BlueToothActivity implements View.OnClickListener {

    private MyApplication myApplication = null;
    private EditText userEdit;
    private EditText passEdit;
    private EditText devNameEdit;
    private EditText devMacEdit;
    private BluetoothDevicesAdapter adapter;
    private final ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();

    @Override
    protected void onStart() {
        myApplication = (MyApplication) getApplication();
        myApplication.setActivity(this);
        super.onStart();
    }

    @Override
    @SuppressLint("MissingPermission")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        findViewById(R.id.setting_save_btn).setOnClickListener(this);
        findViewById(R.id.setting_reflush_btn).setOnClickListener(this);

        userEdit = findViewById(R.id.setting_user_et);
        passEdit = findViewById(R.id.setting_pass_et);
        devNameEdit = findViewById(R.id.setting_dev_name_et);
        devMacEdit = findViewById(R.id.setting_dev_mac_et);
        ListView listView = findViewById(R.id.setting_ble_list);

        adapter = new BluetoothDevicesAdapter(this, bluetoothDevices);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = bluetoothDevices.get(position);
            devNameEdit.setText(device.getName());
            devMacEdit.setText(device.getAddress());
        });

        userEdit.setText(mSharedPreferences.getString(PREFERENCES_USERNAME, ""));
        passEdit.setText(mSharedPreferences.getString(PREFERENCES_PASSWORD, ""));
        devNameEdit.setText(mSharedPreferences.getString(PREFERENCES_DEV_NAME, ""));
        devMacEdit.setText(mSharedPreferences.getString(PREFERENCES_MAC_ADDRESS, ""));
    }

    @Override
    protected void hasPermission() {
        super.hasPermission();
        getBondBLElist();
    }

    @SuppressLint("MissingPermission")
    private void getBondBLElist() {
        bluetoothDevices.clear();
        bluetoothDevices.addAll(bluetoothAdapter.getBondedDevices());
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.setting_save_btn:
                String user = userEdit.getText().toString();
                String pass = passEdit.getText().toString();
                String devName = devNameEdit.getText().toString();
                String devMAC = devMacEdit.getText().toString();
                if (user.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(myApplication, "用户名密码不能为空！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (devMAC.isEmpty()) {
                    Toast.makeText(myApplication, "未选择设备！", Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putString(PREFERENCES_USERNAME, user)
                        .putString(PREFERENCES_PASSWORD, pass)
                        .putString(PREFERENCES_DEV_NAME, devName)
                        .putString(PREFERENCES_MAC_ADDRESS, devMAC)
                        .putBoolean(PREFERENCES_FIRST_USE, false)
                        .apply();
                Toast.makeText(myApplication, "保存成功！", Toast.LENGTH_SHORT).show();
                break;
            case R.id.setting_reflush_btn:
                getBondBLElist();
                break;
        }
    }
}