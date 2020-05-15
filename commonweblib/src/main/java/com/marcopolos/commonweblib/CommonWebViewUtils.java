package com.marcopolos.commonweblib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class CommonWebViewUtils {

    public static boolean checkNetwork(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        }
        @SuppressLint("MissingPermission") NetworkInfo info = connectivity.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

}
