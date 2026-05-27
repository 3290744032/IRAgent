package com.suiyuan.iragent_app.ui.screens.study;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.suiyuan.iragent_app.data.model.Conversation;
import com.suiyuan.iragent_app.data.model.TimelineTitleResponse;
import com.suiyuan.iragent_app.data.model.v3.NoteRef;
import com.suiyuan.iragent_app.data.repository.v3.ChatRepositoryV3;
import com.suiyuan.iragent_app.data.repository.v3.ConversationRepositoryV3;

import java.io.InputStream;
import java.util.List;

public class StudyViewModelV3 extends AndroidViewModel {

    private final ChatRepositoryV3 chatRepositoryV3;
    private final ConversationRepositoryV3 conversationRepoV3;

    private final MutableLiveData<String> streamText = new MutableLiveData<>();
    private final MutableLiveData<List<NoteRef>> streamNoteRefs = new MutableLiveData<>();
    private final MutableLiveData<Boolean> streamStart = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> streamDone = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> conversationTitle = new MutableLiveData<>("💬 答疑");
    private final MutableLiveData<String> completedAiResponse = new MutableLiveData<>();

    private final StringBuilder streamBuffer = new StringBuilder();
    private String currentConversationId = "";

    public StudyViewModelV3(@NonNull Application application) {
        super(application);
        this.chatRepositoryV3 = new ChatRepositoryV3();
        this.conversationRepoV3 = new ConversationRepositoryV3();
    }

    public MutableLiveData<String> getStreamText() { return streamText; }
    public MutableLiveData<List<NoteRef>> getStreamNoteRefs() { return streamNoteRefs; }
    public MutableLiveData<Boolean> getStreamStart() { return streamStart; }
    public MutableLiveData<Boolean> getStreamDone() { return streamDone; }
    public MutableLiveData<String> getError() { return error; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }
    public MutableLiveData<String> getConversationTitle() { return conversationTitle; }
    public MutableLiveData<String> getCompletedAiResponse() { return completedAiResponse; }

    public void consumeStreamDone() {
        streamStart.postValue(false);
        streamDone.postValue(false);
    }

    public void clearError() { error.postValue(null); }

    public void setConversationId(String conversationId) {
        this.currentConversationId = conversationId != null ? conversationId : "";
    }

    public String getConversationId() { return currentConversationId; }

    public void startNewConversation() {
        currentConversationId = "";
        streamBuffer.setLength(0);
        streamText.postValue("");
        streamNoteRefs.postValue(null);
        streamStart.postValue(false);
        streamDone.postValue(false);
        error.postValue(null);
        isLoading.postValue(false);
        conversationTitle.postValue("💬 答疑");
        completedAiResponse.postValue(null);
    }

    public void chat(String question) {
        if (question == null || question.trim().isEmpty()) return;

        isLoading.postValue(true);
        streamBuffer.setLength(0);
        streamText.postValue("");

        if (currentConversationId.isEmpty()) {
            createConversationAndChat(question);
        } else {
            doChat(question);
        }
    }

    public void chatWithImage(InputStream imageStream, String question) {
        if (imageStream == null) return;

        isLoading.postValue(true);
        streamBuffer.setLength(0);
        streamText.postValue("");

        String q = (question != null && !question.trim().isEmpty()) ? question : "请帮我解答这道题";

        if (currentConversationId.isEmpty()) {
            conversationRepoV3.createConversation("AI学习", "", new ConversationRepositoryV3.ResultCallback<Conversation>() {
                @Override
                public void onSuccess(Conversation data) {
                    currentConversationId = data.getConversationId();
                    conversationTitle.postValue("💬 答疑");
                    doChatWithImage(imageStream, q);
                    generateTitle(q);
                }

                @Override
                public void onError(int code, String message) {
                    doChatWithImage(imageStream, q);
                }

                @Override
                public void onException(Exception e) {
                    doChatWithImage(imageStream, q);
                }
            });
        } else {
            doChatWithImage(imageStream, q);
        }
    }

    private void createConversationAndChat(String question) {
        conversationRepoV3.createConversation("AI学习", "", new ConversationRepositoryV3.ResultCallback<Conversation>() {
            @Override
            public void onSuccess(Conversation data) {
                currentConversationId = data.getConversationId();
                conversationTitle.postValue("💬 答疑");
                doChat(question);
                generateTitle(question);
            }

            @Override
            public void onError(int code, String message) {
                doChat(question);
            }

            @Override
            public void onException(Exception e) {
                doChat(question);
            }
        });
    }

    private void generateTitle(String question) {
        if (currentConversationId.isEmpty()) return;
        conversationRepoV3.generateTitle(question, currentConversationId, new ConversationRepositoryV3.ResultCallback<TimelineTitleResponse>() {
            @Override
            public void onSuccess(TimelineTitleResponse data) {
                if (data != null && data.getTitle() != null && !data.getTitle().isEmpty()) {
                    conversationTitle.postValue("💬 " + data.getTitle());
                }
            }

            @Override
            public void onError(int code, String message) {}

            @Override
            public void onException(Exception e) {}
        });
    }

    private void doChatWithImage(InputStream imageStream, String question) {
        chatRepositoryV3.chatStreamWithImage(imageStream, question, currentConversationId, new ChatRepositoryV3.ChatStreamCallback() {
            @Override
            public void onStart() {
                streamStart.postValue(true);
            }

            @Override
            public void onChunk(String content) {
                streamBuffer.append(content);
                streamText.postValue(streamBuffer.toString());
            }

            @Override
            public void onNoteRefs(List<NoteRef> noteRefs) {
                streamNoteRefs.postValue(noteRefs);
            }

            @Override
            public void onDone() {
                isLoading.postValue(false);
                completedAiResponse.postValue(streamBuffer.toString());
                streamDone.postValue(true);
            }

            @Override
            public void onError(int code, String message) {
                isLoading.postValue(false);
                error.postValue(message);
            }

            @Override
            public void onException(Exception e) {
                isLoading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    private void doChat(String question) {
        chatRepositoryV3.chatStream(question, currentConversationId, new ChatRepositoryV3.ChatStreamCallback() {
            @Override
            public void onStart() {
                streamStart.postValue(true);
            }

            @Override
            public void onChunk(String content) {
                streamBuffer.append(content);
                streamText.postValue(streamBuffer.toString());
            }

            @Override
            public void onNoteRefs(List<NoteRef> noteRefs) {
                streamNoteRefs.postValue(noteRefs);
            }

            @Override
            public void onDone() {
                isLoading.postValue(false);
                completedAiResponse.postValue(streamBuffer.toString());
                streamDone.postValue(true);
            }

            @Override
            public void onError(int code, String message) {
                isLoading.postValue(false);
                error.postValue(message);
            }

            @Override
            public void onException(Exception e) {
                isLoading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }
}
