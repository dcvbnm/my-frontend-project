package org.ershoupingtai.controller;

import org.ershoupingtai.common.Result;
import org.ershoupingtai.pojo.ChatConversationItem;
import org.ershoupingtai.service.chatService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class chatController {

    private final chatService chatService;

    public chatController(chatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat-list")
    public String chatListPage() {
        return "chat-list";
    }

    @ResponseBody
    @GetMapping("/api/chat/conversations")
    public Result<List<ChatConversationItem>> listConversations(@RequestParam Integer userId) {
        if (userId == null || userId <= 0) {
            return Result.fail("用户ID不能为空");
        }
        return Result.success(chatService.listConversationsByUserId(userId));
    }

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        e.printStackTrace();
        return Result.fail("聊天列表加载失败，请稍后重试");
    }
}
