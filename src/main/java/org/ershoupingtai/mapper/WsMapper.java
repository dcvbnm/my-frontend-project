package org.ershoupingtai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.logging.log4j.message.Message;
import org.ershoupingtai.pojo.Conversation;
import org.ershoupingtai.pojo.Messages;

@Mapper
public interface WsMapper {
    @Select("SELECT id FROM conversations WHERE (user_id1 = #{userId} AND user_id2 = #{sellerId}) OR (user_id1 = #{sellerId} AND user_id2 = #{userId})")
    Integer getConversationId(@Param("userId") int userId, @Param("sellerId") int sellerId);

    @Select("SELECT id FROM conversations WHERE id = #{conversationId} AND (user_id1 = #{userId} OR user_id2 = #{userId})")
    Integer checkConversationAccess(@Param("conversationId") int conversationId, @Param("userId") int userId);

    @Select("SELECT id FROM conversations WHERE (user_id1 = #{userId} AND user_id2 = #{authorUserId}) OR (user_id1 = #{authorUserId} AND user_id2 = #{userId})")
    Integer getConversationIdByUserIds(@Param("userId") int userId, @Param("authorUserId") int authorUserId);

    @Insert("INSERT INTO conversations (user_id1, user_id2, last_message, last_message_time, " +
            "unread_count1, unread_count2, is_top1, is_top2, is_muted1, is_muted2, " +
            "is_active, created_at, updated_at) " +
            "VALUES (#{userId1}, #{userId2}, #{lastMessage}, #{lastMessageTime}, " +
            "#{unreadCount1}, #{unreadCount2}, #{isTop1}, #{isTop2}, #{isMuted1}, #{isMuted2}, " +
            "#{isActive}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 自动生成ID 
    int insertConversation(Conversation conversation);

    @Select("SELECT * FROM messages WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<Message> getMessagesByConversationId(@Param("conversationId") int conversationId);

    @Insert("INSERT INTO messages (conversation_id, sender_id, content, created_at, time_str) " +
            "VALUES (#{conversationId}, #{senderId}, #{content}, #{createdAt}, #{timeStr})")
    int insertMessage(Messages message);

}
