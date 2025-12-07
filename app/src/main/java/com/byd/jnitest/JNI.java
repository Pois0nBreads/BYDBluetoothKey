package com.byd.jnitest;

import java.io.UnsupportedEncodingException;

public class JNI {

    private byte[] user, pass;

    public JNI(byte[] user, byte[] pass) {
        this.user = user;
        this.pass = pass;
    }

    public JNI(String user, String pass) throws UnsupportedEncodingException {
        this.user = user.getBytes(Utils.CODE_MAP_2132);
        this.pass = pass.getBytes(Utils.CODE_MAP_2132);
    }

    //锁车指令
    public byte[] buildLockRequest() {
        return buildLockRequest(user, pass);
    }

    //解锁指令
    public byte[] buildUnLockRequest() {
        return buildUnLockRequest(user, pass);
    }

    //登录指令
    public byte[] buildLoginRequest() {
        return buildLoginRequest(user, pass);
    }

    //开后备箱指令
    public byte[] buildOpenTruckRequest() {
        return buildOpenTruckRequest(user, pass);
    }

    //开关电源指令
    public byte[] controlPower() {
        return controlPower(user, pass);
    }

    //后退指令
    public byte[] drawBackdRequest() {
        return drawBackdRequest(user, pass);
    }

    //前进指令
    public byte[] forwardRequest() {
        return forwardRequest(user, pass);
    }

    //左转指令
    public byte[] turnLeftRequest() {
        return turnLeftRequest(user, pass);
    }

    //右转指令
    public byte[] turnRightRequest() {
        return turnRightRequest(user, pass);
    }

    //空指令
    public native byte[] buildInvalidRequest();

    //进入注册指令
    public native byte[] buildRegisterRequest();

    //退出注册指令
    public native byte[] buildExitRegister();

    //注册用户指令
    public native byte[] buildRegisterInfo(byte[] user, byte[] pass);

    //锁车指令
    public native byte[] buildLockRequest(byte[] user, byte[] pass);

    //解锁指令
    public native byte[] buildUnLockRequest(byte[] user, byte[] pass);

    //登录指令
    public native byte[] buildLoginRequest(byte[] user, byte[] pass);

    //开后备箱指令
    public native byte[] buildOpenTruckRequest(byte[] user, byte[] pass);

    //开关电源指令
    public native byte[] controlPower(byte[] user, byte[] pass);

    //后退指令
    public native byte[] drawBackdRequest(byte[] user, byte[] pass);

    //前进指令
    public native byte[] forwardRequest(byte[] user, byte[] pass);

    //左转指令
    public native byte[] turnLeftRequest(byte[] user, byte[] pass);

    //右转指令
    public native byte[] turnRightRequest(byte[] user, byte[] pass);

    //修改用户密码指令
    public native byte[] buildReviseUserInfoRequest(byte[] oldUser, byte[] oldPass, byte[] newUser, byte[] newPass);

    //修改蓝牙名称指令
    public native byte[] changeBtNameRequest(byte[] name);

    //修改蓝牙pin指令
    public native byte[] changeBtPinRequest(byte[] pin);
}
