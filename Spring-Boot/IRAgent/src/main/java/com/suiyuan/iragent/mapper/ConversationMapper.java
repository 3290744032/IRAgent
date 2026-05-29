package com.suiyuan.iragent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suiyuan.iragent.entity.Conversation;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话Mapper接口
 * 用于会话的数据库操作
 */
public interface ConversationMapper extends BaseMapper<Conversation> {
    /**
     * 根据用户ID获取会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    @Select("SELECT * FROM conversation WHERE user_id = #{userId} ORDER BY updated_at DESC")
    List<Conversation> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID获取活跃会话列表
     * @param userId 用户ID
     * @return 活跃会话列表
     */
    @Select("SELECT * FROM conversation WHERE user_id = #{userId} AND status = 'active' ORDER BY updated_at DESC")
    List<Conversation> selectActiveByUserId(@Param("userId") Long userId);

    /**
     * 更新会话的更新时间（优化N+1查询问题）
     * @param conversationId 会话ID
     * @param updatedAt 更新时间
     * @return 更新的记录数
     */
    @Update("UPDATE conversation SET updated_at = #{updatedAt} WHERE conversation_id = #{conversationId}")
    int updateUpdatedAt(@Param("conversationId") String conversationId, @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 根据用户ID分页获取会话列表
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 每页大小
     * @return 会话列表
     */
    @Select("SELECT * FROM conversation WHERE user_id = #{userId} ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Conversation> selectByUserIdWithPage(@Param("userId") Long userId, 
                                               @Param("offset") int offset, 
                                               @Param("limit") int limit);

    /**
     * 统计用户的会话数量
     * @param userId 用户ID
     * @return 会话数量
     */
    @Select("SELECT COUNT(*) FROM conversation WHERE user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 获取会话的最新消息摘要
     * @param conversationId 会话ID
     * @return 最新消息内容
     */
    @Select("SELECT content FROM message WHERE conversation_id = #{conversationId} ORDER BY created_at DESC LIMIT 1")
    String selectLatestMessageContent(@Param("conversationId") String conversationId);
}
