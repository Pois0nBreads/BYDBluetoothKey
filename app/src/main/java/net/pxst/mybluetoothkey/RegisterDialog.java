package net.pxst.mybluetoothkey;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.byd.jnitest.JNI;
import com.byd.jnitest.StringMap;
import com.byd.jnitest.Utils;

public class RegisterDialog {

    private static final String TAG = "RegisterDialog";

    private final AlertDialog dialog, loadingDialog;
    private final BleCommunicator commThread;
    private final OnUserPassChangeListener userPassChangeListener;
    private final Activity activity;

    private TextView loadTT;
    private EditText userEt, passEt, passEt2;
    private LinearLayout loadLay, regLay;

    RegisterDialog(Activity activity, BleCommunicator commThread, OnUserPassChangeListener userPassChangeListener) {
        this.commThread = commThread;
        this.userPassChangeListener = userPassChangeListener;
        this.activity = activity;
        this.loadingDialog = new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle("正在注册")
                .setView(new ProgressBar(activity))
                .create();

        View view = activity.getLayoutInflater().inflate(R.layout.dialog_register, null);
        loadTT = view.findViewById(R.id.dialog_register_load_tt);
        loadLay = view.findViewById(R.id.dialog_register_load_lay);
        regLay = view.findViewById(R.id.dialog_register_reg_lay);
        userEt = view.findViewById(R.id.dialog_register_user);
        passEt = view.findViewById(R.id.dialog_register_pass);
        passEt2 = view.findViewById(R.id.dialog_register_pass2);
        view.findViewById(R.id.dialog_register_btn).setOnClickListener(this::onclick);
        dialog = new AlertDialog.Builder(activity)
                .setOnDismissListener(this::onDestroy)
                .setCancelable(false)
                .setTitle("注册钥匙")
                .setNeutralButton("取消注册", (dialog, which) -> dialog.dismiss())
                .setView(view)
                .create();
    }

    private void onclick(View v) {
        String username = userEt.getText().toString();
        String password = passEt.getText().toString();
        String password2 = passEt2.getText().toString();
        if (username.isEmpty()) {
            Toast.makeText(activity, "帐号不能为空，请输入帐号", Toast.LENGTH_SHORT).show();
        } else if (password.equals("")) {
            Toast.makeText(activity, "密码不能为空，请输入密码", Toast.LENGTH_SHORT).show();
        } else if (password2.equals("")) {
            Toast.makeText(activity, "确认密码不能为空", Toast.LENGTH_SHORT).show();
        } else if (!username.matches(StringMap.USERNAME_MATCH)) {
            Toast.makeText(activity, "密码为6-10个字符(字母/数字)", Toast.LENGTH_SHORT).show();
        } else if (username.matches(StringMap.onlyNumber)) {
            Toast.makeText(activity, "帐号不能为纯数字", Toast.LENGTH_SHORT).show();
        } else if (!password.matches(StringMap.numberAndString)) {
            Toast.makeText(activity, "密码为6-14个字符(字母/数字)", Toast.LENGTH_SHORT).show();
        } else if (password.matches(StringMap.onlyNumberAndlow9)) {
            Toast.makeText(activity, "密码不能为9位以下的纯数字", Toast.LENGTH_SHORT).show();
        } else if (!password.equals(password2)) {
            Toast.makeText(activity, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
        } else {
            loadingDialog.show();
            new Thread(() -> {
                boolean b = commThread.sendRegisterInfo(username, password, (statusCode, errorCode) -> {
                    Log.d(TAG, "statusCode: " + Utils.byte2HEX((byte) statusCode));
                    Log.d(TAG, "errorCode: " + Utils.byte2HEX((byte) errorCode));
                    activity.runOnUiThread(() -> {
                        dismiss();
                        if (statusCode == BleCommunicator.STATE_OK)
                            userPassChangeListener.userPassChange(username, password);
                        String msg = BleCommunicator.getMessageByStatusCode(statusCode);
                        if (statusCode == BleCommunicator.STATE_ERR)
                            msg += BleCommunicator.getMessageByErrorCode(errorCode);
                        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                    });
                });
                if (!b)
                    activity.runOnUiThread(() -> {
                        dismiss();
                        Toast.makeText(activity, "蓝牙设备忙，请重试", Toast.LENGTH_SHORT).show();
                    });
            }).start();
        }
    }

    private void onDestroy(DialogInterface dialog) {
        loadingDialog.dismiss();
        dismiss();
        if (!commThread.isRun())
            return;
        //退出注册模式
        new Thread(commThread::leaveRegisterMode).start();
    }

    public void dismiss() {
        if (dialog.isShowing())
            dialog.dismiss();
    }

    public void show() {
        if (dialog.isShowing())
            return;
        loadTT.setText("Loading...");
        userEt.setText("");
        passEt.setText("");
        passEt2.setText("");
        regLay.setVisibility(View.INVISIBLE);
        loadLay.setVisibility(View.VISIBLE);
        //进入注册模式
        boolean result = commThread.enterRegisterMode(new BleCommunicator.RegisterEventListener() {
            boolean ok = false;
            String msg;
            @Override
            public void onEvent(int statusCode) {
                if (ok)
                    return;
                switch (statusCode) {
                    case BleCommunicator.KEY_CODE_VALID_KEY:
                        ok = true;
                        msg = "检测到有效钥匙，允许注册";
                        activity.runOnUiThread(() -> {
                            loadLay.setVisibility(View.INVISIBLE);
                            regLay.setVisibility(View.VISIBLE);
                            Toast.makeText(activity, "检测到有效钥匙，允许注册", Toast.LENGTH_SHORT).show();
                        });
                        break;
                    case BleCommunicator.KEY_CODE_NEW_KEY:
                        msg = "新钥匙";
                        break;
                    case BleCommunicator.KEY_CODE_INVALID_KEY:
                        msg = "无效钥匙";
                        break;
                    case BleCommunicator.KEY_CODE_NEW_KEY_IN_DETECT:
                        msg = "新匹配钥匙，未离开读卡器区域";
                        break;
                    case BleCommunicator.KEY_CODE_NO_DETECT_KEY:
                        msg = "钥匙未靠近读卡器区域";
                        break;
                    case BleCommunicator.KEY_CODE_UNIT_KEY:
                        msg = "初始钥匙";
                        break;
                }
                activity.runOnUiThread(() -> loadTT.setText(msg));
            }
        });
        if (!result) {
            onDestroy(null);
            return;
        }
        dialog.show();
    }
    public interface OnUserPassChangeListener {
        void userPassChange(String user, String pass);
    }
}
