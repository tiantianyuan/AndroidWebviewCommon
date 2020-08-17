package com.marcopolos.androidwebviewcommon;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ValueCallback;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.marcopolos.commonweblib.CommonWebView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.marcopolos.commonweblib.CommonWebViewConfig.TRADITIONAL;


public class MainActivity extends AppCompatActivity {

    private CommonWebView commonWebView;
    private Map<String, Callable> mUrlSchemeFucMap = new HashMap<>();

    @Override

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CommonWebView commonWebView = findViewById(R.id.common_webview);
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
        commonWebView.loadUrl("http://192.168.2.234:8080");
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
}
