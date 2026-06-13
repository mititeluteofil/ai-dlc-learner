package com.aidlc.spring.demo.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Throwaway REST controller used in Phase 2 to verify the raw Spring AI +
 * Ollama integration end-to-end. Expected to be replaced by the core
 * RAG/agent facades in a later phase.
 */
@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @PostMapping("/api/chat/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        String answer = chatClient.prompt()
                .user(request.question())
                .call()
                .content();
        return new AskResponse(answer);
    }

    public record AskRequest(String question) {
    }

    public record AskResponse(String answer) {
    }
}
