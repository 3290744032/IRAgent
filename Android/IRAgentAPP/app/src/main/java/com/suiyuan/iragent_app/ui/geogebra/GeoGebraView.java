package com.suiyuan.iragent_app.ui.geogebra;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GeoGebraView extends FrameLayout {

    private final WebView mWebView;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final List<String> mPendingExpressions = new ArrayList<>();
    private boolean mIsGgbReady = false;
    private boolean mIsDestroyed = false;

    public interface OnGeoGebraReadyListener {
        void onReady();
    }
    private OnGeoGebraReadyListener mReadyListener;

    public GeoGebraView(@NonNull Context context) {
        this(context, null);
    }

    public GeoGebraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mWebView = new WebView(context);
        initWebView();
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.topMargin = 0;
        mWebView.setPadding(0, 0, 0, 0);
        mWebView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        addView(mWebView, params);
    }

    private void initWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setDatabaseEnabled(true);
        settings.setOffscreenPreRaster(false);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowFileAccessFromFileURLs(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 使用软件渲染，避免某些设备上 WebGL 层级遮挡问题
        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mWebView.setBackgroundColor(Color.WHITE);
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);

        mWebView.addJavascriptInterface(new GeoGebraBridge(), "androidBridge");

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (mIsDestroyed) return;
                android.util.Log.d("GeoGebraView", "页面加载完成，等待 JS 初始化...");
            }

            @Override
            public void onReceivedHttpError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                android.util.Log.e("GeoGebraView", "资源加载失败: " + request.getUrl() + " | 错误码: " + errorResponse.getStatusCode());
            }

            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                android.util.Log.e("GeoGebraView", "页面加载错误: " + error.getDescription());
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient());
        
        // 强制清理缓存
        mWebView.clearCache(true);
        mWebView.clearHistory();
        
        // 使用本地 HTML 文件，避免动态生成带来的问题
        mWebView.loadUrl("file:///android_asset/geogebra/index.html");
    }

    public void setOnGeoGebraReadyListener(OnGeoGebraReadyListener listener) {
        this.mReadyListener = listener;
        if (mIsGgbReady) {
            mMainHandler.post(() -> listener.onReady());
        }
    }

    public void setExpressions(List<String> expressions) {
        if (expressions == null || expressions.isEmpty()) return;
        
        mMainHandler.post(() -> {
            if (mIsDestroyed) return;
            
            if (mIsGgbReady) {
                executeSetExpressions(expressions);
            } else {
                android.util.Log.d("GeoGebraView", "GeoGebra 未就绪，暂存表达式");
                mPendingExpressions.clear();
                mPendingExpressions.addAll(expressions);
            }
        });
    }
    
    private void executeSetExpressions(List<String> expressions) {
        mMainHandler.post(() -> {
            if (mIsDestroyed) return;
            
            StringBuilder jsArray = new StringBuilder("[");
            for (int i = 0; i < expressions.size(); i++) {
                if (i > 0) jsArray.append(",");
                String optimized = optimizeGeoGebraExpression(expressions.get(i));
                jsArray.append("'").append(escapeJsString(optimized)).append("'");
            }
            jsArray.append("]");
            
            android.util.Log.d("GeoGebraView", "执行绘图: " + jsArray);
            
            String zoomJs = "if(typeof ggbApplet!=='undefined'){ggbApplet.setCoordSystem(-4.5,4.5,-2.5,2.5);}";
            String resizeJs = "window.dispatchEvent(new Event('resize'));";
            String execJs = "try{" + zoomJs + "window.setExpressions(" + jsArray + ");" + resizeJs + "}catch(e){console.error(e);}";
            
            mWebView.evaluateJavascript(execJs, value -> {
                android.util.Log.d("GeoGebraView", "绘图执行完成: " + value);
            });
        });
    }

    private String optimizeGeoGebraExpression(String expr) {
        if (expr == null || expr.isEmpty()) return "";
        String result = expr;
        result = result.replace("\u03C0", "pi");
        result = result.replace("π", "pi");
        result = result.replaceAll("(?i)\\barctan\\s*\\(", "atan(");
        return result;
    }

    private String escapeJsString(String content) {
        if (content == null || content.isEmpty()) return "";
        return content.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public void release() {
        mIsDestroyed = true;
        mIsGgbReady = false;
        mMainHandler.removeCallbacksAndMessages(null);
        mPendingExpressions.clear();
        if (mWebView != null) {
            ((ViewGroup) mWebView.getParent()).removeView(mWebView);
            mWebView.stopLoading();
            mWebView.loadUrl("about:blank");
            mWebView.removeJavascriptInterface("androidBridge");
            mWebView.setWebViewClient(null);
            mWebView.setWebChromeClient(null);
            mWebView.destroy();
        }
        removeAllViews();
    }

    private class GeoGebraBridge {
        @JavascriptInterface
        public void onGeoGebraReady() {
            mMainHandler.post(() -> {
                if (mIsDestroyed) return;
                mIsGgbReady = true;
                android.util.Log.d("GeoGebraView", "收到 Web 端的正式就绪信号，GeoGebra 核心引擎可用");
                if (!mPendingExpressions.isEmpty()) {
                    executeSetExpressions(new ArrayList<>(mPendingExpressions));
                    mPendingExpressions.clear();
                }
                if (mReadyListener != null) {
                    mReadyListener.onReady();
                }
            });
        }
    }
}
