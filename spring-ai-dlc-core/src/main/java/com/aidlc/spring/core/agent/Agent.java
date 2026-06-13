package com.aidlc.spring.core.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * Runs an {@link AgentDefinition} against a {@link ChatClient}.
 *
 * <p>Spring AI's {@code ToolCallingAdvisor} resolves {@code @Tool} calls internally
 * during {@code call()}, so a single request normally returns a final answer. The
 * {@code maxIterations} guard re-prompts (re-issuing the same conversation) only if the
 * model response still reports pending tool calls after a round, and gives up after
 * {@code maxIterations} rounds rather than looping forever.
 */
public final class Agent {

    private final AgentDefinition definition;
    private final ChatClient chatClient;
    private final int maxIterations;

    Agent(AgentDefinition definition, ChatClient chatClient, int maxIterations) {
        this.definition = Objects.requireNonNull(definition, "definition must not be null");
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient must not be null");
        if (maxIterations < 1) {
            throw new IllegalArgumentException("maxIterations must be at least 1");
        }
        this.maxIterations = maxIterations;
    }

    public AgentResult run(String userMessage) {
        Objects.requireNonNull(userMessage, "userMessage must not be null");

        ChatResponse response = null;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                .system(definition.systemPrompt())
                .user(userMessage);

            if (!definition.tools().isEmpty()) {
                request = request.tools(definition.tools().toArray());
            }
            if (definition.options() != null) {
                request = request.options(definition.options().mutate());
            }

            response = request.call().chatResponse();

            if (response == null || !response.hasToolCalls()) {
                break;
            }
        }

        return toResult(response);
    }

    private AgentResult toResult(ChatResponse response) {
        if (response == null) {
            return new AgentResult("", List.of(), null);
        }

        String finalText = response.getResult() != null && response.getResult().getOutput() != null
            ? response.getResult().getOutput().getText()
            : "";

        List<ToolInvocation> toolInvocations = new ArrayList<>();
        if (response.hasToolCalls()) {
            AssistantMessage output = response.getResult().getOutput();
            for (AssistantMessage.ToolCall toolCall : output.getToolCalls()) {
                toolInvocations.add(new ToolInvocation(toolCall.name(), toolCall.arguments(), null));
            }
        }

        return new AgentResult(finalText, List.copyOf(toolInvocations), response.getMetadata().getUsage());
    }
}
