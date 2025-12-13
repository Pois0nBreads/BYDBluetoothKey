package net.pxst.mybluetoothkey;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.byd.jnitest.JNI;

import java.util.Timer;
import java.util.TimerTask;

public class DriverDialog implements View.OnTouchListener {

    private final static int DRIVER_CODE_UP = 1;
    private final static int DRIVER_CODE_LEFT = 2;
    private final static int DRIVER_CODE_RIGHT = 3;
    private final static int DRIVER_CODE_DOWN = 4;
    private final static int DRIVER_CODE_NONE = 0;

    private final AlertDialog dialog;
    private final BleCommunicator commThread;
    private final JNI jni;
    private final Activity activity;

    private int driverCode = DRIVER_CODE_NONE;
    private Timer driverTimer;

    private final Button btn_up;
    private final Button btn_left;
    private final Button btn_right;
    private final Button btn_down;
    private final TextView driver_display;

    @SuppressLint("ClickableViewAccessibility")
    DriverDialog(Activity activity, BleCommunicator commThread, JNI jni) {
        this.commThread = commThread;
        this.jni = jni;
        this.activity = activity;
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_driver, null);
        btn_up = view.findViewById(R.id.btn_driver_up);
        btn_left = view.findViewById(R.id.btn_driver_left);
        btn_right = view.findViewById(R.id.btn_driver_right);
        btn_down = view.findViewById(R.id.btn_driver_down);
        driver_display = view.findViewById(R.id.btn_text_test);
        btn_up.setOnTouchListener(this);
        btn_left.setOnTouchListener(this);
        btn_right.setOnTouchListener(this);
        btn_down.setOnTouchListener(this);

        dialog = new AlertDialog.Builder(activity)
                .setOnDismissListener(dialog -> driverBtnUp())
                .setCancelable(false)
                .setTitle("遥控驾驶")
                .setNegativeButton("退出遥控驾驶", (dialog, which) -> dialog.dismiss())
                .setView(view)
                .create();
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }

    @SuppressLint({"ClickableViewAccessibility", "NonConstantResourceId"})
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean flag = v.performClick();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (this.commThread == null || !this.commThread.isRun()) {
                    Toast.makeText(activity, "蓝牙未连接", Toast.LENGTH_SHORT).show();
                    return flag;
                }
                Log.i("onTrouchEvent", "ACTION_DOWN");
                switch (v.getId()) {
                    case R.id.btn_driver_up:
                        btn_left.setEnabled(false);
                        btn_right.setEnabled(false);
                        btn_down.setEnabled(false);
                        driverCode = DRIVER_CODE_UP;
                        break;
                    case R.id.btn_driver_left:
                        btn_up.setEnabled(false);
                        btn_right.setEnabled(false);
                        btn_down.setEnabled(false);
                        driverCode = DRIVER_CODE_LEFT;
                        break;
                    case R.id.btn_driver_right:
                        btn_up.setEnabled(false);
                        btn_left.setEnabled(false);
                        btn_down.setEnabled(false);
                        driverCode = DRIVER_CODE_RIGHT;
                        break;
                    case R.id.btn_driver_down:
                        btn_up.setEnabled(false);
                        btn_left.setEnabled(false);
                        btn_right.setEnabled(false);
                        driverCode = DRIVER_CODE_DOWN;
                        break;
                    default:
                        driverCode = DRIVER_CODE_NONE;
                        break;
                }
                driverBtnDown();
                break;
            case MotionEvent.ACTION_UP:
                driverBtnUp();
                Log.i("onTrouchEvent", "ACTION_UP");
                break;
            case MotionEvent.ACTION_CANCEL:
                driverBtnUp();
                Log.i("onTrouchEvent", "ACTION_CANCEL");
                return true;
        }
        return flag;
    }

    private void setDriverDisplay(int pos) {
        String disText;
        switch (driverCode) {
            case DRIVER_CODE_UP:
                disText = "正在前进 - " + pos;
                break;
            case DRIVER_CODE_LEFT:
                disText = "正在左转 - " + pos;
                break;
            case DRIVER_CODE_RIGHT:
                disText = "正在右转 - " + pos;
                break;
            case DRIVER_CODE_DOWN:
                disText = "正在后退 - " + pos;
                break;
            default:
                disText = "待机中";
                break;
        }
        driver_display.setText(disText);
        Log.i("RemoteDriving", disText);
    }

    private void driverBtnDown() {
        driverTimer = new Timer();
        driverTimer.schedule(new TimerTask() {
            int pos = 0;

            @Override
            public void run() {
                pos++;
                byte[] CMD;
                switch (driverCode) {
                    case DRIVER_CODE_UP:
                        CMD = jni.forwardRequest();
                        break;
                    case DRIVER_CODE_LEFT:
                        CMD = jni.turnLeftRequest();
                        break;
                    case DRIVER_CODE_RIGHT:
                        CMD = jni.turnRightRequest();
                        break;
                    case DRIVER_CODE_DOWN:
                        CMD = jni.drawBackdRequest();
                        break;
                    default:
                        return;
                }
                commThread.sendData(CMD, (resultCode, errorCode) -> {
                }, 0);
                activity.runOnUiThread(() -> setDriverDisplay(pos));
            }
        }, 0, 100);
    }

    private void driverBtnUp() {
        if (driverTimer == null)
            return;
        driverTimer.cancel();
        driverTimer = null;
        driverCode = DRIVER_CODE_NONE;
        this.btn_up.setEnabled(true);
        this.btn_left.setEnabled(true);
        this.btn_right.setEnabled(true);
        this.btn_down.setEnabled(true);
        setDriverDisplay(0);
    }
}
