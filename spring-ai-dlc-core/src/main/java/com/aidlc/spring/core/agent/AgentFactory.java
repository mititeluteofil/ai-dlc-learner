package com.aidlc.spring.core.agent;

import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;

/**
 * The only place provider configuration ({@link ChatClient.Builder}) touches agent
 * construction. Builds {@link Agent} instances from {@link AgentDefinition}s, sharing a
 * default max-tool-iterations bound unless an override is supplied.
 */
public final class AgentFactory {

    private static final int DEFAULT_MAX_ITERATIONS = 5;

    private final ChatClient.Builder chatClientBuilder;
    private final int defaultMaxIterations;

    public AgentFactory(ChatClient.Builder chatClientBuilder) {
        this(chatClientBuilder, DEFAULT_MAX_ITERATIONS);
    }

    public AgentFactory(ChatClient.Builder chatClientBuilder, int defaultMaxIterations) {
        this.chatClientBuilder = Objects.requireNonNull(chatClientBuilder, "chatClientBuilder must not be null");
        if (defaultMaxIterations < 1) {
            throw new IllegalArgumentException("defaultMaxIterations must be at least 1");
        }
        this.defaultMaxIterations = defaultMaxIterations;
    }

    public Agent create(AgentDefinition definition) {
        return create(definition, defaultMaxIterations);
    }

    public Agent create(AgentDefinition definition, int maxIterations) {
        Objects.requireNonNull(definition, "definition must not be null");
        return new Agent(definition, chatClientBuilder.build(), maxIterations);
    }
}
