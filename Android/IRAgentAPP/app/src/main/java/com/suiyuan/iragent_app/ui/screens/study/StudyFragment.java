package com.suiyuan.iragent_app.ui.screens.study;

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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.Conversation;
import com.suiyuan.iragent_app.data.model.Message;
import com.suiyuan.iragent_app.data.model.ResponseSegment;
import com.suiyuan.iragent_app.ui.geogebra.GeoGebraView;
import com.suiyuan.iragent_app.ui.screens.main.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 学习页面 Fragment
 * 负责处理数学公式渲染、函数图像绘制、流式响应显示等核心功能
 */
public class StudyFragment extends Fragment {

    /** ViewModel 实例，管理数据和业务逻辑 */
    private StudyViewModel viewModel;

    /** UI组件引用 */
    private LinearLayout llMessages;       // 消息列表容器
    private ScrollView svMessages;         // 消息滚动容器
    private EditText etInput;              // 输入框
    private ImageView ivSend;              // 发送按钮
    private ImageView ivMenu;              // 菜单按钮
    private ImageView ivNewConversation;   // 新建会话按钮
    private TextView tvDeepLearn;          // 深度学习入口
    private LinearLayout sidebar;          // 侧边栏
    private LinearLayout mainContent;      // 主内容区
    private RecyclerView recyclerView;     // 会话列表
    private TextView tvTitle;              // 标题栏
    private ImageView ivSettings;          // 设置按钮
    private View mask;                     // 遮罩层
    private ConversationAdapter adapter;   // 会话适配器

    // ========== 流式渲染核心变量 ==========
    private View mStreamingContainer;      // 流式消息容器
    private WebView mStreamingWebView;     // 流式渲染 WebView
    private List<String> mStreamingDesmosList; // 流式渲染过程中收集的图像表达式
    private String mStreamingPlot3DConfig; // 流式渲染过程中收集的3D配置
    private String mStreamingTimelineJson; // 流式渲染过程中收集的Timeline JSON

    private static final long RENDER_INTERVAL_MS = 200;  // 渲染间隔（防抖）
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRenderRunnable = this::doSafeRender;
    private String mCurrentFullText = "";        // 当前流式完整文本
    private String mLastRenderedText = "";       // 上一次渲染的文本
    private boolean mIsUserScrolling = false;    // 用户是否正在滚动
    private boolean mIsStreaming = false;        // 是否处于流式状态
    private boolean mWebViewReady = false;       // WebView 是否加载完成

    /** 数学模板HTML内容 */
    private String mMathTemplate;
    
    /**
     * 正则：匹配成对的 LaTeX 公式，避免截断
     * 支持四种格式：
     * - $$公式$$ : 行间公式
     * - $公式$   : 行内公式
     * - \(公式\) : 行内公式
     * - \[公式\] : 行间公式
     */
    private static final Pattern MATH_PATTERN = Pattern.compile(
            "\\$\\$[\\s\\S]*?\\$\\$|" +         // $$ 行间公式
            "\\$[\\s\\S]*?\\$|" +                 // $ 行内公式
            "\\\\\\([\\s\\S]*?\\\\\\)|" +         // \( 行内公式
            "\\\\\\[[\\s\\S]*?\\\\\\]"            // \[ 行间公式
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_study, container, false);

        initViews(view);
        viewModel = new ViewModelProvider(this).get(StudyViewModel.class);

        loadMathTemplate();       // 加载数学渲染模板
        setupRecyclerView();      // 设置会话列表
        setupListeners();         // 设置事件监听器
        setupObservers();         // 设置数据观察者
        setupScrollListener();    // 设置滚动监听器

