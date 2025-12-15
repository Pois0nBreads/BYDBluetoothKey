package net.pxst.mybluetoothkey;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class UserPassDialog {

    private final AlertDialog dialog;
    private final SharedPreferences sharedPreferences;
    private final OnUserPassChangeListener userPassChangeListener;
    private final OnUserPassTestListener onUserPassTestListener;
    private final Activity activity;

    private final EditText userEdit;
    private final EditText passEdit;

    UserPassDialog(Activity activity, OnUserPassChangeListener userPassChangeListener, OnUserPassTestListener onUserPassTestListener) {
        this.sharedPreferences = activity.getSharedPreferences(MyApplication.PREFERENCES_SETTINGS, Context.MODE_PRIVATE);
        this.activity = activity;
        this.userPassChangeListener = userPassChangeListener;
        this.onUserPassTestListener = onUserPassTestListener;
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_userpass, null);
        userEdit = view.findViewById(R.id.dialog_userpass_user);
        passEdit = view.findViewById(R.id.dialog_userpass_pass);

        this.dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setTitle("修改登录用户名密码")
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .setNeutralButton("测试用户名密码", null)
                .create();
    }

    public void show() {
        if (dialog.isShowing())
            return;

        userEdit.setText(sharedPreferences.getString(MyApplication.PREFERENCES_USERNAME, ""));
        passEdit.setText(sharedPreferences.getString(MyApplication.PREFERENCES_PASSWORD, ""));
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String user = userEdit.getText().toString();
            String pass = passEdit.getText().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(MyApplication.PREFERENCES_USERNAME, user)
                    .putString(MyApplication.PREFERENCES_PASSWORD, pass)
                    .apply();
            userPassChangeListener.userPassChange();
            Toast.makeText(activity, "设置成功", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            String user = userEdit.getText().toString();
            String pass = passEdit.getText().toString();
            onUserPassTestListener.userPassTest(user, pass);
        });
    }

    public void dismiss() {
        if (dialog.isShowing())
            dialog.dismiss();
    }

    public interface OnUserPassChangeListener {
        void userPassChange();
    }

    public interface OnUserPassTestListener {
        void userPassTest(String user, String pass);
    }
}
