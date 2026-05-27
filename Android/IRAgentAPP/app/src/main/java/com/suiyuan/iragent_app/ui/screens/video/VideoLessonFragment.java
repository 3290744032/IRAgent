package com.suiyuan.iragent_app.ui.screens.video;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.ui.screens.deeplearn.DeepLearnViewModel;
import com.suiyuan.iragent_app.util.TtsHttpClient;

public class VideoLessonFragment extends Fragment {

    private DeepLearnViewModel viewModel;
    private View inputState, loadingState;
    private EditText etTopic;
    private TextView tvGenerate;
    private TextView quickDerivative, quickMaxmin, quickGrammar;
    private boolean mAutoStartTimeline = false;
    private String mPendingTopic = null;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_lesson, container, false);

        view.findViewById(R.id.iv_back).setOnClickListener(v -> requireActivity().onBackPressed());

        inputState = view.findViewById(R.id.layout_input);
        loadingState = view.findViewById(R.id.layout_loading);
        etTopic = view.findViewById(R.id.et_video_topic);
        tvGenerate = view.findViewById(R.id.tv_generate);
        quickDerivative = view.findViewById(R.id.quick_derivative);
        quickMaxmin = view.findViewById(R.id.quick_maxmin);
        quickGrammar = view.findViewById(R.id.quick_grammar);

        viewModel = new ViewModelProvider(this).get(DeepLearnViewModel.class);

        Bundle args = getArguments();
        if (args != null) {
            mAutoStartTimeline = args.getBoolean("auto_timeline", false);
            mPendingTopic = args.getString("topic", null);
        }

        setupListeners();
        setupObservers();

        if (mPendingTopic != null && !mPendingTopic.isEmpty()) {
            startVideoGeneration(mPendingTopic);
        }

        return view;
    }

    private void setupListeners() {
        tvGenerate.setOnClickListener(v -> startVideoFromInput());
        etTopic.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                startVideoFromInput();
                return true;
            }
            return false;
        });

        View.OnClickListener quickListener = v -> {
            String topic = ((TextView) v).getText().toString();
            etTopic.setText(topic);
            startVideoGeneration(topic);
        };
        quickDerivative.setOnClickListener(quickListener);
        quickMaxmin.setOnClickListener(quickListener);
        quickGrammar.setOnClickListener(quickListener);
    }

    private void startVideoFromInput() {
        String topic = etTopic.getText().toString().trim();
        if (topic.isEmpty()) {
            etTopic.setError("请输入知识点");
            etTopic.requestFocus();
            return;
        }
        startVideoGeneration(topic);
    }

    private void startVideoGeneration(String topic) {
        inputState.setVisibility(View.GONE);
        loadingState.setVisibility(View.VISIBLE);
        mPendingTopic = null;
        viewModel.generateTimelineSync(topic);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAutoStartTimeline) {
            mAutoStartTimeline = false;
            viewModel.getTimelineLiveData().observe(getViewLifecycleOwner(), timelineJson -> {
                if (timelineJson != null && !timelineJson.isEmpty()) {
                    showTimelineDialog(timelineJson);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMainHandler.removeCallbacksAndMessages(null);
    }

    private void setupObservers() {
        viewModel.getTimelineLiveData().observe(getViewLifecycleOwner(), timelineJson -> {
            if (timelineJson != null && !timelineJson.isEmpty() && isAdded()) {
                loadingState.setVisibility(View.GONE);
                showTimelineDialog(timelineJson);
            }
        });

        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null && isAdded()) {
                loadingState.setVisibility(View.GONE);
                inputState.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), loading -> {
            if (loadingState != null) {
                loadingState.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showTimelineDialog(String timelineJson) {
        WebView webView = new WebView(requireContext());
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        final TimelineJsBridge jsBridge = new TimelineJsBridge(webView);
        webView.addJavascriptInterface(jsBridge, "AndroidJSBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.postDelayed(() -> {
                    view.evaluateJavascript("unifiedRender('timeline', " + timelineJson + ")", null);
                }, 200);
            }
        });

        android.app.Dialog dialog = new android.app.Dialog(requireContext());
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
            jsBridge.shutdown();
            webView.destroy();
        });

        dialog.show();
        webView.loadUrl("file:///android_asset/engine/renderer.html");
    }

    private class TimelineJsBridge {
        private final WebView mWebView;
        private android.media.MediaPlayer mCurrentPlayer;

        TimelineJsBridge(WebView webView) {
            mWebView = webView;
        }

        @android.webkit.JavascriptInterface
        public void playTTS(String actionId, String text) {
            if (text == null || text.isEmpty()) return;
            String cleanText = TtsHttpClient.cleanForTts(text);
            if (cleanText != null && cleanText.length() < (text != null ? text.length() : 0) / 2) {
                android.util.Log.w(TAG, "playTTS: WARNING cleaned text is less than half original length!");
            }

            final String id = actionId;
            final String content = cleanText;

            new Thread(() -> {
                try {
                    String base64Audio = TtsHttpClient.requestTts(content);
                    if (base64Audio != null) {
                        playBase64Audio(id, base64Audio);
                    } else {
                        notifyAudioComplete(id);
                    }
                } catch (Exception e) {
                    android.util.Log.w(TAG, "TTS playTTS error for " + id, e);
                    notifyAudioComplete(id);
                }
            }).start();
        }

        private void playBase64Audio(String actionId, String base64Data) {
            try {
                if (base64Data == null || base64Data.isEmpty()) {
                    notifyAudioComplete(actionId);
                    return;
                }

                byte[] audioBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                if (audioBytes == null || audioBytes.length == 0) {
                    notifyAudioComplete(actionId);
                    return;
                }

                android.content.Context context = getContext();
                if (context == null) { notifyAudioComplete(actionId); return; }

                java.io.File cacheDir = context.getCacheDir();
                if (cacheDir == null) { notifyAudioComplete(actionId); return; }

                java.io.File tempFile = java.io.File.createTempFile("tts_" + actionId + "_", ".mp3", cacheDir);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                    fos.write(audioBytes);
                    fos.flush();
                }

                final String id = actionId;
                final java.io.File audioFile = tempFile;

                android.app.Activity activity = getActivity();
                if (activity == null) { audioFile.delete(); notifyAudioComplete(id); return; }

                activity.runOnUiThread(() -> {
                    releaseCurrentPlayer();
                    android.media.MediaPlayer player = new android.media.MediaPlayer();
                    mCurrentPlayer = player;
                    try {
                        player.setDataSource(audioFile.getPath());
                        player.setOnPreparedListener(mp -> mp.start());
                        player.setOnCompletionListener(mp -> {
                            safeReleasePlayer(mp);
                            if (mCurrentPlayer == mp) mCurrentPlayer = null;
                            audioFile.delete();
                            notifyAudioComplete(id);
                        });
                        player.setOnErrorListener((mp, what, extra) -> {
                            safeReleasePlayer(mp);
                            if (mCurrentPlayer == mp) mCurrentPlayer = null;
                            audioFile.delete();
                            notifyAudioComplete(id);
                            return true;
                        });
                        player.prepareAsync();
                    } catch (java.io.IOException e) {
                        safeReleasePlayer(player);
                        if (mCurrentPlayer == player) mCurrentPlayer = null;
                        audioFile.delete();
                        notifyAudioComplete(id);
                    }
                });
            } catch (Exception e) {
                android.util.Log.w(TAG, "playBase64Audio error for " + actionId, e);
                notifyAudioComplete(actionId);
            }
        }

        private void releaseCurrentPlayer() {
            if (mCurrentPlayer != null) {
                try { mCurrentPlayer.reset(); } catch (Exception e) { }
                mCurrentPlayer.setOnCompletionListener(null);
                mCurrentPlayer.setOnErrorListener(null);
                mCurrentPlayer.setOnPreparedListener(null);
                mCurrentPlayer.release();
                mCurrentPlayer = null;
            }
        }

        private void safeReleasePlayer(android.media.MediaPlayer player) {
            if (player == null) return;
            try { player.reset(); } catch (Exception e) { }
            player.setOnCompletionListener(null);
            player.setOnErrorListener(null);
            player.setOnPreparedListener(null);
            player.release();
        }

        private void notifyAudioComplete(String actionId) {
            android.app.Activity activity = getActivity();
            if (activity == null || mWebView == null) return;
            activity.runOnUiThread(() -> {
                mWebView.evaluateJavascript(
                        "timelinePlayer.onAudioComplete('" + actionId.replace("'", "\\'") + "')", null);
            });
        }

        @android.webkit.JavascriptInterface
        public void onTimelineDone() {
            android.util.Log.d(TAG, "Timeline playback completed");
        }

        void shutdown() {
            releaseCurrentPlayer();
        }
    }

    private static final String TAG = "VideoLessonFragment";
}
