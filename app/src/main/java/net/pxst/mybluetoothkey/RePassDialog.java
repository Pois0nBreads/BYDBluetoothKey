package net.pxst.mybluetoothkey;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.byd.jnitest.JNI;
import com.byd.jnitest.StringMap;
import com.byd.jnitest.Utils;

import java.io.UnsupportedEncodingException;

public class RePassDialog {
    private final static String TAG = "RePassDialog";

    private final AlertDialog dialog, loadingDialog;
    private final SharedPreferences sharedPreferences;
    private final OnUserPassChangeListener userPassChangeListener;
    private final Activity activity;
    private final BleCommunicator communicator;
    private final JNI jni;

    private final EditText oldpassEdit, newpassEdit, repassEdit;

    RePassDialog(Activity activity, BleCommunicator communicator, JNI jni, OnUserPassChangeListener userPassChangeListener) {
        this.sharedPreferences = activity.getSharedPreferences(MyApplication.PREFERENCES_SETTINGS, Context.MODE_PRIVATE);
        this.activity = activity;
        this.communicator = communicator;
        this.jni = jni;
        this.userPassChangeListener = userPassChangeListener;
        this.loadingDialog = new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle("正在提交")
                .setView(new ProgressBar(activity))
                .create();

        View view = activity.getLayoutInflater().inflate(R.layout.dialog_repass, null);
        oldpassEdit = view.findViewById(R.id.dialog_repass_old_pass);
        newpassEdit = view.findViewById(R.id.dialog_repass_new_pass);
        repassEdit = view.findViewById(R.id.dialog_repass_new_repass);

        this.dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setTitle("修改用户密码")
                .setPositiveButton("提交", null)
                .setNegativeButton("取消", null)
                .create();
    }

    public void show() {
        if (dialog.isShowing())
            return;

        oldpassEdit.setText("");
        newpassEdit.setText("");
        repassEdit.setText("");
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String username = sharedPreferences.getString(MyApplication.PREFERENCES_USERNAME, "");
            String oldpass = oldpassEdit.getText().toString();
            String newpass = newpassEdit.getText().toString();
            String repass = repassEdit.getText().toString();
            if (oldpass.isEmpty()) {
                Toast.makeText(activity, "旧密码不能为空，请输入旧密码", Toast.LENGTH_SHORT).show();
            } else if (newpass.isEmpty()) {
                Toast.makeText(activity, "密码不能为空，请输入密码", Toast.LENGTH_SHORT).show();
            } else if (repass.isEmpty()) {
                Toast.makeText(activity, "确认密码不能为空", Toast.LENGTH_SHORT).show();
            } else if (!newpass.matches(StringMap.numberAndString)) {
                Toast.makeText(activity, "密码为6-14个字符(字母/数字)", Toast.LENGTH_SHORT).show();
            } else if (newpass.matches(StringMap.onlyNumberAndlow9)) {
                Toast.makeText(activity, "密码不能为9位以下的纯数字", Toast.LENGTH_SHORT).show();
            } else if (!newpass.equals(repass)) {
                Toast.makeText(activity, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            } else {
                byte[] user, oldp, newp;
                try {
                    user = username.getBytes(Utils.CODE_MAP_2132);
                    oldp = oldpass.getBytes(Utils.CODE_MAP_2132);
                    newp = newpass.getBytes(Utils.CODE_MAP_2132);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                loadingDialog.show();
                new Thread(() -> {
                    boolean b = communicator.sendData(jni.buildReviseUserInfoRequest(user, oldp, user, newp), (statusCode, errorCode) -> {
                        Log.d(TAG, "statusCode: " + Utils.byte2HEX((byte) statusCode));
                        Log.d(TAG, "errorCode: " + Utils.byte2HEX((byte) errorCode));
                        activity.runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            if (statusCode == BleCommunicator.STATE_OK) {
                                sharedPreferences.edit()
                                        .putString(MyApplication.PREFERENCES_PASSWORD, newpass)
                                        .apply();
                                Toast.makeText(activity, "修改密码成功", Toast.LENGTH_SHORT).show();
                                userPassChangeListener.userPassChange();
                                dialog.dismiss();
                            }
                            String msg = BleCommunicator.getMessageByStatusCode(statusCode);
                            if (statusCode == BleCommunicator.STATE_ERR)
                                msg += BleCommunicator.getMessageByErrorCode(errorCode);
                            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                        });
                    }, 3000);
                    if (!b)
                        activity.runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            Toast.makeText(activity, "蓝牙设备忙，请重试", Toast.LENGTH_SHORT).show();
                        });
                }).start();
            }
        });

    }

    public void dismiss() {
        if (dialog.isShowing())
            dialog.dismiss();
    }

    public interface OnUserPassChangeListener {
        void userPassChange();
    }
}
