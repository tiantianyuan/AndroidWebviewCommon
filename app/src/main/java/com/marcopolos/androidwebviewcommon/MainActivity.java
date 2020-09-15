package com.marcopolos.androidwebviewcommon;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ValueCallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.marcopolos.commonweblib.CommonWebView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.marcopolos.commonweblib.CommonWebView.CAMERA_FILE_REQUEST_CODE;
import static com.marcopolos.commonweblib.CommonWebView.GALLERY_FILE_REQUEST_CODE;
import static com.marcopolos.commonweblib.CommonWebView.REQUEST_PERMISSION_FOR_CAMERA;
import static com.marcopolos.commonweblib.CommonWebView.REQUEST_PERMISSION_FOR_GALLERY;


public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private CommonWebView commonWebView;
    private Map<String, Callable> mUrlSchemeFucMap = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        commonWebView = findViewById(R.id.common_webview);
        //webview的初始化
        commonWebView.initCommonWebView();
        //设置webview的显示的样式
        // commonWebView.setStyle(FULL_SCREEN);
        //设置图标的颜色
        commonWebView.setTintColor(Color.BLUE);
        //设置title的颜色
        commonWebView.setTitleColor(Color.BLUE);
        //设置title标题
        commonWebView.setTitleText("CommonWebView");
        //设置加载的网页地址
        commonWebView.loadUrl("http://192.168.2.234:8080/Register/SelfConfirm");
        //webView拦截到URLScheme执行指定的方法
        mUrlSchemeFucMap.clear();
        mUrlSchemeFucMap.put("otb", new Callable() {
            @Override
            public Object call() throws Exception {
                finish();
                return null;
            }
        });
        commonWebView.setUrlSchemeFucMap(mUrlSchemeFucMap);
        //userAgent的设定
        commonWebView.setUserAgent("Android");
        //cookie的设定
        ArrayList<String> cookieList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            cookieList.add("APP_ID=2");
        }
        //commonWebView.synCookies(MainActivity.this, "https://www.baidu.com", cookieList);

        //JS 调用 Android 方法
        commonWebView.addJavascriptInterface(new JsInterface(this), "launcher");

        //Android4.4以上 调用  JS方法
        commonWebView.evaluateJavascript("window.showAlert(\"弹窗\");", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.e("TAG", "--------->>" + value);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if ((requestCode != CAMERA_FILE_REQUEST_CODE && requestCode != GALLERY_FILE_REQUEST_CODE) ||
                commonWebView.getFilePathCallback() == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_FILE_REQUEST_CODE) {
                // If there is not data, then we may have taken a photo
                if (commonWebView.getCameraPhotoPath() != null) {
                    results = new Uri[]{Uri.parse("file://" + commonWebView.getCameraPhotoPath())};
                }
            } else if (requestCode == GALLERY_FILE_REQUEST_CODE) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        commonWebView.getFilePathCallback().onReceiveValue(results);
        commonWebView.setFilePathCallbackNull();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_FOR_CAMERA
                && Manifest.permission.CAMERA.equals(permissions[0])
                && Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[1])
                && Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[2])
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED
        ) {
            commonWebView.dispatchTakePictureIntent();
        } else if (requestCode == REQUEST_PERMISSION_FOR_GALLERY
                && Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[0])
                && Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[1])
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
        ) {
            commonWebView.pickImage();
        } else {
            finish();
        }
    }
}
