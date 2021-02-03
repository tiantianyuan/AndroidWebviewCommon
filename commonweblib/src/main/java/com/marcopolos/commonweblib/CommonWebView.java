package com.marcopolos.commonweblib;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.customview.DialogCustomViewExtKt;
import com.afollestad.materialdialogs.list.DialogListExtKt;
import com.airbnb.lottie.LottieAnimationView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private TextView mTvTitle, mTvLoadingTitle;
    private LinearLayout mLlFooter, mLlBack, mLlForward;
    private WebView mCommonWebView;
    private LottieAnimationView lottieAnimationView;
    private RelativeLayout rlLoadingBack;
    private ImageView ivLoadingBack;
    private WebSettings mWebSettings;
    private String mTitleText = "";
    private boolean mIsFragment = false;
    private Activity mActivity;
    private Context mContext;
    private Map<String, Callable> mUrlSchemeFucMap = new HashMap<>();
    private boolean mHasEvaluateJs = false;
    private String mScript;
    private ValueCallback<String> mResultCallback;
    private int mViewStyle;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    public static final int CAMERA_FILE_REQUEST_CODE = 1;
    public static final int GALLERY_FILE_REQUEST_CODE = 2;
    public static final int REQUEST_PERMISSION_FOR_CAMERA = 3;
    public static final int REQUEST_PERMISSION_FOR_GALLERY = 4;
    private MaterialDialog dialog;
    private boolean isShowLoading = true;
    private boolean isLoadingComplete = false;
    private boolean isShowNativeBack = false;
    private boolean isAlwaysShowNativeTop = false;
    private String nativeTitle = "";

    public CommonWebView(Context context) {
        super(context);
    }

    public boolean isShowLoading() {
        return isShowLoading;
    }

    public void setShowLoading(boolean showLoading) {
        isShowLoading = showLoading;
    }

    public boolean isLoadingComplete() {
        return isLoadingComplete;
    }

    public void setShowNativeBack(boolean showNativeBack) {
        isShowNativeBack = showNativeBack;
    }


    public boolean isAlwaysShowNativeTop() {
        return isAlwaysShowNativeTop;
    }

    public void setAlwaysShowNativeTop(boolean alwaysShowNativeTop) {
        isAlwaysShowNativeTop = alwaysShowNativeTop;
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
        lottieAnimationView = findViewById(R.id.progress_view);
        rlLoadingBack = findViewById(R.id.rl_loading_back);
        mTvLoadingTitle = findViewById(R.id.tv_loading_status);
        ivLoadingBack = findViewById(R.id.iv_loading_back);
        dialog = new MaterialDialog(mContext, MaterialDialog.getDEFAULT_BEHAVIOR());
        DialogCustomViewExtKt.customView(dialog, R.layout.layout_loading_view, null, false, true, false, false).cancelable(true).cancelOnTouchOutside(false);
        TypedArray ta = context.obtainStyledAttributes(attrs,
                R.styleable.MyView);
        mViewStyle = ta.getInteger(R.styleable.MyView_view_style, 1);
        setStyle(mViewStyle);
        initClick();
        Log.d("CommonWebView", "context:" + context);
    }

    private void initClick() {
        mIvTopLeft.setOnClickListener(this);
        mIvTopRight.setOnClickListener(this);
        mIvTransparentClose.setOnClickListener(this);
        mLlBack.setOnClickListener(this);
        mLlForward.setOnClickListener(this);
        ivLoadingBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finishPage();
            }
        });
    }

    public void initCommonWebView() {
        initWebSettings(mWebSettings);
        mCommonWebView.setWebViewClient(new CommonWebViewClient());
        mCommonWebView.setWebChromeClient(new CommonWWebChromeClient());
        Log.d("CommonWebView", "mCommonWebView:" + mCommonWebView.getSettings().getUserAgentString());
    }

    public void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(mContext.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(mContext,
                        mContext.getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                ((Activity) mContext).startActivityForResult(takePictureIntent, CAMERA_FILE_REQUEST_CODE);
            }
        }
    }

    public void pickImage() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/jpeg");
        }
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        ((Activity) mContext).startActivityForResult(intent, GALLERY_FILE_REQUEST_CODE);
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

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            mTvLoadingTitle.setText("読み込みに失敗しました。");
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
            nativeTitle = title;
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
                isLoadingComplete = true;
                dissmissLoading();
                if (isAlwaysShowNativeTop()){
                    mTvLoadingTitle.setText(nativeTitle);
                }else {
                    rlLoadingBack.setVisibility(GONE);
                }
            } else {
                if (isShowLoading()) {
                    showLoading();
                }
                if (isShowNativeBack) {
                    rlLoadingBack.setVisibility(VISIBLE);
                }
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

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.d("CommonWWebChromeClient", "result:" + result);
            return super.onJsAlert(view, url, message, result);

        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
            Log.d("CommonWWebChromeClient", "result:" + result);
            return super.onJsPrompt(view, url, message, defaultValue, result);
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;

            MaterialDialog dialog = new MaterialDialog(mContext, MaterialDialog.getDEFAULT_BEHAVIOR());
            ArrayList<String> mList = new ArrayList<>();
            mList.add("拍照");
            mList.add("相册");
            DialogListExtKt.listItems(dialog, null, mList, null, false, (materialDialog, index, text) -> {
                if (index == 0) {
                    if (hasSelfPermission((Activity) mContext, Manifest.permission.CAMERA)
                            && hasSelfPermission((Activity) mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                            && hasSelfPermission((Activity) mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        dispatchTakePictureIntent();
                    } else {
                        ((Activity) mContext).requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_FOR_CAMERA);
                    }
                } else if (index == 1) {
                    // 「ライブラリから選ぶ」を選択
                    if (hasSelfPermission((Activity) mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                            && hasSelfPermission((Activity) mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        pickImage();
                    } else {
                        ((Activity) mContext).requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_FOR_GALLERY);
                    }
                }
                return null;
            }).cancelOnTouchOutside(false).setOnCancelListener(dialog1 -> {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                    mFilePathCallback = null;
                }
            });

            dialog.show();
            return true;
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            super.onPermissionRequest(request);
            Log.d("CommonWWebChromeClient", "request:" + request);
        }
    }

    public ValueCallback<Uri[]> getFilePathCallback() {
        return mFilePathCallback;
    }

    public void setFilePathCallbackNull() {
        mFilePathCallback = null;
    }


    public String getCameraPhotoPath() {
        return mCameraPhotoPath;
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

    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        mCommonWebView.loadUrl(url, additionalHttpHeaders);
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

    public void setIsFragment(boolean isFragment) {
        mIsFragment = isFragment;
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
     * 获取webview的实例
     *
     * @param userAgent
     */
    public WebView getWebview() {
        return mCommonWebView;
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

    //拍照获取文件的创建
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCameraPhotoPath = image.getAbsolutePath();
        return image;
    }

    @SuppressLint("NewApi")
    public static boolean hasSelfPermission(Activity activity, String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void finishPage() {
        //dialog.show();
        if (mContext instanceof Activity) {
            if (mIsFragment) {
                mCommonWebView.goBack();
            } else {
                ((Activity) mContext).finish();
            }
        }
    }


    private void showLoading() {
        //dialog.show();
        lottieAnimationView.setVisibility(View.VISIBLE);
        lottieAnimationView.playAnimation();
    }

    private void dissmissLoading() {
        //dialog.dismiss();
        lottieAnimationView.setVisibility(View.GONE);
    }
}
