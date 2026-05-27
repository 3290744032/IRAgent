package com.suiyuan.iragent_app.ui.desmos;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DesmosView extends WebView {
    private boolean isGeoGebraReady = false;
    private JSONArray pendingExpressions = null;

    public DesmosView(Context context) {
        super(context);
        initWebView();
    }

    public DesmosView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWebView();
    }

    public DesmosView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initWebView();
    }

    private void initWebView() {
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        setLayerType(LAYER_TYPE_HARDWARE, null);

        setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                android.util.Log.d("GeoGebraConsole",
                    consoleMessage.message() + " | 行号:" + consoleMessage.lineNumber() + " | 来源:" + consoleMessage.sourceId());
                return super.onConsoleMessage(consoleMessage);
            }
        });

        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                isGeoGebraReady = true;
                android.util.Log.d("GeoGebraView", "GeoGebra初始化完成");

                if (pendingExpressions != null) {
                    setExpressionsInternal(pendingExpressions);
                    pendingExpressions = null;
                }
            }
        });

        String htmlContent = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "    <script src='https://cdn.geogebra.org/apps/deployggb.js'></script>\n" +
                "    <style>\n" +
                "        html,body{margin:0;padding:0;height:100%;width:100%;overflow:hidden;}\n" +
                "        #ggb-element{width:100%;height:100%;}\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id='ggb-element'></div>\n" +
                "    <script>\n" +
                "        var parameters = {\n" +
                "            appName: 'graphing',\n" +
                "            width: '100%',\n" +
                "            height: '100%',\n" +
                "            showToolBar: false,\n" +
                "            showAlgebraInput: false,\n" +
                "            showMenuBar: false,\n" +
                "            enable3d: true,\n" +
                "            useBrowserForJS: true\n" +
                "        };\n" +
                "        var applet = new GGBApplet(parameters, true);\n" +
                "        applet.inject('ggb-element');\n" +
                "        \n" +
                "        function setExpressionsJson(jsonArray) {\n" +
                "            try {\n" +
                "                var expressions = JSON.parse(jsonArray);\n" +
                "                ggbApplet.reset();\n" +
                "                expressions.forEach(function(item) {\n" +
                "                    ggbApplet.evalCommand(item.latex);\n" +
                "                });\n" +
                "            } catch(e) {\n" +
                "                console.error('Error setting expressions:', e);\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        function setViewport(xmin, xmax, ymin, ymax) {\n" +
                "            try {\n" +
                "                ggbApplet.setCoordSystem(xmin, xmax, ymin, ymax);\n" +
                "            } catch(e) {\n" +
                "                console.error('Error setting viewport:', e);\n" +
                "            }\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        loadDataWithBaseURL(
                "https://www.geogebra.org",
                htmlContent,
                "text/html",
                "UTF-8",
                null
        );
    }

    public void setExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            return;
        }
        setExpressions(java.util.Collections.singletonList(expression));
    }

    public void addExpression(String expression) {
        setExpression(expression);
    }

    public void setExpressions(java.util.List<String> expressions) {
        if (expressions == null || expressions.isEmpty()) {
            clearExpressions();
            return;
        }

        JSONArray jsonArray = new JSONArray();
        for (String expr : expressions) {
            try {
                JSONObject exprObj = new JSONObject();
                exprObj.put("latex", expr.trim());
                jsonArray.put(exprObj);
            } catch (JSONException e) {
                android.util.Log.e("GeoGebraView", "表达式JSON生成失败: " + expr, e);
            }
        }

        if (!isGeoGebraReady) {
            pendingExpressions = jsonArray;
            android.util.Log.d("GeoGebraView", "暂存表达式，等待初始化: " + expressions.size() + " 个");
            return;
        }

        setExpressionsInternal(jsonArray);
    }

    private void setExpressionsInternal(JSONArray jsonArray) {
        String jsonStr = jsonArray.toString();
        try {
            new JSONArray(jsonStr);
            android.util.Log.d("GeoGebraView", "设置表达式合法JSON: " + jsonStr);
        } catch (JSONException e) {
            android.util.Log.e("GeoGebraView", "JSON格式非法，终止执行", e);
            return;
        }

        evaluateJavascript("setExpressionsJson('" + jsonStr.replace("'", "\\'") + "')", null);
        evaluateJavascript("setViewport(-10, 10, -10, 10)", null);
    }

    public void clearExpressions() {
        if (isGeoGebraReady) {
            evaluateJavascript("ggbApplet.reset();", null);
        }
        pendingExpressions = null;
    }

    public void release() {
        stopLoading();
        removeAllViews();
        destroy();
    }
}