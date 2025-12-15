package net.pxst.mybluetoothkey;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.byd.jnitest.JNI;
import com.byd.jnitest.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class BleCommunicator {

    private final static String TAG = "BleCommunicator";

    public final static int RESULT_START_FLAG = 0x5A; //字节起始

    public final static int STATE_TIMEOUT = 0xF0; //超时状态
    public final static int STATE_THREAD_END = 0xF1; //蓝牙线程结束状态
    public final static int STATE_DETECT_KEY = 0xF2; //检测钥匙状态
    public final static int STATE_RETRY = 0x00; //请重试
    public final static int STATE_OK = 0x01; //成功状态
    public final static int STATE_ERR = 0x02; //错误状态

    public final static int KEY_CODE_NEW_KEY = 0x01; //新钥匙
    public final static int KEY_CODE_VALID_KEY = 0x02; //有效钥匙
    public final static int KEY_CODE_INVALID_KEY = 0x03; //无效钥匙
    public final static int KEY_CODE_NEW_KEY_IN_DETECT = 0x05; //新匹配钥匙，未离开读卡器区域
    public final static int KEY_CODE_NO_DETECT_KEY = 0x06; //钥匙未靠近读卡器区域
    public final static int KEY_CODE_UNIT_KEY = 0x07; //初始钥匙

    public final static int ERR_RETRY = 0x00; //错误码请重试
    public final static int ERR_LOGIN_FAILED = 0x01; //错误码登陆失败
    public final static int ERR_NOT_OFF = 0x02; //错误码未熄火
    public final static int ERR_NO_USER = 0x03; //错误码没有该用户
    public final static int ERR_I_KEY_SAVE_FAILED = 0x04; //错误码IKey 保存失败
    public final static int ERR_CAN_AUTH_BUSY = 0x05; //错误码 CAN总线忙
    public final static int ERR_I_KEY_BUSY = 0x06; //错误码 iKey系统忙

    private final Object lock = new Object();
    private BluetoothSocket mBluetoothSocket;
    private boolean isRun = false;
    private CommCallback mCommCallback;
    private final OnConnectListener onConnectListener;
    private final OnDisConnectListener onDisConnectListener;
    private Timer commTimer = new Timer();
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private Timer registerTimer = new Timer();
    private boolean isDestroyed = false;
    private boolean inRegisterMode = false;
    private RegisterEventListener registerEventListener;

    private final JNI jni = new JNI();

    public BleCommunicator(OnConnectListener onConnectListener, OnDisConnectListener onDisConnectListener) {
        this.onConnectListener = onConnectListener;
        this.onDisConnectListener = onDisConnectListener;
        new Thread() {
            @Override
            public void run() {
                byte[] data = new byte[18];
                while (!isDestroyed) {
                    while (isRun) {
                        try {
                            if (mInputStream.read() == RESULT_START_FLAG) {
                                for (int i = 0; i < 18; i++) {
                                    data[i] = (byte) mInputStream.read();
                                }
                                if ((data[16] & 0xFF) == 0xF5 && (data[17] & 0xFF) == 0xFA) {
                                    int dataFlag = (data[2] & 0xFF) | ((data[1] & 0xFF) << 8);
                                    if (dataFlag == 665) {
                                        int resultCode = (data[7] & 0xFF);
                                        int errorCode = (data[8] & 0xFF);
                                        Log.d(TAG, "ResultCode: " + resultCode + " ErrorCode: " + errorCode);
                                        if (mCommCallback != null) {
                                            commTimer.cancel(); //取消超时任务
                                            mCommCallback.onData(resultCode & 3, errorCode & 15);
                                            mCommCallback = null;
                                        }
                                    }
                                    if (dataFlag == 384) {
                                        byte code = (byte) (data[5] & 0xFF);
                                        code = (byte) ((code >> 3) & 7);
                                        if (code >= 1 && registerEventListener != null) {
                                            Log.d(TAG, "RegisterEventCode " + code);
                                            registerEventListener.onEvent(code);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "RevThread Error!", e);
                            onStop();
                            break;
                        }
                    }
                }
            }
        }.start();
    }

    private boolean isInRegisterMode() {
        return inRegisterMode;
    }

    synchronized public boolean enterRegisterMode(RegisterEventListener eventListener) {
        if (!isRun)
            return false;
        if (inRegisterMode)
            return false;
        this.registerEventListener = eventListener;
        //取消普通发送的事件 和 计时器
        commTimer.cancel();
        if (mCommCallback != null) {
            mCommCallback.onData(STATE_TIMEOUT, STATE_TIMEOUT);
            mCommCallback = null;
        }
        inRegisterMode = true;
        registerTimer = new Timer();
        registerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    mOutputStream.write(jni.buildRegisterRequest());
                    mOutputStream.flush();
                } catch (IOException e) {
                    Log.e(TAG, "sendRegisterRequest Failed", e);
                }
            }
        }, 0, 3000);
        return true;
    }

    synchronized public void leaveRegisterMode() {
        if (!isRun)
            return;
        if (!inRegisterMode)
            return;
        this.registerEventListener = null;
        inRegisterMode = false;
        registerTimer.cancel();
        commTimer.cancel();
        mCommCallback = null;
        try {
            mOutputStream.write(jni.buildExitRegister());
            mOutputStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "leaveRegisterMode Failed", e);
        }
    }

    public boolean sendRegisterInfo(String user, String pass, CommCallback callback) {
        synchronized (this) {
            if (!isRun)
                return false;
            if (!inRegisterMode)
                return false;
        }
        registerTimer.cancel();
        commTimer = new Timer();
        commTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mCommCallback != null)
                    mCommCallback.onData(STATE_TIMEOUT, STATE_TIMEOUT);
                mCommCallback = null;
            }
        }, 3000);
        this.mCommCallback = (resultCode, exCode) -> {
            leaveRegisterMode();
            callback.onData(resultCode, exCode);
        };
        try {
            final byte[] cmd = jni.buildRegisterInfo(user.getBytes(Utils.CODE_MAP_2132), pass.getBytes(Utils.CODE_MAP_2132));
            mOutputStream.write(cmd);
            mOutputStream.flush();
        } catch (Exception e) {
            Log.e(TAG, "Send buildRegisterInfo Error", e);
            return false;
        }
        return true;
    }

    /**
     * 发送数据函数
     *
     * @param data     要发送的数据
     * @param callback 发送回调响应
     * @param timeout  超时
     * @return 是否发送数据
     */
    synchronized public boolean sendData(byte[] data, CommCallback callback, int timeout) {
        if (inRegisterMode || !isRun)
            return false;
        if (callback == null)
            return false;
        if (mCommCallback != null)
            return false;
        mCommCallback = callback;
        //创建超时任务
        commTimer = new Timer();
        commTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mCommCallback.onData(STATE_TIMEOUT, STATE_TIMEOUT);
                mCommCallback = null;
            }
        }, timeout);
        //发送数据
        try {
            mOutputStream.write(data);
            mOutputStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "SandData Error:", e);
            commTimer.cancel();
            mCommCallback = null;
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    synchronized public void start(BluetoothSocket bluetoothSocket) throws IOException {
        if (isDestroyed)
            return;
        if (isRun)
            return;
        this.mBluetoothSocket = bluetoothSocket;
        this.mBluetoothSocket.connect();
        this.mOutputStream = mBluetoothSocket.getOutputStream();
        this.mInputStream = mBluetoothSocket.getInputStream();
        inRegisterMode = false;
        isRun = true;
        Log.i(TAG, "start!");
        if (onConnectListener != null)
            onConnectListener.onConnect();
    }

    /**
     * 结束运行
     */
    synchronized public void stop() {
        if (isDestroyed)
            return;
        if (!isRun)
            return;
        this.onStop();
        Log.i(TAG, "stop!");
    }

    private void onStop() {
        synchronized (lock) {
            if (!isRun)
                return;
            isRun = false;
            Log.i(TAG, "onStop!");
            registerTimer.cancel();
            this.registerEventListener = null;
            if (mCommCallback != null) {
                commTimer.cancel();
                mCommCallback.onData(STATE_THREAD_END, STATE_THREAD_END);
                mCommCallback = null;
            }
            try {
                mOutputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "onStop mOutputStream.close() Error", e);
            }
            try {
                mInputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "onStop mInputStream.close() Error", e);
            }
            try {
                mBluetoothSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "onStop mBluetoothSocket.close() Error", e);
            }
        }
        onDisConnectListener.onDisConnect();
    }

    synchronized public void destroy() {
        if (isDestroyed)
            return;
        isDestroyed = true;
        this.onStop();
        Log.i(TAG, "destroy!");
    }

    /**
     * 是否正在运行
     *
     * @return isRun status
     */
    synchronized public boolean isRun() {
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
    public interface CommCallback {
        void onData(int resultCode, int errCode);
    }

    public interface RegisterEventListener {
        void onEvent(int statusCode);
    }

    public interface OnConnectListener {
        void onConnect();
    }

    public interface OnDisConnectListener {
        void onDisConnect();
    }
}
