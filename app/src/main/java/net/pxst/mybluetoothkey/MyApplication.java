package net.pxst.mybluetoothkey;

import android.app.Activity;
import android.app.Application;

public class MyApplication extends Application {

    private Activity mActivity = null;

    static {
        System.loadLibrary("jni_test");
    }

    synchronized public void setActivity(Activity activity) {
        if (mActivity != null)
            if (!mActivity.isFinishing())
                mActivity.finish();
        mActivity = activity;
    }

}
