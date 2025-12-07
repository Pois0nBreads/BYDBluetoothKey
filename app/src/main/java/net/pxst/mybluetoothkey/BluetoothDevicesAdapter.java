package net.pxst.mybluetoothkey;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class BluetoothDevicesAdapter extends BaseAdapter {

    private List<BluetoothDevice> bluetoothDevices;
    private Context mContext;
    private LayoutInflater inflater;

    public BluetoothDevicesAdapter(Context context, List<BluetoothDevice> bluetoothDevices) {
        this.bluetoothDevices = bluetoothDevices;
        this.mContext = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return bluetoothDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return bluetoothDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("MissingPermission")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DataBean bean;
        if (convertView == null) {
            bean = new DataBean();
            convertView = inflater.inflate(R.layout.ble_device_item, null);
            bean.nameTT = convertView.findViewById(R.id.item_name);
            bean.nameMAC = convertView.findViewById(R.id.item_mac);
            convertView.setTag(bean);
        } else {
            bean = (DataBean) convertView.getTag();
        }
        BluetoothDevice device = (BluetoothDevice) getItem(position);
        bean.nameTT.setText(device.getName());
        bean.nameMAC.setText(device.getAddress());
        return convertView;
    }

    static class DataBean {
        TextView nameTT;
        TextView nameMAC;
    }
}
