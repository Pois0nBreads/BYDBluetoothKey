package com.byd.jnitest;

public class Utils {

    public final static String CODE_MAP_2132 = "GB2312-80";
    public final static String CODE_MAP_UTF_8 = "UTF-8";

    public static String bytes2HEX(byte[] bArr) {
        StringBuffer stringBuffer = new StringBuffer(16);
        for (byte b : bArr) {
            String hexString = Integer.toHexString(b & 255);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            stringBuffer.append(hexString.toUpperCase());
        }
        return stringBuffer.toString();
    }

    public static String byte2HEX(byte b) {
        String hexString = Integer.toHexString(b & 255);
        if (hexString.length() == 1) {
            hexString = '0' + hexString;
        }
        return hexString.toUpperCase();
    }

    public static byte[] hex2bytes(String str) {
        if (str.length() < 1) {
            return null;
        }
        byte[] bArr = new byte[(str.length() / 2)];
        for (int i = 0; i < str.length() / 2; i++) {
            bArr[i] = (byte) ((Integer.parseInt(str.substring(i * 2, (i * 2) + 1), 16) * 16) + Integer.parseInt(str.substring((i * 2) + 1, (i * 2) + 2), 16));
        }
        return bArr;
    }
}
