package net.pxst.mybluetoothkey;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.byd.jnitest.JNI;
import com.byd.jnitest.StringMap;
import com.byd.jnitest.Utils;

import java.io.UnsupportedEncodingException;

public class ChangeBtPinDialog {

    private final static String TAG = "ChangeBtPinDialog";

    private final AlertDialog dialog, loadingDialog;
    private final Activity activity;
    private final BleCommunicator commThread;

    private final EditText pinEdit;
    private final JNI jni;

    public ChangeBtPinDialog(Activity activity, BleCommunicator commThread, JNI jni) {
        this.activity = activity;
        this.commThread = commThread;
        this.jni = jni;
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_change_bt_pin, null);
        pinEdit = view.findViewById(R.id.dialog_new_bt_pin);
        this.loadingDialog = new AlertDialog.Builder(activity)
                .setTitle("正在提交")
                .setCancelable(false)
                .setView(new ProgressBar(activity))
                .create();
        this.dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setTitle("修改车辆蓝牙PIN")
                .setPositiveButton("提交", null)
                .setNegativeButton("取消", null)
                .create();
    }

    public void show() {
        if (dialog.isShowing())
            return;
        dialog.show();
        pinEdit.setText("");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String pin = pinEdit.getText().toString().trim();
            try {
                if (pin.isEmpty() || pin.getBytes(Utils.CODE_MAP_UTF_8).length != 4) {
                    Toast.makeText(activity, "PIN码为4个数字", Toast.LENGTH_SHORT).show();
                } else {
                    loadingDialog.show();
                    //先测试登录
                    new Thread(() -> commThread.sendData(jni.buildLoginRequest(), (resultCode, errCode) -> {
                        if (resultCode != BleCommunicator.STATE_OK) {
                            activity.runOnUiThread(() -> {
                                String msg = BleCommunicator.getMessageByStatusCode(resultCode);
                                if (resultCode == BleCommunicator.STATE_ERR)
                                    msg += BleCommunicator.getMessageByErrorCode(errCode);
                                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                                loadingDialog.dismiss();
                            });
                            return;
                        }
                        //修改蓝牙名称
                        try {
                            commThread.sendData(jni.changeBtPinRequest(pin.getBytes(Utils.CODE_MAP_UTF_8)), (resultCode1, errCode1) -> {
                                if (resultCode1 != BleCommunicator.STATE_OK) {
                                    activity.runOnUiThread(() -> {
                                        String msg = BleCommunicator.getMessageByStatusCode(resultCode1);
                                        if (resultCode1 == BleCommunicator.STATE_ERR)
                                            msg += BleCommunicator.getMessageByErrorCode(errCode1);
                                        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                                        loadingDialog.dismiss();
                                    });
                                    return;
                                }
                                activity.runOnUiThread(() -> {
                                    Toast.makeText(activity, "设置成功,蓝牙可能需要重新配对", Toast.LENGTH_SHORT).show();
                                    loadingDialog.dismiss();
                                    dialog.dismiss();
                                });
                            }, 3000);
                        } catch (UnsupportedEncodingException e) {
                            Log.e(TAG, "UnsupportedEncodingException", e);
                            dismiss();
                        }
                    }, 3000)).start();
                }
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "UnsupportedEncodingException", e);
                dismiss();
            }
        });
    }

    public void dismiss() {
        if (dialog.isShowing())
            dialog.dismiss();
    }
}
