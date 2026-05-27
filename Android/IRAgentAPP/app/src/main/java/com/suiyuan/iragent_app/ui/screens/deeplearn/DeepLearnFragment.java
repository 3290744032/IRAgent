package com.suiyuan.iragent_app.ui.screens.deeplearn;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v2.LearningStep;
import com.suiyuan.iragent_app.data.model.v2.SessionHistoryItem;
import com.suiyuan.iragent_app.data.model.v2.SessionSummaryResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeepLearnFragment extends Fragment {

    private DeepLearnViewModel viewModel;

    private ImageView ivBack, ivHistory;
    private TextView tvTitle;
    private ScrollView svMessages;
    private LinearLayout llMessages;
    private EditText etInput;
    private ImageView ivSend;
    private View layoutStepActions, layoutSummaryReady;
    private TextView btnUnderstand, btnNotUnderstand, btnContinue, btnViewSummary;
    private SessionSummaryResponse mSummaryResponse;

    private String mMathTemplate;
    private WebView mStreamingWebView;
    private View mStreamingContainer;
    private LinearLayout mStreamingContentLayout;
    private List<String> mStreamingDesmosList;
    private String mStreamingPlot3DConfig;

    private static final long RENDER_INTERVAL_MS = 200;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRenderRunnable = this::doSafeRender;
    private String mCurrentFullText = "";
    private String mLastRenderedText = "";
    private boolean mIsStreaming = false;
    private boolean mWebViewReady = false;
    private boolean mIsSummaryMode = false;
    private boolean mIsHistoryLoad = false;
    private View mLoadingView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deep_learn, container, false);
        initViews(view);
        loadMathTemplate();
        viewModel = new ViewModelProvider(this).get(DeepLearnViewModel.class);

        setupListeners();
        setupObservers();

        addAIMessage("你好！你想学习什么？输入你的问题，我会一步步讲解给你听。");
        etInput.setHint("输入你的问题...");

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMainHandler.removeCallbacksAndMessages(null);
        mIsStreaming = false;
        mWebViewReady = false;
        if (mStreamingWebView != null) {
            mStreamingWebView.stopLoading();
            mStreamingWebView.destroy();
            mStreamingWebView = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mStreamingWebView != null) mStreamingWebView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mStreamingWebView != null) mStreamingWebView.onResume();
    }

    private void initViews(View view) {
        ivBack = view.findViewById(R.id.iv_back);
        ivHistory = view.findViewById(R.id.iv_history);
        tvTitle = view.findViewById(R.id.tv_deep_title);
        svMessages = view.findViewById(R.id.sv_messages);
        llMessages = view.findViewById(R.id.ll_messages);
        etInput = view.findViewById(R.id.et_input);
        ivSend = view.findViewById(R.id.iv_send);
        layoutStepActions = view.findViewById(R.id.layout_step_actions);
        layoutSummaryReady = view.findViewById(R.id.layout_summary_ready);
        btnUnderstand = view.findViewById(R.id.btn_understand);
        btnNotUnderstand = view.findViewById(R.id.btn_not_understand);
        btnContinue = view.findViewById(R.id.btn_continue);
        btnViewSummary = view.findViewById(R.id.btn_view_summary);
        mLoadingView = view.findViewById(R.id.layout_loading);
    }

    private void loadMathTemplate() {
        try {
            InputStream is = requireContext().getAssets().open("math_template.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            reader.close();
            is.close();
            mMathTemplate = sb.toString();
        } catch (IOException e) {
            mMathTemplate = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body><div id='content'></div></body></html>";
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> requireActivity().onBackPressed());
        ivHistory.setOnClickListener(v -> showHistory());
        ivSend.setOnClickListener(v -> handleSend());

        etInput.setOnEditorActionListener((v, actionId, event) -> { handleSend(); return true; });
        btnUnderstand.setOnClickListener(v -> {
            layoutStepActions.setVisibility(View.GONE);
            addUserMessage("听懂了");
            viewModel.submitAnswer("听懂了");
        });
        btnNotUnderstand.setOnClickListener(v -> {
            layoutStepActions.setVisibility(View.GONE);
            String msg = "没听懂，能再解释一遍吗？";
            addUserMessage(msg);
            viewModel.submitAnswer(msg);
        });
        btnContinue.setOnClickListener(v -> {
            layoutStepActions.setVisibility(View.GONE);
            addUserMessage("直接继续讲");
            viewModel.submitAnswer("直接继续讲");
        });
        btnViewSummary.setOnClickListener(v -> showSummaryPage());
    }

    private static final String TAG = "DeepLearnFragment";

    private void setupObservers() {
        viewModel.getSessionLiveData().observe(getViewLifecycleOwner(), session -> {
            if (session != null) {
                String topic = session.getTopic() != null ? session.getTopic() : "深度学习";
                tvTitle.setText(topic);

                if (mIsHistoryLoad && session.getSteps() != null && !session.getSteps().isEmpty()) {
                    mIsHistoryLoad = false;
                    android.util.Log.d(TAG, "Rendering history session: id=" + session.getSessionId()
                            + ", steps=" + session.getSteps().size());
                    llMessages.removeAllViews();
                    if (session.getQuestion() != null && !session.getQuestion().isEmpty()) {
                        addUserMessage(session.getQuestion());
                    }
                    for (LearningStep step : session.getSteps()) {
                        String title = step.getTitle() != null ? step.getTitle() : "";
                        String content = step.getContent();
                        if (content != null && !content.isEmpty()) {
                            addAIMessage("### " + title + "\n\n" + content);
                        }
                    }
                }
            }
        });

        viewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), loading -> {
            ivSend.setEnabled(!loading);
            etInput.setEnabled(!loading);
            if (mLoadingView != null) {
                mLoadingView.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getIsSessionCompleteLiveData().observe(getViewLifecycleOwner(), completed -> {
            if (completed) mIsSummaryMode = true;
        });

        viewModel.getIsTeachingLiveData().observe(getViewLifecycleOwner(), isTeaching -> {
            if (isTeaching != null && !isTeaching) {
                android.util.Log.d(TAG, "Stream finished, fullText length=" + mCurrentFullText.length());
                finishStreamingMessage();
            }
            ivSend.setEnabled(isTeaching == null || !isTeaching);
            boolean showActions = isTeaching != null && !isTeaching
                    && viewModel.getSession() != null && !mIsSummaryMode;
            layoutStepActions.setVisibility(showActions ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                android.util.Log.e(TAG, "Error: " + error);
                if (mLoadingView != null) mLoadingView.setVisibility(View.GONE);
                addAIMessage("❌ " + error);
            }
        });

        viewModel.getTeachContentLiveData().observe(getViewLifecycleOwner(), text -> {
            if (text != null && isAdded()) {
                appendStreamingText(text);
            }
        });

        viewModel.getSummaryLiveData().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null && isAdded()) {
                mSummaryResponse = summary;
                tvTitle.setText("学习总结");
                etInput.setHint("再问一题，或输入新问题开始新对话...");
                layoutSummaryReady.setVisibility(View.VISIBLE);
                android.util.Log.d(TAG, "Summary ready, showing view summary button");
            }
        });
        viewModel.getSummaryContentLiveData().observe(getViewLifecycleOwner(), text -> {
            if (text != null && isAdded()) {
                android.util.Log.d(TAG, "summaryStream text: length=" + text.length());
            }
        });

        viewModel.getHistoryLiveData().observe(getViewLifecycleOwner(), items -> {
            if (items != null) showHistoryDialog(items);
        });
    }

    private void handleSend() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;

        addUserMessage(text);
        etInput.setText("");

        if (mIsSummaryMode) {
            mIsSummaryMode = false;
            View summaryView = getView().findViewById(R.id.ll_summary);
            summaryView.setVisibility(View.GONE);
            svMessages.setVisibility(View.VISIBLE);
            llMessages.removeAllViews();
            tvTitle.setText("深度学习");
            layoutSummaryReady.setVisibility(View.GONE);
            mSummaryResponse = null;
        }

        viewModel.createSession(text, "general");
        startStreamingMessage();
    }

    private void addUserMessage(String text) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.item_message_user, llMessages, false);
        ((TextView) v.findViewById(R.id.tv_content)).setText(text);
        llMessages.addView(v);
        scrollToBottom();
    }

    private void addAIMessage(String text) {
        View container = LayoutInflater.from(getContext()).inflate(R.layout.item_message_ai_container, llMessages, false);
        LinearLayout contentLayout = container.findViewById(R.id.ll_content);

        boolean hasPlot = text.contains("【PLOT】") || text.contains("【GEGEBRA】");
        boolean hasPlot3D = text.contains("【PLOT3D】");

        if (hasPlot || hasPlot3D) {
            // Render text without tags
            WebView webView = createMathWebView();
            String textWithoutTags = removeGeogebraTags(text);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    String escaped = escapeJsString(textWithoutTags);
                    view.evaluateJavascript("renderMathContent('" + escaped + "')", null);
                }
            });
            webView.loadDataWithBaseURL("https://cdn.jsdelivr.net", mMathTemplate, "text/html", "UTF-8", null);
            contentLayout.addView(webView);

            // Extract and add 2D plot
            if (hasPlot) {
                List<String> expressions = new ArrayList<>();
                Pattern pattern = Pattern.compile("【(?:GEGEBRA|PLOT)】([\\s\\S]*?)(【END】|$)");
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    String exprBlock = matcher.group(1).trim();
                    if (!exprBlock.isEmpty()) {
                        String[] exprs = exprBlock.split("\\r?\\n");
                        for (String expr : exprs) {
                            String trimmedExpr = expr.trim();
                            if (!trimmedExpr.isEmpty() && !expressions.contains(trimmedExpr)) {
                                expressions.add(trimmedExpr);
                            }
                        }
                    }
                }
                if (!expressions.isEmpty()) {
                    addGeoGebraContainer(contentLayout, expressions);
                }
            }

            // Extract and add 3D plot
            if (hasPlot3D) {
                Pattern pattern = Pattern.compile("【PLOT3D】([\\s\\S]*?)(【END】|$)");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String configBlock = matcher.group(1).trim();
                    android.util.Log.d(TAG, "addAIMessage: found PLOT3D config, len=" + configBlock.length()
                            + ", startsWith={=" + configBlock.startsWith("{"));
                    addPlot3DContainer(contentLayout, configBlock);
                } else {
                    android.util.Log.w(TAG, "addAIMessage: hasPlot3D=true but regex not matched");
                }
            }
        } else {
            WebView webView = createMathWebView();
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    String escaped = escapeJsString(text);
                    view.evaluateJavascript("renderMathContent('" + escaped + "')", null);
                }
            });
            webView.loadDataWithBaseURL("https://cdn.jsdelivr.net", mMathTemplate, "text/html", "UTF-8", null);
            contentLayout.addView(webView);
        }

        llMessages.addView(container);
        scrollToBottom();
    }

    // ========== 流式渲染（照抄 StudyFragment 的成熟模式） ==========

    private void startStreamingMessage() {
        mIsStreaming = true;
        mCurrentFullText = "";
        mLastRenderedText = "";
        mWebViewReady = false;
        mStreamingDesmosList = new ArrayList<>();
        mStreamingPlot3DConfig = null;
        mMainHandler.removeCallbacksAndMessages(null);

        mStreamingContainer = LayoutInflater.from(getContext()).inflate(R.layout.item_message_ai_container, llMessages, false);
        mStreamingContentLayout = mStreamingContainer.findViewById(R.id.ll_content);

        mStreamingWebView = createMathWebView();
        mStreamingWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mWebViewReady = true;
                doSafeRender();
            }
        });
        mStreamingWebView.loadDataWithBaseURL("https://cdn.jsdelivr.net", mMathTemplate, "text/html", "UTF-8", null);
        mStreamingContentLayout.addView(mStreamingWebView);
        llMessages.addView(mStreamingContainer);
        scrollToBottom();
    }

    private void appendStreamingText(String fullText) {
        if (!mIsStreaming) {
            startStreamingMessage();
        }
        mCurrentFullText = fullText;
        mMainHandler.removeCallbacks(mRenderRunnable);
        mMainHandler.postDelayed(mRenderRunnable, RENDER_INTERVAL_MS);
    }

    private void doSafeRender() {
        if (mStreamingWebView == null || !mIsStreaming || getContext() == null || !mWebViewReady) {
            return;
        }
        String textToRender = mCurrentFullText;
        if (textToRender.equals(mLastRenderedText)) {
            return;
        }
        textToRender = removeUnclosedMarkdownAndMath(textToRender);
        String escapedContent = escapeJsString(textToRender);
        mStreamingWebView.evaluateJavascript("renderMathContent('" + escapedContent + "')", null);
        mLastRenderedText = textToRender;
        scrollToBottom();
    }

    private void finishStreamingMessage() {
        mIsStreaming = false;
        if (mStreamingContainer == null) return;

        mMainHandler.removeCallbacksAndMessages(null);

        android.util.Log.d(TAG, "finishStreamingMessage: total chars=" + mCurrentFullText.length()
                + ", hasGeoGebra=" + mCurrentFullText.contains("【GEGEBRA】")
                + ", hasPLOT=" + mCurrentFullText.contains("【PLOT】")
                + ", hasPLOT3D=" + mCurrentFullText.contains("【PLOT3D】"));
        if (!mCurrentFullText.isEmpty()) {
            String full = mCurrentFullText;
            int chunkSize = 3000;
            for (int i = 0; i < full.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, full.length());
                android.util.Log.d(TAG, "RECEIVED_CONTENT[" + i + "-" + end + "]: " + full.substring(i, end));
            }
        } else {
            android.util.Log.w(TAG, "RECEIVED_CONTENT: (empty)");
        }

        extractGeogebraFromText(mCurrentFullText);

        if (mWebViewReady && mStreamingWebView != null) {
            String textWithoutTags = removeGeogebraTags(mCurrentFullText);
            String textToRender = removeUnclosedMarkdownAndMath(textWithoutTags);
            String escapedContent = escapeJsString(textToRender);
            mStreamingWebView.evaluateJavascript("renderMathContent('" + escapedContent + "')", null);
        }

        LinearLayout contentLayout = mStreamingContainer.findViewById(R.id.ll_content);
        if (mStreamingDesmosList != null && !mStreamingDesmosList.isEmpty()) {
            addGeoGebraContainer(contentLayout, mStreamingDesmosList);
        }
        if (mStreamingPlot3DConfig != null && !mStreamingPlot3DConfig.isEmpty()) {
            addPlot3DContainer(contentLayout, mStreamingPlot3DConfig);
        }

        mStreamingContainer = null;
        mStreamingWebView = null;
        mCurrentFullText = "";
        mLastRenderedText = "";
        mStreamingDesmosList = null;
        mStreamingPlot3DConfig = null;
        mWebViewReady = false;
        scrollToBottom();
    }

    // ========== 图像提取（2D/3D，从 StudyFragment 移植） ==========

    private void extractGeogebraFromText(String text) {
        if (text == null || text.isEmpty()) return;

        Pattern pattern = Pattern.compile("【(?:GEGEBRA|PLOT)】([\\s\\S]*?)(【END】|$)");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String exprBlock = matcher.group(1).trim();
            if (!exprBlock.isEmpty()) {
                String[] exprs = exprBlock.split("\\r?\\n");
                for (String expr : exprs) {
                    String trimmedExpr = expr.trim();
                    if (!trimmedExpr.isEmpty()) {
                        addUniqueExpression(mStreamingDesmosList, trimmedExpr);
                    }
                }
            }
        }

        extractPlot3DFromText(text);
    }

    private void extractPlot3DFromText(String text) {
        if (text == null || text.isEmpty()) return;

        Pattern pattern = Pattern.compile("【PLOT3D】([\\s\\S]*?)(【END】|$)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String configBlock = matcher.group(1).trim();
            mStreamingPlot3DConfig = configBlock;
        }
    }

    private String removeGeogebraTags(String text) {
        if (text == null) return "";
        text = text.replaceAll("【(?:GEGEBRA|PLOT)】[\\s\\S]*?(【END】|$)", "");
        text = text.replaceAll("【PLOT3D】[\\s\\S]*?(【END】|$)", "");
        return text.trim();
    }

    private void addUniqueExpression(List<String> expressions, String expr) {
        if (expressions == null || expr == null) return;
        String trimmedExpr = expr.trim();
        if (!trimmedExpr.isEmpty() && !expressions.contains(trimmedExpr)) {
            expressions.add(trimmedExpr);
        }
    }

    private void addGeoGebraContainer(LinearLayout contentLayout, List<String> expressions) {
        FrameLayout container = new FrameLayout(getContext());
        int heightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 260, getResources().getDisplayMetrics());
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
        containerParams.setMargins(0, 12, 0, 12);
        container.setLayoutParams(containerParams);
        container.setBackgroundColor(Color.WHITE);

        ImageView staticImageView = new ImageView(getContext());
        staticImageView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        staticImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        staticImageView.setBackgroundColor(Color.WHITE);
        container.addView(staticImageView);

        TextView errorView = new TextView(getContext());
        errorView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        errorView.setGravity(Gravity.CENTER);
        errorView.setText("图像生成失败");
        errorView.setTextColor(Color.GRAY);
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        errorView.setVisibility(View.GONE);

        LinearLayout loadingView = new LinearLayout(getContext());
        loadingView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        loadingView.setGravity(Gravity.CENTER);
        loadingView.setOrientation(LinearLayout.VERTICAL);
        ProgressBar progressBar = new ProgressBar(getContext());
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        loadingView.addView(progressBar);
        TextView loadingText = new TextView(getContext());
        loadingText.setText("正在生成图像...");
        loadingText.setTextColor(Color.GRAY);
        loadingText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        loadingText.setPadding(0, 8, 0, 0);
        loadingView.addView(loadingText);
        container.addView(loadingView);

        WebView renderWebView = new WebView(getContext());
        renderWebView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        renderWebView.setAlpha(0.01f);
        renderWebView.setBackgroundColor(Color.WHITE);
        renderWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        renderWebView.setVerticalScrollBarEnabled(false);
        renderWebView.setHorizontalScrollBarEnabled(false);
        renderWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        WebSettings renderSettings = renderWebView.getSettings();
        renderSettings.setJavaScriptEnabled(true);
        renderSettings.setDomStorageEnabled(true);
        renderSettings.setAllowFileAccess(true);
        renderSettings.setAllowContentAccess(true);
        renderSettings.setAllowUniversalAccessFromFileURLs(true);
        renderSettings.setAllowFileAccessFromFileURLs(true);
        renderSettings.setLoadWithOverviewMode(true);
        renderSettings.setUseWideViewPort(true);
        renderSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        renderWebView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onGeoGebraReady() {
            }

            @android.webkit.JavascriptInterface
            public void onImageCaptured(String base64) {
                mMainHandler.post(() -> {
                    try {
                        if (base64 == null || base64.trim().isEmpty()) {
                            loadingView.setVisibility(View.GONE);
                            errorView.setVisibility(View.VISIBLE);
                            mMainHandler.postDelayed(() -> {
                                if (renderWebView.getParent() == container) {
                                    container.removeView(renderWebView);
                                }
                                renderWebView.destroy();
                            }, 500);
                            return;
                        }

                        String pureBase64 = base64;
                        if (base64.contains(",")) {
                            pureBase64 = base64.substring(base64.indexOf(",") + 1);
                        }

                        byte[] decodedString = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT);
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                        if (bitmap != null) {
                            loadingView.setVisibility(View.GONE);
                            staticImageView.setImageBitmap(bitmap);
                            staticImageView.setBackgroundColor(Color.WHITE);
                            errorView.setVisibility(View.GONE);
                        } else {
                            loadingView.setVisibility(View.GONE);
                            errorView.setVisibility(View.VISIBLE);
                        }

                        mMainHandler.postDelayed(() -> {
                            if (renderWebView.getParent() == container) {
                                container.removeView(renderWebView);
                            }
                            renderWebView.destroy();
                        }, 500);

                    } catch (Exception e) {
                        loadingView.setVisibility(View.GONE);
                        errorView.setVisibility(View.VISIBLE);
                        mMainHandler.postDelayed(() -> {
                            if (renderWebView.getParent() == container) {
                                container.removeView(renderWebView);
                            }
                            renderWebView.destroy();
                        }, 500);
                    }
                });
            }
        }, "androidBridge");

        renderWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.postDelayed(() -> {
                    String jsonExpr = new com.google.gson.Gson().toJson(expressions);
                    view.evaluateJavascript("setExpressions(" + jsonExpr + ")", null);
                }, 200);
            }
        });

        container.addView(renderWebView);
        contentLayout.addView(container);

        renderWebView.loadUrl("file:///android_asset/geogebra/index.html");
    }

    private void addPlot3DContainer(LinearLayout contentLayout, String plot3DConfig) {
        android.util.Log.d(TAG, "addPlot3DContainer called, config startsWith={="
                + (plot3DConfig != null ? plot3DConfig.substring(0, Math.min(50, plot3DConfig.length())) : "null"));

        FrameLayout container = new FrameLayout(getContext());
        int heightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
        containerParams.setMargins(0, 12, 0, 12);
        container.setLayoutParams(containerParams);
        container.setBackgroundColor(Color.WHITE);

        ImageView staticImageView = new ImageView(getContext());
        staticImageView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        staticImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        staticImageView.setBackgroundColor(Color.WHITE);
        container.addView(staticImageView);

        TextView errorView = new TextView(getContext());
        errorView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        errorView.setGravity(Gravity.CENTER);
        errorView.setText("3D图像生成失败");
        errorView.setTextColor(Color.GRAY);
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        errorView.setVisibility(View.GONE);
        container.addView(errorView);

        LinearLayout loadingView = new LinearLayout(getContext());
        loadingView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        loadingView.setGravity(Gravity.CENTER);
        loadingView.setOrientation(LinearLayout.VERTICAL);
        ProgressBar progressBar = new ProgressBar(getContext());
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        loadingView.addView(progressBar);
        TextView loadingText = new TextView(getContext());
        loadingText.setText("正在生成3D图像...");
        loadingText.setTextColor(Color.GRAY);
        loadingText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        loadingText.setPadding(0, 8, 0, 0);
        loadingView.addView(loadingText);
        container.addView(loadingView);

        WebView renderWebView = new WebView(getContext());
        renderWebView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        renderWebView.setBackgroundColor(Color.WHITE);
        renderWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        renderWebView.setVerticalScrollBarEnabled(false);
        renderWebView.setHorizontalScrollBarEnabled(false);
        renderWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        WebSettings renderSettings = renderWebView.getSettings();
        renderSettings.setJavaScriptEnabled(true);
        renderSettings.setDomStorageEnabled(true);
        renderSettings.setAllowFileAccess(true);
        renderSettings.setAllowContentAccess(true);
        renderSettings.setAllowUniversalAccessFromFileURLs(true);
        renderSettings.setAllowFileAccessFromFileURLs(true);
        renderSettings.setLoadWithOverviewMode(true);
        renderSettings.setUseWideViewPort(true);
        renderSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        renderWebView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onRenderComplete(String base64) {
                mMainHandler.post(() -> {
                    try {
                        if (base64 == null || base64.trim().isEmpty()) {
                            loadingView.setVisibility(View.GONE);
                            errorView.setVisibility(View.VISIBLE);
                            return;
                        }

                        String pureBase64 = base64;
                        if (base64.contains(",")) {
                            pureBase64 = base64.substring(base64.indexOf(",") + 1);
                        }

                        byte[] decodedString = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT);
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                        if (bitmap != null) {
                            loadingView.setVisibility(View.GONE);
                            staticImageView.setImageBitmap(bitmap);
                            errorView.setVisibility(View.GONE);
                        } else {
                            loadingView.setVisibility(View.GONE);
                            errorView.setVisibility(View.VISIBLE);
                        }

                        mMainHandler.postDelayed(() -> {
                            if (renderWebView.getParent() == container) {
                                container.removeView(renderWebView);
                            }
                            renderWebView.destroy();
                        }, 500);

                    } catch (Exception e) {
                        loadingView.setVisibility(View.GONE);
                        errorView.setVisibility(View.VISIBLE);
                    }
                });
            }
        }, "androidBridge");

        renderWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.postDelayed(() -> {
                    String jsCall;
                    if (plot3DConfig != null && plot3DConfig.trim().startsWith("{")) {
                        jsCall = "render3D(" + plot3DConfig + ")";
                    } else {
                        String escaped = plot3DConfig != null ? plot3DConfig.replace("\n", "\\n").replace("'", "\\'") : "";
                        jsCall = "render3D('" + escaped + "')";
                    }
                    view.evaluateJavascript(jsCall, null);
                }, 200);
            }
        });

        container.addView(renderWebView);
        contentLayout.addView(container);

        renderWebView.loadUrl("file:///android_asset/geogebra/3d_renderer.html");
    }

    private String removeUnclosedMarkdownAndMath(String text) {
        if (text == null || text.isEmpty()) return text;
        String processed = text;

        int dollarCount = 0;
        for (char c : processed.toCharArray()) {
            if (c == '$') dollarCount++;
        }
        if (dollarCount % 2 != 0) {
            int lastDollar = processed.lastIndexOf('$');
            if (lastDollar != -1) processed = processed.substring(0, lastDollar);
        }

        int boldCount = 0;
        int lastBoldIndex = -1;
        Matcher boldMatcher = Pattern.compile("\\*\\*").matcher(processed);
        while (boldMatcher.find()) { boldCount++; lastBoldIndex = boldMatcher.start(); }
        if (boldCount % 2 != 0 && lastBoldIndex != -1) processed = processed.substring(0, lastBoldIndex);

        int italicCount = 0;
        int lastItalicIndex = -1;
        Matcher italicMatcher = Pattern.compile("(?<!\\\\)\\*(?!\\*)").matcher(processed);
        while (italicMatcher.find()) { italicCount++; lastItalicIndex = italicMatcher.start(); }
        if (italicCount % 2 != 0 && lastItalicIndex != -1) processed = processed.substring(0, lastItalicIndex);

        int codeCount = 0;
        int lastCodeIndex = -1;
        Matcher codeMatcher = Pattern.compile("`").matcher(processed);
        while (codeMatcher.find()) { codeCount++; lastCodeIndex = codeMatcher.start(); }
        if (codeCount % 2 != 0 && lastCodeIndex != -1) processed = processed.substring(0, lastCodeIndex);

        int blockCodeCount = 0;
        int lastBlockCodeIndex = -1;
        Matcher blockCodeMatcher = Pattern.compile("```").matcher(processed);
        while (blockCodeMatcher.find()) { blockCodeCount++; lastBlockCodeIndex = blockCodeMatcher.start(); }
        if (blockCodeCount % 2 != 0 && lastBlockCodeIndex != -1) processed = processed.substring(0, lastBlockCodeIndex);

        int parenOpenCount = 0, parenCloseCount = 0;
        int lastParenOpenIndex = -1;
        int idx = 0;
        while ((idx = processed.indexOf("\\(", idx)) != -1) { parenOpenCount++; lastParenOpenIndex = idx; idx += 2; }
        idx = 0;
        while ((idx = processed.indexOf("\\)", idx)) != -1) { parenCloseCount++; idx += 2; }
        if (parenOpenCount > parenCloseCount && lastParenOpenIndex != -1)
            processed = processed.substring(0, lastParenOpenIndex);

        int bracketOpenCount = 0, bracketCloseCount = 0;
        int lastBracketOpenIndex = -1;
        idx = 0;
        while ((idx = processed.indexOf("\\[", idx)) != -1) { bracketOpenCount++; lastBracketOpenIndex = idx; idx += 2; }
        idx = 0;
        while ((idx = processed.indexOf("\\]", idx)) != -1) { bracketCloseCount++; idx += 2; }
        if (bracketOpenCount > bracketCloseCount && lastBracketOpenIndex != -1)
            processed = processed.substring(0, lastBracketOpenIndex);

        return processed;
    }

    private String escapeJsString(String content) {
        if (content == null || content.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\u2028': sb.append("\\u2028"); break;
                case '\u2029': sb.append("\\u2029"); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    private WebView createMathWebView() {
        WebView webView = new WebView(requireContext());
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        webView.setBackgroundColor(0);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        return webView;
    }

    private void showSummaryPage() {
        if (mSummaryResponse == null) return;
        layoutSummaryReady.setVisibility(View.GONE);
        onSummaryLoaded(mSummaryResponse);
    }

    private void onSummaryLoaded(SessionSummaryResponse summary) {
        if (summary == null) return;
        mIsSummaryMode = true;
        tvTitle.setText("学习总结");
        etInput.setEnabled(true);
        etInput.setHint("再问一题，或输入新问题开始新对话...");
        layoutStepActions.setVisibility(View.GONE);

        // Switch to summary report page
        llMessages.removeAllViews();
        svMessages.setVisibility(View.GONE);
        View summaryView = getView().findViewById(R.id.ll_summary);
        summaryView.setVisibility(View.VISIBLE);
        summaryView.findViewById(R.id.summary_header_card).setVisibility(View.VISIBLE);
        summaryView.findViewById(R.id.summary_streaming_container).setVisibility(View.GONE);

        // Fill header
        String q = summary.getQuestion();
        if (q != null && !q.isEmpty())
            ((TextView) summaryView.findViewById(R.id.summary_question)).setText(q);
        ((TextView) summaryView.findViewById(R.id.summary_total_time))
                .setText(summary.getTotalTime() != null ? summary.getTotalTime() : "—");
        String completed = summary.getCompletedAt();
        if (completed != null && completed.length() >= 16)
            completed = completed.substring(0, 16).replace("T", " ");
        ((TextView) summaryView.findViewById(R.id.summary_completed_at))
                .setText(completed != null ? completed : "—");

        // Mastery
        View masteredContainer = summaryView.findViewById(R.id.summary_mastered_container);
        View weakContainer = summaryView.findViewById(R.id.summary_weak_container);
        if (summary.getMasterySummary() != null) {
            var ms = summary.getMasterySummary();
            if (ms.getMasteredPoints() != null && !ms.getMasteredPoints().isEmpty()) {
                masteredContainer.setVisibility(View.VISIBLE);
                ((TextView) summaryView.findViewById(R.id.summary_mastered))
                        .setText("• " + String.join("\n• ", ms.getMasteredPoints()));
            }
            if (ms.getWeakPoints() != null && !ms.getWeakPoints().isEmpty()) {
                weakContainer.setVisibility(View.VISIBLE);
                ((TextView) summaryView.findViewById(R.id.summary_weak))
                        .setText("• " + String.join("\n• ", ms.getWeakPoints()));
            }
        }

        // Knowledge graph
        View knowledgeContainer = summaryView.findViewById(R.id.summary_knowledge_container);
        if (summary.getKnowledgeGraph() != null
                && summary.getKnowledgeGraph().getCoreKnowledgePoints() != null
                && !summary.getKnowledgeGraph().getCoreKnowledgePoints().isEmpty()) {
            knowledgeContainer.setVisibility(View.VISIBLE);
            ((TextView) summaryView.findViewById(R.id.summary_knowledge_points))
                    .setText("• " + String.join("\n• ", summary.getKnowledgeGraph().getCoreKnowledgePoints()));
        }

        // Recommendations
        View recContainer = summaryView.findViewById(R.id.summary_recommendations_container);
        if (summary.getRecommendations() != null && !summary.getRecommendations().isEmpty()) {
            recContainer.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            for (var rec : summary.getRecommendations()) {
                sb.append("• ").append(rec.getDescription()).append("\n");
            }
            ((TextView) summaryView.findViewById(R.id.summary_recommendations)).setText(sb.toString().trim());
        }

        scrollToBottom();
    }

    private void showHistory() {
        viewModel.loadHistory(1, 50);
    }

    private void showHistoryDialog(List<SessionHistoryItem> items) {
        if (!isAdded() || items == null || items.isEmpty()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(getContext()).inflate(R.layout.dialog_history_bottom_sheet, null);
        dialog.setContentView(sheet);

        RecyclerView rv = sheet.findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new HistoryAdapter(items, item -> {
            dialog.dismiss();
            if (item.getSessionId() != null) {
                mIsSummaryMode = false;
                mIsHistoryLoad = true;
                View summaryView = getView().findViewById(R.id.ll_summary);
                summaryView.setVisibility(View.GONE);
                svMessages.setVisibility(View.VISIBLE);
                llMessages.removeAllViews();
                tvTitle.setText("深度学习");
                viewModel.loadSessionDetail(item.getSessionId());
            }
        }));

        dialog.show();
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<SessionHistoryItem> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener { void onItemClick(SessionHistoryItem item); }

        HistoryAdapter(List<SessionHistoryItem> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            View statusDot;
            TextView tvTopic, tvTime;
            ViewHolder(View v) {
                super(v);
                statusDot = v.findViewById(R.id.v_status_dot);
                tvTopic = v.findViewById(R.id.tv_topic);
                tvTime = v.findViewById(R.id.tv_time);
            }
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_session, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder h, int i) {
            SessionHistoryItem item = items.get(i);
            boolean completed = "completed".equals(item.getStatus());
            h.statusDot.setBackgroundResource(completed ? R.drawable.round_bg_white : R.drawable.round_bg_white);
            h.statusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    completed ? 0xFF10B981 : 0xFF9CA3AF));
            h.tvTopic.setText(item.getTopic() != null ? item.getTopic() : "未命名");
            h.tvTime.setText(formatTime(item.getCreatedAt()));
            h.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        @Override public int getItemCount() { return items.size(); }

        private static String formatTime(String iso) {
            if (iso == null) return "";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date d = sdf.parse(iso);
                if (d == null) return "";
                SimpleDateFormat out = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
                return out.format(d);
            } catch (Exception e) {
                return iso.length() > 10 ? iso.substring(0, 10) : iso;
            }
        }
    }

    private void scrollToBottom() {
        svMessages.post(() -> svMessages.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
