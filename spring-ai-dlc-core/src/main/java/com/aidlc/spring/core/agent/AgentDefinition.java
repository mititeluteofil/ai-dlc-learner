package com.aidlc.spring.core.agent;

import java.util.List;
import java.util.Objects;

import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Declarative description of an agent: its system prompt, the {@code @Tool}-annotated
 * objects it may call, and the {@link ChatOptions} (temperature, etc.) used when
 * prompting the model.
 *
 * <p>{@code tools} are plain objects passed to {@link org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec#tools(Object...)}
 * — methods annotated with {@code org.springframework.ai.tool.annotation.Tool}.
 */
public record AgentDefinition(String name, String systemPrompt, List<Object> tools, ChatOptions options) {

    public AgentDefinition {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(systemPrompt, "systemPrompt must not be null");
        tools = tools == null ? List.of() : List.copyOf(tools);
    }

    /** Convenience factory for an agent with no tools and default chat options. */
    public static AgentDefinition of(String name, String systemPrompt) {
        return new AgentDefinition(name, systemPrompt, List.of(), null);
    }

    public AgentDefinition withTools(List<Object> tools) {
        return new AgentDefinition(name, systemPrompt, tools, options);
    }

    public AgentDefinition withOptions(ChatOptions options) {
        return new AgentDefinition(name, systemPrompt, tools, options);
    }
}
