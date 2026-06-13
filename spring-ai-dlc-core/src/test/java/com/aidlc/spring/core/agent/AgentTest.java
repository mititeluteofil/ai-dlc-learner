package com.aidlc.spring.core.agent;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentTest {

    @Test
    void runReturnsFinalTextAndUsageWhenNoToolCallsArePending() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage("Hello there"))))
            .metadata(ChatResponseMetadata.builder().usage(new DefaultUsage(10, 5, 15)).build())
            .build();
        when(callResponseSpec.chatResponse()).thenReturn(chatResponse);

        AgentDefinition definition = AgentDefinition.of("greeter", "You are a greeter.");
        Agent agent = new Agent(definition, chatClient, 3);

        AgentResult result = agent.run("hi");

        assertEquals("Hello there", result.finalText());
        assertTrue(result.toolInvocations().isEmpty());
        assertEquals(15, result.usage().getTotalTokens());
        verify(requestSpec, times(1)).call();
    }

    @Test
    void runStopsAtMaxIterationsWhenToolCallsKeepBeingRequested() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call-1", "function", "getBuildStatus", "{}");
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(toolCall))
            .build();
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(assistantMessage)))
            .metadata(ChatResponseMetadata.builder().usage(new DefaultUsage(1, 1, 2)).build())
            .build();
        when(callResponseSpec.chatResponse()).thenReturn(chatResponse);

        AgentDefinition definition = new AgentDefinition("tool-agent", "You use tools.", List.of(new Object()), null);
        Agent agent = new Agent(definition, chatClient, 2);

        AgentResult result = agent.run("what's the build status?");

        assertEquals(1, result.toolInvocations().size());
        assertEquals("getBuildStatus", result.toolInvocations().get(0).toolName());
        verify(requestSpec, times(2)).call();
    }
}
