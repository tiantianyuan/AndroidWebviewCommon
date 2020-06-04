package com.marcopolos.commonweblib;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.marcopolos.commonweblib.CommonWebViewConfig.CONCISE;
import static com.marcopolos.commonweblib.CommonWebViewConfig.FULL_SCREEN;
import static com.marcopolos.commonweblib.CommonWebViewConfig.MODERN;
import static com.marcopolos.commonweblib.CommonWebViewConfig.TRADITIONAL;

public class CommonWebView extends ConstraintLayout implements View.OnClickListener {

    private ConstraintLayout mClHeader;
    private ImageView mIvTopLeft, mIvTopRight, mIvTransparentClose, mIvBack, mIvForward;
    private TextView mTvTitle;
    private LinearLayout mLlFooter, mLlBack, mLlForward;
    private ProgressBar mProgressBar;
    private WebView mCommonWebView;
    private WebSettings mWebSettings;
    private String mTitleText = "";
    private Activity mActivity;
    private Context mContext;
    private Map<String, Callable> mUrlSchemeFucMap = new HashMap<>();
    private boolean mHasEvaluateJs = false;
    private String mScript;
    private ValueCallback<String> mResultCallback;

    public CommonWebView(Context context) {
        super(context);
    }

    public CommonWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.common_webview, this);
        mClHeader = findViewById(R.id.cl_header);
        mIvTopLeft = findViewById(R.id.iv_top_left);
        mIvTopRight = findViewById(R.id.iv_top_right);
        mIvTransparentClose = findViewById(R.id.iv_transparent_close);
        mIvBack = findViewById(R.id.iv_back);
        mIvForward = findViewById(R.id.iv_forward);
        mLlForward = findViewById(R.id.ll_forward);
        mTvTitle = findViewById(R.id.tv_title);
        mLlFooter = findViewById(R.id.ll_footer);
        mLlBack = findViewById(R.id.ll_back);
        mLlForward = findViewById(R.id.ll_forward);
        mCommonWebView = findViewById(R.id.common_web_view);
        mProgressBar = findViewById(R.id.progress_circular);
        initClick();
        Log.d("CommonWebView", "context:" + context);
    }

    private void initClick() {
        mIvTopLeft.setOnClickListener(this);
        mIvTopRight.setOnClickListener(this);
        mIvTransparentClose.setOnClickListener(this);
        mLlBack.setOnClickListener(this);
        mLlForward.setOnClickListener(this);
    }

    public void initCommonWebView() {
        initWebSettings(mWebSettings);
        mCommonWebView.setWebViewClient(new CommonWebViewClient());
        mCommonWebView.setWebChromeClient(new CommonWWebChromeClient());
    }

    private class CommonWebViewClient extends android.webkit.WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("CommonWebViewClient", url);
            Uri uri = Uri.parse(url);

            String scheme = uri.getScheme();

            Log.d("CommonWebViewClient", "scheme:" + scheme);
            if (mUrlSchemeFucMap.containsKey(scheme)) {
                try {
                    mUrlSchemeFucMap.get(scheme).call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    private class CommonWWebChromeClient extends WebChromeClient {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (!mTitleText.equals("")) {
                mTvTitle.setText(mTitleText);
            } else {
                if (!title.equals("")) {
                    mTvTitle.setText(title);
                } else {
                    mTvTitle.setText("");
                }
            }
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            Log.d("CommonWWebChromeClient", "newProgress:" + newProgress);
            if (newProgress == 100) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mHasEvaluateJs) {
                    mHasEvaluateJs = false;
                    mCommonWebView.evaluateJavascript(mScript, mResultCallback);
                }
                mProgressBar.setVisibility(GONE);
            } else {
                mProgressBar.setVisibility(VISIBLE);
            }
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            String message = consoleMessage.message();
            int lineNumber = consoleMessage.lineNumber();
            String sourceID = consoleMessage.sourceId();
            String messageLevel = consoleMessage.message();


            Log.e("CommonWWebChromeClient", message);
            Log.d("CommonWWebChromeClient", "lineNumber:" + lineNumber);
/*
            Log.e("[WebView]", String.format("[%s] sourceID: %s lineNumber: %n message: %s",
                    messageLevel, sourceID, lineNumber, message));
*/
            return super.onConsoleMessage(consoleMessage);
        }
    }


    //webSetting 初始化
    private void initWebSettings(WebSettings mWebSettings) {
        mWebSettings = mCommonWebView.getSettings();
        Log.d("CommonWebView", "mWebSettings:" + mWebSettings);
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setSupportZoom(true);
        mWebSettings.setBuiltInZoomControls(false);
        mWebSettings.setSavePassword(false);
        if (CommonWebViewUtils.checkNetwork(mCommonWebView.getContext())) {
            //根据cache-control获取数据。
            mWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            //没网，则从本地获取，即离线加载
            mWebSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //适配5.0不允许http和https混合使用情况
            mWebSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            mCommonWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mCommonWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            mCommonWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        mWebSettings.setTextZoom(100);
        mWebSettings.setDatabaseEnabled(true);
        mWebSettings.setAppCacheEnabled(true);
        mWebSettings.setLoadsImagesAutomatically(true);
        mWebSettings.setSupportMultipleWindows(false);
        // 是否阻塞加载网络图片  协议http or https
        mWebSettings.setBlockNetworkImage(false);
        // 允许加载本地文件html  file协议
        mWebSettings.setAllowFileAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // 通过 file url 加载的 Javascript 读取其他的本地文件 .建议关闭
            mWebSettings.setAllowFileAccessFromFileURLs(false);
            // 允许通过 file url 加载的 Javascript 可以访问其他的源，包括其他的文件和 http，https 等其他的源
            mWebSettings.setAllowUniversalAccessFromFileURLs(false);
        }
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        } else {
            mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        }
        mWebSettings.setLoadWithOverviewMode(false);
        mWebSettings.setUseWideViewPort(false);
        mWebSettings.setDomStorageEnabled(true);
        mWebSettings.setNeedInitialFocus(true);
        mWebSettings.setDefaultTextEncodingName("utf-8");//设置编码格式
        mWebSettings.setDefaultFontSize(16);
        mWebSettings.setMinimumFontSize(12);//设置 WebView 支持的最小字体大小，默认为 8
        mWebSettings.setGeolocationEnabled(true);

        //设置数据库路径  api19 已经废弃,这里只针对 webkit 起作用

        //缓存文件最大值
        mWebSettings.setAppCacheMaxSize(Long.MAX_VALUE);
    }

    public CommonWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void loadUrl(String url) {
        mCommonWebView.loadUrl(url);
    }


    public void setUrlSchemeFucMap(Map<String, Callable> urlSchemeFucMap) {
        mUrlSchemeFucMap = urlSchemeFucMap;
    }

    /**
     * 设置自定义webview的显示样式
     *
     * @param style
     */
    public void setStyle(int style) {
        switch (style) {
            case TRADITIONAL:
                mClHeader.setVisibility(VISIBLE);
                mLlFooter.setVisibility(VISIBLE);
                mIvTransparentClose.setVisibility(GONE);
                break;
            case MODERN:
                mClHeader.setVisibility(VISIBLE);
                mLlFooter.setVisibility(GONE);
                mIvTransparentClose.setVisibility(GONE);
                break;
            case CONCISE:
                mClHeader.setVisibility(GONE);
                mLlFooter.setVisibility(GONE);
                mIvTransparentClose.setVisibility(VISIBLE);
                break;
            case FULL_SCREEN:
                mClHeader.setVisibility(GONE);
                mLlFooter.setVisibility(GONE);
                mIvTransparentClose.setVisibility(GONE);
                break;
        }
    }

    /**
     * 设置图标的颜色
     *
     * @param color
     */
    public void setTintColor(int color) {
        mIvTopLeft.setColorFilter(color);
        mIvTopRight.setColorFilter(color);
        mIvTransparentClose.setColorFilter(color);
        mIvBack.setColorFilter(color);
        mIvForward.setColorFilter(color);
    }

    /**
     * 设置title颜色
     *
     * @param color
     */
    public void setTitleColor(int color) {
        mTvTitle.setTextColor(color);
    }

    public void setTitleText(String titleText) {
        mTitleText = titleText;
    }

    /**
     * userAgent的设置
     *
     * @param userAgent
     */
    public void setUserAgent(String userAgent) {
        mWebSettings = mCommonWebView.getSettings();
        mWebSettings.setUserAgentString(userAgent);
    }

    /**
     * cookie的设定
     *
     * @param context
     * @param url
     */
    public void synCookies(Context context, String url, ArrayList<String> cookieList) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(context);
        }
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);// 允许接受 Cookie
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeSessionCookie();// 移除
        } else {
            cookieManager.removeSessionCookies(null);// 移除
        }
        cookieManager.setAcceptCookie(true);
        for (int i = 0; i < cookieList.size(); i++) {
            cookieManager.setCookie(url, cookieList.get(i));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().flush();
        } else {
            CookieSyncManager.createInstance(context);
            CookieSyncManager.getInstance().sync();
        }
    }


    @SuppressLint("JavascriptInterface")
    public void addJavascriptInterface(Object obj, String interfaceName) {
        mCommonWebView.addJavascriptInterface(obj, interfaceName);
    }

    public void evaluateJavascript(String script, ValueCallback<String> resultCallback) {
        mHasEvaluateJs = true;
        mScript = script;
        mResultCallback = resultCallback;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_top_left) {
            finishView();
        } else if (v.getId() == R.id.iv_top_right) {
            mCommonWebView.reload();
        } else if (v.getId() == R.id.iv_transparent_close) {
            finishView();
        } else if (v.getId() == R.id.ll_back) {
            if (mCommonWebView.canGoBack()) {
                mCommonWebView.goBack();
            }
        } else if (v.getId() == R.id.ll_forward) {
            if (mCommonWebView.canGoForward()) {
                mCommonWebView.goForward();
            }
        }
    }

    private void finishView() {
        if (mContext instanceof Activity) {
            ((Activity) mContext).finish();
        }
    }
}