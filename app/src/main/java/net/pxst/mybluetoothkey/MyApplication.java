package net.pxst.mybluetoothkey;

import android.app.Activity;
import android.app.Application;

public class MyApplication extends Application {

    protected static final String PREFERENCES_SETTINGS = "settings";
    protected static final String PREFERENCES_USERNAME = "username";
    protected static final String PREFERENCES_PASSWORD = "password";
    protected static final String PREFERENCES_MAC_ADDRESS = "address";
    protected static final String PREFERENCES_DEV_NAME = "dev_name";
    protected static final String PREFERENCES_FIRST_USE = "first_use";
    protected static final String INTENT_FROM_WHAT = "from_what";
    protected static final int INTENT_FROM_NONE = 0;
    protected static final int INTENT_FROM_MAIN = 1;
    private Activity mActivity = null;

    static {
        System.loadLibrary("jni_test");
    }

    synchronized public void setActivity(Activity activity) {
        if (mActivity != null)
            if (!mActivity.isDestroyed())
                mActivity.finish();
        mActivity = activity;
    }

}
