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

public class ChangeBtNameDialog {

    private final static String TAG = "ChangeBtNameDialog";

    private final AlertDialog dialog, loadingDialog;
    private final Activity activity;
    private final BleCommunicator commThread;

    private final EditText nameEdit;
    private final JNI jni;

    public ChangeBtNameDialog(Activity activity, BleCommunicator commThread, JNI jni) {
        this.activity = activity;
        this.commThread = commThread;
        this.jni = jni;
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_change_bt_name, null);
        nameEdit = view.findViewById(R.id.dialog_new_bt_name);
        this.loadingDialog = new AlertDialog.Builder(activity)
                .setTitle("正在提交")
                .setCancelable(false)
                .setView(new ProgressBar(activity))
                .create();
        this.dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setTitle("修改车辆蓝牙名称")
                .setPositiveButton("提交", null)
                .create();
    }

    public void show() {
        if (dialog.isShowing())
            return;
        dialog.show();
        nameEdit.setText("");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameEdit.getText().toString();
            if (name.isEmpty()) {
                Toast.makeText(activity, "蓝牙名称不能为空", Toast.LENGTH_SHORT).show();
            } else if (!name.matches(StringMap.USERNAME_MATCH)) {
                Toast.makeText(activity, "蓝牙名称为6-10个字符(字母/数字)", Toast.LENGTH_SHORT).show();
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
                        commThread.sendData(jni.changeBtNameRequest(name.getBytes(Utils.CODE_MAP_UTF_8)), (resultCode1, errCode1) -> {
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
        });
    }

    public void dismiss() {
        if (dialog.isShowing())
            dialog.dismiss();
    }
}
