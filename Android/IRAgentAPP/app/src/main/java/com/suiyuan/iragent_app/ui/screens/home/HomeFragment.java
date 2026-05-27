package com.suiyuan.iragent_app.ui.screens.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.ui.screens.deeplearn.DeepLearnFragment;
import com.suiyuan.iragent_app.ui.screens.study.StudyFragment;
import com.suiyuan.iragent_app.ui.screens.video.VideoLessonFragment;

public class HomeFragment extends Fragment {

    private ImageView ivPlay, ivGenerateVideo;
    private TextView tvStartLearn;
    private LinearLayout layoutAskQuestion, layoutDeepLearn, layoutVideoIntro;
    private EditText etVideoTopic;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        ivPlay = view.findViewById(R.id.iv_play);
        tvStartLearn = view.findViewById(R.id.tv_start_learn);
        layoutAskQuestion = view.findViewById(R.id.layout_ask_question);
        layoutDeepLearn = view.findViewById(R.id.layout_study_record);
        layoutVideoIntro = view.findViewById(R.id.layout_video_intro);
        etVideoTopic = view.findViewById(R.id.et_video_topic);
        ivGenerateVideo = view.findViewById(R.id.iv_generate_video);

        View cardRoot = (View) ivPlay.getParent().getParent().getParent().getParent();

        cardRoot.setOnClickListener(v -> openVideoDemo());
        ivPlay.setOnClickListener(v -> openVideoDemo());
        tvStartLearn.setOnClickListener(v -> openVideoDemo());
        layoutAskQuestion.setOnClickListener(v -> openAskQuestion());
        layoutDeepLearn.setOnClickListener(v -> openDeepLearn());
        ivGenerateVideo.setOnClickListener(v -> generateVideo());
        etVideoTopic.setOnEditorActionListener((v, actionId, event) -> {
            generateVideo();
            return true;
        });

        return view;
    }

    private void openVideoDemo() {
        Bundle args = new Bundle();
        args.putBoolean("auto_timeline", true);
        VideoLessonFragment fragment = new VideoLessonFragment();
        fragment.setArguments(args);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack("video_lesson")
                .commit();
    }

    private void openAskQuestion() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new StudyFragment())
                .addToBackStack("study")
                .commit();
    }

    private void openDeepLearn() {
        DeepLearnFragment fragment = new DeepLearnFragment();
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack("deep_learn")
                .commit();
    }

    private void generateVideo() {
        String topic = etVideoTopic.getText().toString().trim();
        if (topic.isEmpty()) {
            Toast.makeText(getContext(), "请输入知识点名称", Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle args = new Bundle();
        args.putString("topic", topic);
        VideoLessonFragment fragment = new VideoLessonFragment();
        fragment.setArguments(args);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack("video_lesson")
                .commit();
    }
}
