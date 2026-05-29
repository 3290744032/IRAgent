package com.suiyuan.iragent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.suiyuan.iragent.entity.Conversation;
import com.suiyuan.iragent.entity.Message;
import com.suiyuan.iragent.mapper.ConversationMapper;
import com.suiyuan.iragent.mapper.MessageMapper;
import com.suiyuan.iragent.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {

    private final MessageMapper messageMapper;

    @Override
    public Conversation createConversation(Long userId, String name, String description) {
        Conversation conversation = new Conversation();
        conversation.setConversationId(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setName(name);
        conversation.setDescription(description);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversation.setStatus("active");
        save(conversation);
        return conversation;
    }

    @Override
    public List<Conversation> getConversationsByUserId(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    @Override
    public List<Conversation> getConversationsByUserIdWithPage(Long userId, int page, int size) {
        return baseMapper.selectByUserIdWithPage(userId, (page - 1) * size, size);
    }

    @Override
    public long countConversationsByUserId(Long userId) {
        return baseMapper.countByUserId(userId);
    }

    @Override
    public List<Conversation> getActiveConversationsByUserId(Long userId) {
        return baseMapper.selectActiveByUserId(userId);
    }

    @Override
    public Conversation getConversationById(String conversationId) {
        return getById(conversationId);
    }

    @Override
    public Conversation updateConversation(Conversation conversation) {
        conversation.setUpdatedAt(LocalDateTime.now());
        updateById(conversation);
        return conversation;
    }

    @Override
    public void updateConversationName(String conversationId, String name) {
        Conversation conversation = getById(conversationId);
        if (conversation != null) {
            conversation.setName(name);
            conversation.setUpdatedAt(LocalDateTime.now());
            updateById(conversation);
        }
    }

    @Override
    public boolean deleteConversation(String conversationId) {
        return removeById(conversationId);
    }

    @Override
    public Message sendMessage(String conversationId, String senderType, String content, String messageType) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : "text");
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);

        baseMapper.updateUpdatedAt(conversationId, LocalDateTime.now());
        return message;
    }

    @Override
    public List<Message> getMessagesByConversationId(String conversationId) {
        return messageMapper.selectByConversationId(conversationId);
    }

    @Override
    public List<Message> getMessagesByConversationIdWithPage(String conversationId, int page, int size) {
        return messageMapper.selectByConversationIdWithPage(conversationId, (page - 1) * size, size);
    }

    @Override
    public long countMessagesByConversationId(String conversationId) {
        return messageMapper.countByConversationId(conversationId);
    }

    @Override
    public List<Message> getLatestMessagesByConversationId(String conversationId, int limit) {
        return messageMapper.selectLatestByConversationId(conversationId, limit);
    }

    @Override
    public String getLatestMessageContent(String conversationId) {
        return baseMapper.selectLatestMessageContent(conversationId);
    }

    @Override
    public int clearMessagesByConversationId(String conversationId) {
        return messageMapper.deleteByConversationId(conversationId);
    }
}
