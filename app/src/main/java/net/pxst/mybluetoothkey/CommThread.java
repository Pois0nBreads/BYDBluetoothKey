package net.pxst.mybluetoothkey;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.byd.jnitest.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class CommThread extends Thread{

    private final static String TAG = "CommThread";

    public final static int RESULT_START_FLAG = 0x5A;
    public final static int STATE_TIMEOUT = 0xF0;
    public final static int STATE_THREAD_END = 0xF1;
    public final static int STATE_RETRY = 0x00;
    public final static int STATE_OK = 0x01;
    public final static int STATE_ERR = 0x02;

    public final static int ERR_RETRY = 0x00;
    public final static int ERR_LOGIN_FAILED = 0x01;
    public final static int ERR_NOT_OFF = 0x02;
    public final static int ERR_NO_USER = 0x03;
    public final static int ERR_I_KEY_SAVE_FAILED = 0x04;
    public final static int ERR_CAN_AUTH_BUSY = 0x05;
    public final static int ERR_I_KEY_BUSY = 0x06;

    private final BluetoothSocket bluetoothSocket;
    private boolean isRun = true;
    private InputStream inputStream;
    private OutputStream outputStream;
    private CommCallback commCallback;
    private Timer timer;

    @SuppressLint("MissingPermission")
    public CommThread(BluetoothSocket bluetoothSocket) throws IOException {
        this.bluetoothSocket = bluetoothSocket;
        bluetoothSocket.connect();
        inputStream = bluetoothSocket.getInputStream();
        outputStream = bluetoothSocket.getOutputStream();
    }

    /**
     * 发送数据函数
     * @param data 要发送的数据
     * @param callback 发送回调响应
     * @param timeout 超时
     * @return 是否发送数据
     */
    synchronized public boolean sendData(byte[] data, CommCallback callback, int timeout) {
        if (!bluetoothSocket.isConnected() || !isRun)
            return false;
        if (callback == null)
            return false;
        if (commCallback != null)
            return false;
        commCallback = callback;
        //创建超时任务
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                commCallback.onData(STATE_TIMEOUT, STATE_TIMEOUT);
                commCallback = null;
            }
        }, timeout);
        //发送数据
        try {
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            timer.cancel();
            commCallback = null;
            return false;
        }
        return true;
    }

    /**
     * 数据接收处理函数
     */
    @Override
    public void run() {
        byte[] data = new byte[18];
        try {
            while (bluetoothSocket.isConnected() && isRun) {
                if (inputStream.read() == RESULT_START_FLAG) {
                    for (int i = 0; i < 18; i++) {
                        data[i] = (byte) inputStream.read();
                    }
                    Log.d(TAG, "RecvData: " + Utils.bytes2HEX(data));
                    if ((data[16] & 0xFF) == 0xF5 && (data[17] & 0xFF) == 0xFA) {
                        if (((data[2] & 0xFF) | ((data[1] & 0xFF) << 8)) == 665) {
                            int resultCode = (data[7] & 0xFF);
                            int errorCode = (data[8] & 0xFF);
                            if (commCallback != null) {
                                timer.cancel(); //取消超时任务
                                commCallback.onData(resultCode & 3, errorCode & 15);
                                commCallback = null;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (isRun)
                endRun();
        }
    }

    /**
     * 结束运行
     */
    public void endRun() {
        if (!isRun)
            return;
        isRun = false;
        if (commCallback != null) {
            timer.cancel();
            commCallback.onData(STATE_THREAD_END, STATE_THREAD_END);
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 是否正在运行
     * @return isRun status
     */
    public boolean isRun() {
        return isRun;
    }

    public static String getMessageByStatusCode(int code) {
        switch (code) {
            case STATE_TIMEOUT:
                return "响应超时";
            case STATE_THREAD_END:
                return "蓝牙断开";
            case STATE_RETRY:
                return "请重试";
            case STATE_OK:
                return "发送成功";
            case STATE_ERR:
                return "出错: ";
            default:
                return "未知响应";
        }
    }

    public static String getMessageByErrorCode(int code) {
        switch (code) {
            case ERR_RETRY:
                return "请重试";
            case ERR_LOGIN_FAILED:
                return "您输入的帐号或密码错误";
            case ERR_NOT_OFF:
                return "请将您的电源档位换至OFF档";
            case ERR_NO_USER:
                return "您输入的帐号不存在";
            case ERR_I_KEY_SAVE_FAILED:
                return "IK注册保存失败 ";
            case ERR_CAN_AUTH_BUSY:
                return "信息站认证反馈超时 ";
            case ERR_I_KEY_BUSY:
                return "I-KEY系统忙 ";
            default:
                return "未知错误";
        }
    }

    /**
     * 回调接口
     */
    public static interface CommCallback {
        void onData(int resultCode, int errorCode);
    }
}
