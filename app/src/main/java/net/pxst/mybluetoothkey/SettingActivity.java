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

    private EditText devNameEdit;
    private EditText devMacEdit;
    private BluetoothDevicesAdapter adapter;
    private final ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();

    @Override
    @SuppressLint("MissingPermission")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        findViewById(R.id.setting_save_btn).setOnClickListener(this);
        findViewById(R.id.setting_reflush_btn).setOnClickListener(this);

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
            case R.id.setting_reflush_btn:
                getBondBLElist();
                break;
        }
    }
}