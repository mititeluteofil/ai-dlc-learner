package com.aidlc.spring.demo.config;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidlc.spring.autoconfigure.AiDlcProperties;
import com.aidlc.spring.core.agent.memory.ConversationMemory;
import com.aidlc.spring.demo.DemoApplication;

/**
 * Reports the active zero-API-key runtime mode and memory configuration for the demo UI and CLI users.
 */
@RestController
public class DemoStatusController {

    private final AiDlcProperties properties;
    private final ObjectProvider<ConversationMemory> conversationMemoryProvider;

    public DemoStatusController(
            AiDlcProperties properties,
            ObjectProvider<ConversationMemory> conversationMemoryProvider) {
        this.properties = properties;
        this.conversationMemoryProvider = conversationMemoryProvider;
    }

    @GetMapping("/api/demo/status")
    public Map<String, Object> status() {
        boolean ollamaUp = DemoApplication.isPortOpen("localhost", 11434, 200);
        boolean postgresUp = DemoApplication.isPortOpen("localhost", 5432, 200);
        ConversationMemory memory = conversationMemoryProvider.getIfAvailable();

        return Map.of(
                "llmProvider", ollamaUp ? "local-ollama (http://localhost:11434)" : "keyless-llm (https://text.pollinations.ai/openai)",
                "apiKeyRequired", false,
                "vectorStore", postgresUp ? "pgvector (localhost:5432)" : "in-memory (SimpleVectorStore)",
                "memoryEnabled", memory != null && memory.isEnabled(),
                "memoryBackend", properties.agent().memory().backend().name(),
                "memoryMaxMessages", properties.agent().memory().maxMessages()
        );
    }
}
