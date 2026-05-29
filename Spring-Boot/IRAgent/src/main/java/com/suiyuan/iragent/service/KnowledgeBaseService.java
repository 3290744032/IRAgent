package com.suiyuan.iragent.service;

import com.suiyuan.iragent.rag.retrieval.PersonalNoteRetriever.NoteFragment;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface KnowledgeBaseService {
    Map<String, Object> upload(Long userId, String title, String content, String subject, String chapter, String tags, String fileType);
    Map<String, Object> upload(Long userId, String title, String content, String subject, String chapter, String tags, String fileType, String imageUrl);
    Map<String, Object> uploadFile(Long userId, MultipartFile file, String userTitle) throws IOException;
    List<Map<String, Object>> listNotes(Long userId, String subject, int page, int size);
    Map<String, Object> getNoteDetail(Long userId, String noteId);
    void updateNote(Long userId, String noteId, Map<String, String> body);
    Map<String, Object> optimizeNote(Long userId, String noteId, String instruction);
    Map<String, Object> optimizeNote(Long userId, String noteId);
    Map<String, Object> classifyContent(String content);
    List<NoteFragment> searchNotes(Long userId, String query, int topK);
    void deleteNote(Long userId, String noteId);
}