        viewModel.loadConversations();  // 加载历史会话
        return view;
    }

    /**
     * 加载数学渲染模板 HTML
     * 模板包含 KaTeX、marked.js、DOMPurify 等依赖
     */
    private void loadMathTemplate() {
        try {
            InputStream is = requireContext().getAssets().open("math_template.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            mMathTemplate = sb.toString();
            reader.close();
            is.close();
        } catch (IOException e) {
            android.util.Log.e("StudyFragment", "加载数学模板失败", e);
            // 降级方案：使用最简模板
            mMathTemplate = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body><div id='content'></div></body></html>";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清理消息和任务
        mMainHandler.removeCallbacksAndMessages(null);
        mIsStreaming = false;
        mWebViewReady = false;
        
        // 释放流式 WebView
        if (mStreamingWebView != null) {
            mStreamingWebView.stopLoading();
            mStreamingWebView.destroy();
            mStreamingWebView = null;
        }
        
        // 释放所有 GeoGebraView
        if (llMessages != null) {
            for (int i = 0; i < llMessages.getChildCount(); i++) {
                android.view.View child = llMessages.getChildAt(i);
                if (child instanceof GeoGebraView) {
                    ((GeoGebraView) child).release();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 暂停 WebView
        if (mStreamingWebView != null) {
            mStreamingWebView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 恢复 WebView
        if (mStreamingWebView != null) {
            mStreamingWebView.onResume();
        }
    }

    /**
     * 初始化所有 UI 组件引用
     */
    private void initViews(View view) {
        llMessages = view.findViewById(R.id.ll_messages);
        svMessages = view.findViewById(R.id.sv_messages);
        etInput = view.findViewById(R.id.et_input);
        ivSend = view.findViewById(R.id.iv_send);
        ivMenu = view.findViewById(R.id.iv_menu);
        ivNewConversation = view.findViewById(R.id.iv_new_conversation);
        tvDeepLearn = view.findViewById(R.id.tv_deep_learn);
        tvTitle = view.findViewById(R.id.tv_title);
        sidebar = view.findViewById(R.id.sidebar);
        mainContent = view.findViewById(R.id.main_content);
        recyclerView = view.findViewById(R.id.recycler_view);
        ivSettings = view.findViewById(R.id.iv_more);
        mask = view.findViewById(R.id.mask);
    }

    /**
     * 设置会话列表 RecyclerView
     */
    private void setupRecyclerView() {
        adapter = new ConversationAdapter(
            conversation -> {
                viewModel.setCurrentConversation(conversation.getConversationId());
                tvTitle.setText(conversation.getName());
                toggleSidebar();
            },
            conversation -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("删除对话")
                    .setMessage("确定要删除「" + conversation.getName() + "」吗？")
                    .setPositiveButton("删除", (dialog, which) ->
                        viewModel.deleteConversation(conversation.getConversationId(), null))
                    .setNegativeButton("取消", null)
                    .show();
            }
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * 设置所有点击事件监听器
     */
    private void setupListeners() {
        ivSend.setOnClickListener(v -> sendMessage());
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
        ivMenu.setOnClickListener(v -> toggleSidebar());
        ivNewConversation.setOnClickListener(v -> {
            viewModel.startNewConversation();
            tvTitle.setText("新对话");
            toggleSidebar();
        });
        ivSettings.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToProfile();
            }
        });
        mask.setOnClickListener(v -> toggleSidebar());
        tvDeepLearn.setOnClickListener(v -> {
            toggleSidebar();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new com.suiyuan.iragent_app.ui.screens.deeplearn.DeepLearnFragment())
                    .addToBackStack("deep_learn")
                    .commit();
        });
    }

    /**
     * 设置滚动触摸监听器
     * 用于判断用户是否正在手动滚动，避免自动滚动干扰
     */
    private void setupScrollListener() {
        svMessages.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    mIsUserScrolling = true;
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    mIsUserScrolling = false;
                    break;
            }
            return false;
        });
    }

    /**
     * 切换侧边栏显示/隐藏
     */
    private void toggleSidebar() {
        if (sidebar.getVisibility() == View.GONE) {
            sidebar.setVisibility(View.VISIBLE);
            mask.setVisibility(View.VISIBLE);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int sidebarWidth = (int) (screenWidth * 0.8);
            sidebar.setLayoutParams(new LinearLayout.LayoutParams(sidebarWidth, LinearLayout.LayoutParams.MATCH_PARENT, 0));
            mask.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
            mainContent.setVisibility(View.GONE);
            viewModel.loadConversations();
        } else {
            sidebar.setVisibility(View.GONE);
            mask.setVisibility(View.GONE);
            sidebar.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0));
            mask.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0));
            mainContent.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 设置数据观察者
     * 监听 ViewModel 的 LiveData 变化
     */
    private void setupObservers() {
        // 监听分段响应（非流式）
        viewModel.getSegmentsLiveData().observe(getViewLifecycleOwner(), segments -> {
            if (segments != null && !segments.isEmpty()) addResponseSegments(segments);
        });

        // 监听错误
        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            finishStreamingMessage();
            // 检查是否是 401 错误（token过期）
            if (error != null && error.contains("401")) {
                addErrorMessage("登录已过期，请重新登录");
                // 清除本地 token 并跳转到登录页面
                new com.suiyuan.iragent_app.data.local.PreferencesManager(requireContext()).clearAll();
                com.suiyuan.iragent_app.data.remote.NetworkClient.setToken(null);
                startActivity(new android.content.Intent(getContext(), com.suiyuan.iragent_app.ui.screens.auth.AuthActivity.class));
                requireActivity().finish();
            } else {
                addErrorMessage("连接中断：" + error);
            }
        });

        // 监听加载状态
        viewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            ivSend.setEnabled(!isLoading);
            etInput.setEnabled(!isLoading);
        });

        // 监听会话列表
        viewModel.getConversationsLiveData().observe(getViewLifecycleOwner(), adapter::setConversations);
        
        // 监听历史消息
        viewModel.getHistoryMessagesLiveData().observe(getViewLifecycleOwner(), this::loadHistoryMessages);

        // ========== 流式响应核心观察者 ==========
        viewModel.getStreamStartLiveData().observe(getViewLifecycleOwner(), started -> {
            if (started != null && started) startStreamingMessage();
        });

        viewModel.getStreamTextLiveData().observe(getViewLifecycleOwner(), this::appendStreamingText);
        viewModel.getStreamDesmosLiveData().observe(getViewLifecycleOwner(), this::addStreamingDesmos);
        viewModel.getStreamPlot3DLiveData().observe(getViewLifecycleOwner(), config -> {
            if (config != null && mIsStreaming) {
                mStreamingPlot3DConfig = config;
            }
        });
        viewModel.getStreamTimelineLiveData().observe(getViewLifecycleOwner(), json -> {
            if (json != null && mIsStreaming) {
                mStreamingTimelineJson = json;
            }
        });
        viewModel.getStreamDoneLiveData().observe(getViewLifecycleOwner(), done -> {
            if (done != null && done) finishStreamingMessage();
        });
    }

    /**
     * 发送消息
     */
    private void sendMessage() {
        String message = etInput.getText().toString().trim();
        if (message.isEmpty()) return;
        addUserMessage(message);
        etInput.setText("");
        viewModel.solveStream(message);
    }

    /**
     * 添加用户消息
     */
    private void addUserMessage(String text) {
        View messageView = LayoutInflater.from(getContext()).inflate(
                R.layout.item_message_user, llMessages, false
        );
        TextView tvContent = messageView.findViewById(R.id.tv_content);
        tvContent.setText(text);
        llMessages.addView(messageView);
        scrollToBottom();
    }

    /**
     * 添加响应分段（非流式）
     * @param segments 响应分段列表
     */
    private void addResponseSegments(List<ResponseSegment> segments) {
        View messageContainer = LayoutInflater.from(getContext()).inflate(
                R.layout.item_message_ai_container, llMessages, false
        );
        LinearLayout contentLayout = messageContainer.findViewById(R.id.ll_content);
        List<String> desmosList = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder();

        // 解析所有分段
        for (ResponseSegment seg : segments) {
            if (seg.isText() || seg.isPlot()) {
                textBuilder.append(seg.getContent());
            }
            if (seg.isGeogebra() && seg.getExpression() != null) {
                addUniqueExpression(desmosList, seg.getExpression());
            }
        }

        // 添加文本内容（含公式渲染）
        if (textBuilder.length() > 0) {
            String processedText = removeUnclosedMarkdownAndMath(textBuilder.toString());
            WebView webView = createMathWebView();
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    // 页面加载完成后再渲染内容
                    String escapedContent = escapeJsString(processedText);
                    view.evaluateJavascript("renderMathContent('" + escapedContent + "')", null);
                }
            });
            webView.loadDataWithBaseURL("https://cdn.jsdelivr.net", mMathTemplate, "text/html", "UTF-8", null);
            contentLayout.addView(webView);
        }

        // 添加图像
        if (!desmosList.isEmpty()) addGeoGebraContainer(contentLayout, desmosList);
        llMessages.addView(messageContainer);
        scrollToBottom();
    }

    /**
     * 添加错误消息
     */
    private void addErrorMessage(String error) {
        View container = LayoutInflater.from(getContext()).inflate(
                R.layout.item_message_ai_container, llMessages, false
        );
        LinearLayout content = container.findViewById(R.id.ll_content);
        TextView tv = new TextView(getContext());
        tv.setText(error);
        tv.setTextColor(Color.RED);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setPadding(0, 8, 0, 8);
        content.addView(tv);
        llMessages.addView(container);
        scrollToBottom();
    }

    /**
     * 加载历史消息
     */
    private void loadHistoryMessages(List<Message> messages) {
        llMessages.removeAllViews();
        for (Message msg : messages) {
            if ("user".equals(msg.getSenderType())) {
                addHistoryUserMessage(msg.getContent());
            } else {
                addHistoryAiMessage(msg.getContent());
            }
        }
        scrollToBottom();
    }

    /**
     * 添加历史用户消息
     */
    private void addHistoryUserMessage(String text) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.item_message_user, llMessages, false);
        ((TextView) v.findViewById(R.id.tv_content)).setText(text);
        llMessages.addView(v);
    }

    /**
     * 添加历史 AI 消息
     * @param text 消息内容（可能包含 Markdown 和 LaTeX）
     */
    private void addHistoryAiMessage(String text) {
        View container = LayoutInflater.from(getContext()).inflate(
                R.layout.item_message_ai_container, llMessages, false
        );
        LinearLayout contentLayout = container.findViewById(R.id.ll_content);
        List<ResponseSegment> segments = parseMessageToSegments(text);
        List<String> desmosList = new ArrayList<>();
        String plot3DConfig = null;
        StringBuilder textBuilder = new StringBuilder();

        // 解析分段
        for (ResponseSegment seg : segments) {
            if (seg.isText()) {
                textBuilder.append(seg.getContent());
            }
            if (seg.isGeogebra() && seg.getExpression() != null) {
                addUniqueExpression(desmosList, seg.getExpression());
            }
            if (seg.isPlot3D() && seg.getPlot3dConfig() != null) {
                plot3DConfig = seg.getPlot3dConfig();
            }
        }

        // 添加文本内容
        if (textBuilder.length() > 0) {
            WebView webView = createMathWebView();
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    String escapedContent = escapeJsString(textBuilder.toString());
                    view.evaluateJavascript("renderMathContent('" + escapedContent + "')", null);
                }
            });
            webView.loadDataWithBaseURL("https://cdn.jsdelivr.net", mMathTemplate, "text/html", "UTF-8", null);
            contentLayout.addView(webView);
        }

        // 添加 2D 图像
        if (!desmosList.isEmpty()) addGeoGebraContainer(contentLayout, desmosList);
        // 添加 3D 图像
        if (plot3DConfig != null) addPlot3DContainer(contentLayout, plot3DConfig);
        llMessages.addView(container);
    }

    // ========== 流式响应核心方法 ==========

    /**
     * 启动流式消息渲染
     */
    private void startStreamingMessage() {
        mIsStreaming = true;
        mCurrentFullText = "";
        mLastRenderedText = "";
        mWebViewReady = false;
        mStreamingDesmosList = new ArrayList<>();
        mStreamingPlot3DConfig = null;
        mStreamingTimelineJson = null;
        mMainHandler.removeCallbacksAndMessages(null);

        // 初始化流式容器
        mStreamingContainer = LayoutInflater.from(getContext()).inflate(
                R.layout.item_message_ai_container, llMessages, false
        );
        LinearLayout contentLayout = mStreamingContainer.findViewById(R.id.ll_content);

        // 创建 WebView，等待页面加载完成再渲染
        mStreamingWebView = createMathWebView();
        mStreamingWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mWebViewReady = true;
                // 页面加载完成后，立即渲染已有内容
                doSafeRender();
            }
        });
        // 先加载模板，再后续渲染内容
        mStreamingWebView.loadDataWithBaseURL("https://cdn.jsdelivr.net", mMathTemplate, "text/html", "UTF-8", null);
        contentLayout.addView(mStreamingWebView);
        llMessages.addView(mStreamingContainer);
        scrollToBottom();
    }

    /**
     * 追加流式文本
     * @param fullText 当前完整的流式文本
     */
    private void appendStreamingText(String fullText) {
        if (mStreamingWebView == null || fullText == null || !mIsStreaming) return;

        mCurrentFullText = fullText;
        // 防抖：移除之前的渲染任务，重新调度
        mMainHandler.removeCallbacks(mRenderRunnable);
        mMainHandler.postDelayed(mRenderRunnable, RENDER_INTERVAL_MS);
    }

    /**
     * 安全渲染方法（核心修复）
     * 处理未闭合的 Markdown 和 LaTeX 语法，避免渲染失败
     */
    private void doSafeRender() {
        // 检查前置条件
        if (mStreamingWebView == null || !mIsStreaming || getContext() == null || !mWebViewReady) {
            return;
        }

        String textToRender = mCurrentFullText;
        // 避免重复渲染
        if (textToRender.equals(mLastRenderedText)) {
            return;
        }

        // 核心修复：移除末尾未闭合的 Markdown + LaTeX 语法
        textToRender = removeUnclosedMarkdownAndMath(textToRender);

        // 转义特殊字符，避免 JS 语法错误
        String escapedContent = escapeJsString(textToRender);
        // 调用 JS 方法渲染内容（先转 MD，再渲染公式）
        mStreamingWebView.evaluateJavascript("renderMathContent('" + escapedContent + "')", null);

        mLastRenderedText = textToRender;
        scrollToBottom();
    }

    /**
     * JS 字符串转义
     * 将特殊字符转义为 JS 字符串中合法的形式
     * 注意：不转义反斜杠，保留 LaTeX 公式中的命令（如 \tan, \pi）
     */
    private String escapeJsString(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\u2028':
                    sb.append("\\u2028");
                    break;
                case '\u2029':
                    sb.append("\\u2029");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * 移除末尾未闭合的 Markdown + LaTeX 语法
     * 防止流式渲染过程中因未完成的语法导致渲染失败
     * @param text 原始文本
     * @return 处理后的文本
     */
    private String removeUnclosedMarkdownAndMath(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String processed = text;

        // 1. 处理 LaTeX 公式定界符
        // 统计文本中所有 $ 的数量（包括公式内的）
        int dollarCount = 0;
        for (char c : processed.toCharArray()) {
            if (c == '$') dollarCount++;
        }
        // 如果 $ 数量是奇数，移除最后一个未闭合的 $
        if (dollarCount % 2 != 0) {
            int lastDollar = processed.lastIndexOf('$');
            if (lastDollar != -1) {
                processed = processed.substring(0, lastDollar);
            }
        }

        // 2. 处理 Markdown 加粗语法 **（只移除末尾未配对的）
        int boldCount = 0;
        int lastBoldIndex = -1;
        Matcher boldMatcher = Pattern.compile("\\*\\*").matcher(processed);
        while (boldMatcher.find()) {
            boldCount++;
            lastBoldIndex = boldMatcher.start();
        }
        if (boldCount % 2 != 0 && lastBoldIndex != -1) {
            processed = processed.substring(0, lastBoldIndex);
        }

        // 3. 处理 Markdown 斜体 *（排除转义的 * 和 ** 中的 *）
        int italicCount = 0;
        int lastItalicIndex = -1;
        Matcher italicMatcher = Pattern.compile("(?<!\\\\)\\*(?!\\*)").matcher(processed);
        while (italicMatcher.find()) {
            italicCount++;
            lastItalicIndex = italicMatcher.start();
        }
        if (italicCount % 2 != 0 && lastItalicIndex != -1) {
            processed = processed.substring(0, lastItalicIndex);
        }

        // 4. 处理 Markdown 行内代码 `
        int codeCount = 0;
        int lastCodeIndex = -1;
        Matcher codeMatcher = Pattern.compile("`").matcher(processed);
        while (codeMatcher.find()) {
            codeCount++;
            lastCodeIndex = codeMatcher.start();
        }
        if (codeCount % 2 != 0 && lastCodeIndex != -1) {
            processed = processed.substring(0, lastCodeIndex);
        }

        // 5. 处理 Markdown 代码块 ```
        int blockCodeCount = 0;
        int lastBlockCodeIndex = -1;
        Matcher blockCodeMatcher = Pattern.compile("```").matcher(processed);
        while (blockCodeMatcher.find()) {
            blockCodeCount++;
            lastBlockCodeIndex = blockCodeMatcher.start();
        }
        if (blockCodeCount % 2 != 0 && lastBlockCodeIndex != -1) {
            processed = processed.substring(0, lastBlockCodeIndex);
        }

        return processed;
    }

    /**
     * 添加流式图像表达式
     */
    private void addStreamingDesmos(String expr) {
        addUniqueExpression(mStreamingDesmosList, expr);
    }

    /**
     * 添加不重复的表达式
     */
    private void addUniqueExpression(List<String> expressions, String expr) {
        if (expressions == null || expr == null) return;

        String trimmedExpr = expr.trim();
        if (!trimmedExpr.isEmpty() && !expressions.contains(trimmedExpr)) {
            expressions.add(trimmedExpr);
        }
    }

    /**
     * 流式结束，强制完整渲染
     */
    private void finishStreamingMessage() {
        mIsStreaming = false;
        if (mStreamingContainer == null) return;

        mMainHandler.removeCallbacksAndMessages(null);

        // 解析完整文本中的 GeoGebra/PLOT 表达式
        extractGeogebraFromText(mCurrentFullText);

        // 最后一次强制渲染完整内容（移除 GeoGebra 标签后的文本）
        if (mWebViewReady && mStreamingWebView != null) {
            String textWithoutGeogebra = removeGeogebraTags(mCurrentFullText);
            String escapedContent = escapeJsString(textWithoutGeogebra);
            mStreamingWebView.evaluateJavascript("renderMathContent('" + escapedContent + "')", null);
        }

        // 打印完整内容（调试用）
        android.util.Log.d("StudyFragment", "==================== 完整回答内容 ====================");
        android.util.Log.d("StudyFragment", mCurrentFullText);
        android.util.Log.d("StudyFragment", "=====================================================");

        // 添加图像
        LinearLayout contentLayout = mStreamingContainer.findViewById(R.id.ll_content);
        if (mStreamingDesmosList != null && !mStreamingDesmosList.isEmpty()) {
            addGeoGebraContainer(contentLayout, mStreamingDesmosList);
        }

        // 添加3D图像
        if (mStreamingPlot3DConfig != null && !mStreamingPlot3DConfig.isEmpty()) {
            addPlot3DContainer(contentLayout, mStreamingPlot3DConfig);
        }

        // 显示时间轴动画
        if (mStreamingTimelineJson != null && !mStreamingTimelineJson.isEmpty()) {
            startTimelineDialog(mStreamingTimelineJson);
        }

        // 重置变量
        mStreamingContainer = null;
        mStreamingWebView = null;
        mCurrentFullText = "";
        mLastRenderedText = "";
        mStreamingDesmosList = null;
        mStreamingPlot3DConfig = null;
        mStreamingTimelineJson = null;
        mWebViewReady = false;

        scrollToBottom();
    }

    /**
     * 从文本中提取 GeoGebra/PLOT 表达式
     * 支持 【GEGEBRA】 和 【PLOT】 两种标签格式
     */
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

        // 提取3D配置
        extractPlot3DFromText(text);
    }

    /**
     * 从文本中提取【PLOT3D】配置
     * @param text 完整文本
     */
    private void extractPlot3DFromText(String text) {
        if (text == null || text.isEmpty()) return;

        Pattern pattern = Pattern.compile("【PLOT3D】([\\s\\S]*?)(【END】|$)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String configBlock = matcher.group(1).trim();
            mStreamingPlot3DConfig = configBlock;
        }
    }

    /**
     * 移除文本中的 GeoGebra/PLOT 标签
     * @param text 原始文本
     * @return 移除标签后的文本
     */
    private String removeGeogebraTags(String text) {
        if (text == null) return "";
        text = text.replaceAll("【(?:GEGEBRA|PLOT)】[\\s\\S]*?(【END】|$)", "");
        text = text.replaceAll("【PLOT3D】[\\s\\S]*?(【END】|$)", "");
        return text.trim();
    }

    /**
     * 创建用于渲染数学公式的 WebView
     * 配置 JS 桥接、自适应高度等
     * @return 配置好的 WebView
     */
    private WebView createMathWebView() {
        WebView webView = new WebView(requireContext());
        WebSettings settings = webView.getSettings();
        
        // 启用 JavaScript
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        
        // 自适应缩放
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        
        // 缓存设置
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // 安全设置：禁止文件访问
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        
        // 硬件加速（部分机型需要关闭）
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // 关键：高度自适应设置
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        webView.setBackgroundColor(Color.TRANSPARENT);
        
        // 禁用滚动，让外层 ScrollView 处理滚动
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // JS 桥接，用于高度自适应回调
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onRenderComplete() {
                mMainHandler.post(() -> {
                    if (mStreamingWebView != null) {
                        // 强制重新测量高度
                        mStreamingWebView.measure(
                                View.MeasureSpec.makeMeasureSpec(svMessages.getWidth(), View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        );
                        mStreamingWebView.requestLayout();
                        scrollToBottom();
                    }
                });
            }
        }, "androidBridge");

        return webView;
    }

    /**
     * 添加函数图像渲染容器
     * 使用 Canvas 绘制教科书风格的函数图像
     * @param contentLayout 父容器
     * @param expressions 表达式列表
     */
    private void addGeoGebraContainer(LinearLayout contentLayout, List<String> expressions) {
        // 1. 创建容器并设置固定高度（解决塌陷问题）
        FrameLayout container = new FrameLayout(getContext());
        int heightPx = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 260, getResources().getDisplayMetrics());
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
        containerParams.setMargins(0, 12, 0, 12);
        container.setLayoutParams(containerParams);
        container.setBackgroundColor(Color.WHITE);

        // 2. 创建用于显示结果的原生 ImageView
        ImageView staticImageView = new ImageView(getContext());
        staticImageView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        staticImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        staticImageView.setBackgroundColor(Color.WHITE);
        container.addView(staticImageView);

        // 3. 创建错误提示 View
        TextView errorView = new TextView(getContext());
        errorView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        errorView.setGravity(Gravity.CENTER);
        errorView.setText("图像生成失败");
        errorView.setTextColor(Color.GRAY);
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        errorView.setVisibility(View.GONE);

        // 4. 创建加载动画 View
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

        // 5. 创建后台渲染 WebView（透明，仅用于渲染）
        WebView renderWebView = new WebView(getContext());
        renderWebView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        renderWebView.setAlpha(0.01f);  // 几乎透明
        renderWebView.setBackgroundColor(Color.WHITE);
        renderWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        renderWebView.setVerticalScrollBarEnabled(false);
        renderWebView.setHorizontalScrollBarEnabled(false);
        renderWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // 配置 WebSettings
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

        // 5. 注册 JS 桥接对象
        renderWebView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onGeoGebraReady() {
                android.util.Log.d("StudyFragment", "GeoGebra renderer ready");
            }

            @android.webkit.JavascriptInterface
            public void onImageCaptured(String base64) {
                mMainHandler.post(() -> {
                    try {
                        if (base64 == null || base64.trim().isEmpty()) {
                            loadingView.setVisibility(View.GONE);
                            errorView.setVisibility(View.VISIBLE);
                            android.util.Log.e("StudyFragment", "GeoGebra image capture returned empty base64");
                            mMainHandler.postDelayed(() -> {
                                if (renderWebView.getParent() == container) {
                                    container.removeView(renderWebView);
                                }
                                renderWebView.destroy();
                            }, 500);
                            return;
                        }

                        // 关键点：剔除 Base64 前缀（如 data:image/png;base64,）
                        String pureBase64 = base64;
                        if (base64.contains(",")) {
                            pureBase64 = base64.substring(base64.indexOf(",") + 1);
                        }

                        // 解码并显示
                        byte[] decodedString = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT);
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                        if (bitmap != null) {
                            loadingView.setVisibility(View.GONE);
                            staticImageView.setImageBitmap(bitmap);
                            staticImageView.setBackgroundColor(android.graphics.Color.WHITE);
                            errorView.setVisibility(View.GONE);
                        } else {
                            loadingView.setVisibility(View.GONE);
                            errorView.setVisibility(View.VISIBLE);
                            android.util.Log.e("StudyFragment", "Bitmap decoding failed");
                        }

                        // 6. 渲染完成后销毁 WebView（释放资源）
                        mMainHandler.postDelayed(() -> {
                            if (renderWebView.getParent() == container) {
                                container.removeView(renderWebView);
                            }
                            renderWebView.destroy();
                        }, 500);

                    } catch (Exception e) {
                        loadingView.setVisibility(View.GONE);
                        errorView.setVisibility(View.VISIBLE);
                        android.util.Log.e("StudyFragment", "Image process error: " + e.getMessage());
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

        // 7. 配置加载逻辑
        renderWebView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                view.postDelayed(() -> {
                    String jsonExpr = new com.google.gson.Gson().toJson(expressions);
                    view.evaluateJavascript("setExpressions(" + jsonExpr + ")", null);
                }, 200);
            }
        });

        container.addView(renderWebView);
        contentLayout.addView(container);
        
        // 加载本地 HTML 文件
        renderWebView.loadUrl("file:///android_asset/geogebra/index.html");
    }

    /**
     * 添加3D图像容器
     * @param contentLayout 父布局
     * @param plot3DConfig 3D配置文本
     */
    private void addPlot3DContainer(LinearLayout contentLayout, String plot3DConfig) {
        // 1. 创建容器并设置固定高度
        FrameLayout container = new FrameLayout(getContext());
        int heightPx = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
        containerParams.setMargins(0, 12, 0, 12);
        container.setLayoutParams(containerParams);
        container.setBackgroundColor(Color.WHITE);

        // 2. 创建用于显示结果的原生 ImageView
        ImageView staticImageView = new ImageView(getContext());
        staticImageView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        staticImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        staticImageView.setBackgroundColor(Color.WHITE);
        container.addView(staticImageView);

        // 3. 创建错误提示 View
        TextView errorView = new TextView(getContext());
        errorView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        errorView.setGravity(Gravity.CENTER);
        errorView.setText("3D图像生成失败");
        errorView.setTextColor(Color.GRAY);
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        errorView.setVisibility(View.GONE);
        container.addView(errorView);

        // 4. 创建加载动画 View
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

        // 5. 创建3D渲染 WebView
        WebView renderWebView = new WebView(getContext());
        renderWebView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        renderWebView.setBackgroundColor(Color.WHITE);
        renderWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        renderWebView.setVerticalScrollBarEnabled(false);
        renderWebView.setHorizontalScrollBarEnabled(false);
        renderWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // 配置 WebSettings
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

        // 6. 注册 JS 桥接对象
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

                        // 销毁 WebView 释放资源
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

        // 7. 配置加载逻辑
        renderWebView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
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

        // 加载本地 HTML 文件
        renderWebView.loadUrl("file:///android_asset/geogebra/3d_renderer.html");
    }

    private void startTimelineDialog(String timelineJson) {
        android.util.Log.d("StudyFragment", "显示时间轴动画, JSON长度=" + timelineJson.length());

        android.webkit.WebView webView = new android.webkit.WebView(getContext());
        android.webkit.WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE);

        webView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                view.postDelayed(() -> {
                    view.evaluateJavascript("unifiedRender('timeline', " + timelineJson + ")", null);
                }, 200);
            }
        });

        android.app.Dialog dialog = new android.app.Dialog(getContext());
        dialog.setContentView(webView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.85)
            );
        }

        dialog.setOnDismissListener(d -> {
            webView.evaluateJavascript("unifiedRender('stop', '')", null);
            webView.destroy();
        });

        dialog.show();
        webView.loadUrl("file:///android_asset/engine/renderer.html");
    }

    /**
     * 解析3D配置文本为JSON对象
     * @param configText 配置文本
     * @return 配置对象
     */
    private java.util.Map<String, Object> parsePlot3DConfig(String configText) {
        java.util.Map<String, Object> config = new java.util.HashMap<>();

        String[] lines = configText.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("type:")) {
                config.put("type", trimmed.substring(5).trim());
            } else if (trimmed.startsWith("vectors:")) {
                config.put("vectors", parseVectors(trimmed.substring(7).trim()));
            } else if (trimmed.startsWith("points:")) {
                config.put("points", parse3DPoints(trimmed.substring(7).trim()));
            } else if (trimmed.startsWith("planes:")) {
                config.put("planes", parsePlanes(trimmed.substring(6).trim()));
            } else if (trimmed.startsWith("boxes:")) {
                config.put("boxes", parseBoxes(trimmed.substring(5).trim()));
            } else if (trimmed.startsWith("spheres:")) {
                config.put("spheres", parseSpheres(trimmed.substring(7).trim()));
            } else if (trimmed.startsWith("lines:")) {
                config.put("lines", parseLines(trimmed.substring(5).trim()));
            }
        }

        return config;
    }

    private java.util.List<java.util.Map<String, Object>> parseVectors(String text) {
        java.util.List<java.util.Map<String, Object>> vectors = new java.util.ArrayList<>();
        String[] parts = text.split(",");
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;

            java.util.Map<String, Object> v = new java.util.HashMap<>();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "<([\\w+]+)\\s*\\(\\s*([^,]+)\\s*,\\s*([^,]+)\\s*,\\s*([^)]+)\\s*\\)>");
            java.util.regex.Matcher matcher = pattern.matcher(p);
            if (matcher.find()) {
                v.put("name", matcher.group(1));
                v.put("x", Double.parseDouble(matcher.group(2).trim()));
                v.put("y", Double.parseDouble(matcher.group(3).trim()));
                v.put("z", Double.parseDouble(matcher.group(4).trim()));
                vectors.add(v);
            }
        }
        return vectors;
    }

    private java.util.List<java.util.Map<String, Object>> parse3DPoints(String text) {
        java.util.List<java.util.Map<String, Object>> points = new java.util.ArrayList<>();
        String[] parts = text.split(",");
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;

            java.util.Map<String, Object> pt = new java.util.HashMap<>();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "([A-Za-z])\\s*\\(\\s*([^,]+)\\s*,\\s*([^,]+)\\s*,\\s*([^)]+)\\s*\\)");
            java.util.regex.Matcher matcher = pattern.matcher(p);
            if (matcher.find()) {
                pt.put("name", matcher.group(1));
                pt.put("x", Double.parseDouble(matcher.group(2).trim()));
                pt.put("y", Double.parseDouble(matcher.group(3).trim()));
                pt.put("z", Double.parseDouble(matcher.group(4).trim()));
                points.add(pt);
            }
        }
        return points;
    }

    private java.util.List<java.util.Map<String, Object>> parsePlanes(String text) {
        java.util.List<java.util.Map<String, Object>> planes = new java.util.ArrayList<>();
        String[] parts = text.split(",");
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;

            java.util.Map<String, Object> plane = new java.util.HashMap<>();
            plane.put("expr", p);
            planes.add(plane);
        }
        return planes;
    }

    private java.util.List<java.util.Map<String, Object>> parseBoxes(String text) {
        java.util.List<java.util.Map<String, Object>> boxes = new java.util.ArrayList<>();
        String[] parts = text.split(",");
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;

            java.util.Map<String, Object> box = new java.util.HashMap<>();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "<([^>]+)>");
            java.util.regex.Matcher matcher = pattern.matcher(p);
            if (matcher.find()) {
                box.put("name", matcher.group(1));
            }
            boxes.add(box);
        }
        return boxes;
    }

    private java.util.List<java.util.Map<String, Object>> parseSpheres(String text) {
        java.util.List<java.util.Map<String, Object>> spheres = new java.util.ArrayList<>();
        String[] parts = text.split(",");
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;

            java.util.Map<String, Object> sphere = new java.util.HashMap<>();
            sphere.put("expr", p);
            spheres.add(sphere);
        }
        return spheres;
    }

    private java.util.List<java.util.Map<String, Object>> parseLines(String text) {
        java.util.List<java.util.Map<String, Object>> lines = new java.util.ArrayList<>();
        String[] parts = text.split(",");
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;

            java.util.Map<String, Object> line = new java.util.HashMap<>();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "([A-Za-z])\\s*\\(\\s*([^,]+)\\s*,\\s*([^,]+)\\s*,\\s*([^)]+)\\s*\\)\\s*->\\s*([A-Za-z])\\s*\\(\\s*([^,]+)\\s*,\\s*([^,]+)\\s*,\\s*([^)]+)\\s*\\)");
            java.util.regex.Matcher matcher = pattern.matcher(p);
            if (matcher.find()) {
                java.util.Map<String, Object> p1 = new java.util.HashMap<>();
                p1.put("x", Double.parseDouble(matcher.group(2).trim()));
                p1.put("y", Double.parseDouble(matcher.group(3).trim()));
                p1.put("z", Double.parseDouble(matcher.group(4).trim()));
                
                java.util.Map<String, Object> p2 = new java.util.HashMap<>();
                p2.put("x", Double.parseDouble(matcher.group(6).trim()));
                p2.put("y", Double.parseDouble(matcher.group(7).trim()));
                p2.put("z", Double.parseDouble(matcher.group(8).trim()));
                
                line.put("p1", p1);
                line.put("p2", p2);
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * 解析消息文本为分段
     * 支持 【GEGEBRA】 和 【PLOT】 两种标签
     * @param text 原始消息文本
     * @return 分段列表
     */
    private List<ResponseSegment> parseMessageToSegments(String text) {
        List<ResponseSegment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) return segments;
        List<String> parsedExpressions = new ArrayList<>();

        // 清理文本
        String cleaned = text.replace("智能解答:", "").replace("[流式响应]", "").trim();
        cleaned = cleaned.replace("\\n", "\n").replace("\\'", "'");

        // 先提取 PLOT3D 配置，并从文本中移除
        Pattern plot3dPattern = Pattern.compile("【PLOT3D】([\\s\\S]*?)(【END】|$)");
        Matcher plot3dMatcher = plot3dPattern.matcher(cleaned);
        if (plot3dMatcher.find()) {
            String configBlock = plot3dMatcher.group(1).trim();
            if (!configBlock.isEmpty()) {
                ResponseSegment s = new ResponseSegment();
                s.setType("plot3d");
                s.setPlot3dConfig(configBlock);
                segments.add(s);
            }
            cleaned = cleaned.replaceAll("【PLOT3D】[\\s\\S]*?(【END】|$)", "").trim();
        }

        // 匹配 GeoGebra/PLOT 标签
        Pattern pattern = Pattern.compile("【(?:GEGEBRA|PLOT)】([\\s\\S]*?)(【END】|$)");
        Matcher matcher = pattern.matcher(cleaned);
        int last = 0;

        while (matcher.find()) {
            // 添加标签前的文本
            String txt = cleaned.substring(last, matcher.start()).trim();
            if (!txt.isEmpty()) {
                ResponseSegment s = new ResponseSegment();
                s.setType("text");
                s.setContent(txt);
                segments.add(s);
            }
            
            // 添加图像表达式
            String exprBlock = matcher.group(1).trim();
            if (!exprBlock.isEmpty()) {
                String[] exprs = exprBlock.split("\\r?\\n");
                for (String expr : exprs) {
                    String trimmedExpr = expr.trim();
                    if (!trimmedExpr.isEmpty() && !parsedExpressions.contains(trimmedExpr)) {
                        parsedExpressions.add(trimmedExpr);
                        ResponseSegment s = new ResponseSegment();
                        s.setType("geogebra");
                        s.setExpression(trimmedExpr);
                        segments.add(s);
                    }
                }
            }
            last = matcher.end();
        }

        // 添加剩余文本
        if (last < cleaned.length()) {
            String txt = cleaned.substring(last).trim();
            if (!txt.isEmpty()) {
                ResponseSegment s = new ResponseSegment();
                s.setType("text");
                s.setContent(txt);
                segments.add(s);
            }
        }
        return segments;
    }

    /**
     * 滚动到底部（非用户滚动时）
     */
    private void scrollToBottom() {
        if (!mIsUserScrolling) {
            svMessages.post(() -> svMessages.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    /**
     * 会话列表适配器
     */
    private static class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {
        private List<Conversation> conversations;
        private final OnConversationClickListener clickListener;
        private final OnConversationLongClickListener longClickListener;

        public interface OnConversationClickListener {
            void onConversationClick(Conversation conversation);
        }

        public interface OnConversationLongClickListener {
            void onConversationLongClick(Conversation conversation);
        }

        public ConversationAdapter(OnConversationClickListener clickListener, OnConversationLongClickListener longClickListener) {
            this.clickListener = clickListener;
            this.longClickListener = longClickListener;
        }

        public void setConversations(List<Conversation> conversations) {
            this.conversations = conversations;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_conversation, parent, false);
            return new ConversationViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
            Conversation c = conversations.get(position);
            holder.title.setText(c.getName());
            holder.preview.setText(c.getDescription());
            holder.time.setText(formatTime(c.getUpdatedAt()));
            holder.itemView.setOnClickListener(v -> clickListener.onConversationClick(c));
            holder.itemView.setOnLongClickListener(v -> {
                longClickListener.onConversationLongClick(c);
                return true;
            });
        }
        
        private String formatTime(String timestamp) {
            if (timestamp == null || timestamp.isEmpty()) {
                return "";
            }
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                java.util.Date date = inputFormat.parse(timestamp);
                if (date != null) {
                    java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault());
                    return outputFormat.format(date);
                }
            } catch (Exception e) {
                android.util.Log.e("ConversationAdapter", "formatTime error", e);
            }
            return timestamp;
        }

        @Override
        public int getItemCount() {
            return conversations == null ? 0 : conversations.size();
        }

        static class ConversationViewHolder extends RecyclerView.ViewHolder {
            TextView title, preview, time;
            public ConversationViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tv_name);
                preview = itemView.findViewById(R.id.tv_description);
                time = itemView.findViewById(R.id.tv_time);
            }
        }
    }
}
