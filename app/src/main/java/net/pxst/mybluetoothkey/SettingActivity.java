package net.pxst.mybluetoothkey;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

public class SettingActivity extends BlueToothActivity implements View.OnClickListener {

    private EditText devNameEdit;
    private EditText devMacEdit;
    private BluetoothDevicesAdapter adapter;
    private final ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    private int fromWhat = MyApplication.INTENT_FROM_NONE;

    @Override
    @SuppressLint("MissingPermission")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Objects.requireNonNull(getActionBar()).setTitle("蓝牙设备选择");
        fromWhat = getIntent().getIntExtra(MyApplication.INTENT_FROM_WHAT, MyApplication.INTENT_FROM_NONE);
        findViewById(R.id.setting_save_btn).setOnClickListener(this);
        findViewById(R.id.setting_bond_btn).setOnClickListener(this);

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
        devNameEdit.setText(mSharedPreferences.getString(MyApplication.PREFERENCES_DEV_NAME, ""));
        devMacEdit.setText(mSharedPreferences.getString(MyApplication.PREFERENCES_MAC_ADDRESS, ""));
    }

    @Override
    protected void onResume() {
        getBondBleList();
        super.onResume();
    }

    @Override
    protected void hasPermission() {
        super.hasPermission();
        getBondBleList();
    }

    @SuppressLint("MissingPermission")
    private void getBondBleList() {
        bluetoothDevices.clear();
        bluetoothDevices.addAll(bluetoothAdapter.getBondedDevices());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        switch (fromWhat) {
            case MyApplication.INTENT_FROM_NONE:
                break;
            case MyApplication.INTENT_FROM_MAIN:
                startActivity(new Intent(this, MainActivity.class));
                break;
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.setting_save_btn:
                String devName = devNameEdit.getText().toString();
                String devMAC = devMacEdit.getText().toString();
                if (devMAC.isEmpty()) {
                    Toast.makeText(myApplication, "未选择设备！", Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putString(MyApplication.PREFERENCES_DEV_NAME, devName)
                        .putString(MyApplication.PREFERENCES_MAC_ADDRESS, devMAC)
                        .putBoolean(MyApplication.PREFERENCES_FIRST_USE, false)
                        .apply();
                Toast.makeText(myApplication, "保存成功！", Toast.LENGTH_SHORT).show();
                break;
            case R.id.setting_bond_btn:
                startActivity(new Intent("android.settings.BLUETOOTH_SETTINGS"));
                break;
        }
    }
}