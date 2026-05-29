package com.suiyuan.iragent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suiyuan.iragent.entity.Message;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 消息Mapper接口
 * 用于消息的数据库操作
 */
public interface MessageMapper extends BaseMapper<Message> {
    /**
     * 根据会话ID获取消息列表
     * @param conversationId 会话ID
     * @return 消息列表
     */
    @Select("SELECT * FROM message WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<Message> selectByConversationId(@Param("conversationId") String conversationId);

    /**
     * 根据会话ID获取最新的N条消息
     * @param conversationId 会话ID
     * @param limit 消息数量限制
     * @return 消息列表
     */
    @Select("SELECT * FROM message WHERE conversation_id = #{conversationId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Message> selectLatestByConversationId(@Param("conversationId") String conversationId, @Param("limit") int limit);

    /**
     * 删除会话的所有消息
     * @param conversationId 会话ID
     * @return 删除的消息数量
     */
    @Delete("DELETE FROM message WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(@Param("conversationId") String conversationId);

    /**
     * 根据会话ID分页获取消息列表
     * @param conversationId 会话ID
     * @param offset 偏移量
     * @param limit 每页大小
     * @return 消息列表
     */
    @Select("SELECT * FROM message WHERE conversation_id = #{conversationId} ORDER BY created_at ASC LIMIT #{limit} OFFSET #{offset}")
    List<Message> selectByConversationIdWithPage(@Param("conversationId") String conversationId,
                                                 @Param("offset") int offset, 
                                                 @Param("limit") int limit);

    /**
     * 统计会话的消息数量
     * @param conversationId 会话ID
     * @return 消息数量
     */
    @Select("SELECT COUNT(*) FROM message WHERE conversation_id = #{conversationId}")
    long countByConversationId(@Param("conversationId") String conversationId);
}
