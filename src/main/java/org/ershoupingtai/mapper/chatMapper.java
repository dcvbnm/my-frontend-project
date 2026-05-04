package org.ershoupingtai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ershoupingtai.pojo.ChatConversationItem;

import java.util.List;

@Mapper
public interface chatMapper {

    @Select("SELECT c.id AS conversationId, " +
            "CASE WHEN c.user_id1 = #{userId} THEN c.user_id2 ELSE c.user_id1 END AS otherUserId, " +
            "ISNULL(CASE WHEN u.UserName = N'1111111111' THEN N'管理员' ELSE u.UserName END, N'用户' + CAST(CASE WHEN c.user_id1 = #{userId} THEN c.user_id2 ELSE c.user_id1 END AS NVARCHAR(20))) AS otherUserName, " +
            "ISNULL(c.last_message, N'') AS lastMessage, " +
            "c.last_message_time AS lastMessageTime, " +
            "CASE WHEN c.user_id1 = #{userId} THEN ISNULL(c.unread_count1, 0) ELSE ISNULL(c.unread_count2, 0) END AS unreadCount " +
            "FROM dbo.conversation c " +
            "LEFT JOIN dbo.UserLogin u ON u.UserId = CASE WHEN c.user_id1 = #{userId} THEN c.user_id2 ELSE c.user_id1 END " +
            "WHERE (c.user_id1 = #{userId} OR c.user_id2 = #{userId}) " +
            "AND ISNULL(c.is_active, 1) = 1 " +
            "ORDER BY ISNULL(c.last_message_time, c.updated_at) DESC")
    List<ChatConversationItem> listConversationsByUserId(@Param("userId") int userId);
}
