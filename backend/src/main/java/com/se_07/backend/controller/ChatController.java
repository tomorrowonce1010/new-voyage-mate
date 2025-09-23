package com.se_07.backend.controller;

import com.se_07.backend.dto.ChatMessageDTO;
import com.se_07.backend.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ChatController {

    @Autowired
    private ChatMessageService chatMessageService;

    @PostMapping("/send")
    public ChatMessageDTO sendMessage(@RequestBody ChatMessageDTO dto) {
        return chatMessageService.sendMessage(dto);
    }

    @PostMapping("/history")
    public List<ChatMessageDTO> getMessagesBetween(@RequestParam Long userId1, @RequestParam Long userId2) {
        return chatMessageService.getMessagesBetweenUsers(userId1, userId2);
    }

    @GetMapping("/all")
    public List<ChatMessageDTO> getAllMessagesForUser(@RequestParam Long userId) {
        return chatMessageService.getAllMessagesForUser(userId);
    }
}
