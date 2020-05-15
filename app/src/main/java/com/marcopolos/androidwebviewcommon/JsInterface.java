package com.marcopolos.androidwebviewcommon;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

public class JsInterface {
    private static final String TAG = "JsInterface";

    public JsInterface(Context context) {

    }

    /**
     * 这个方法由 JS 调用， 不在主线程执行
     *
     * @param value
     */
    @JavascriptInterface
    public void postMessage(String value) {
        Log.i(TAG, "value = " + value);
    }
}
