package com.aidlc.spring.core.agent;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentFactoryTest {

    @Test
    void createBuildsAnAgentFromTheChatClientBuilder() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        when(builder.build()).thenReturn(chatClient);

        AgentFactory factory = new AgentFactory(builder);

        Agent agent = factory.create(AgentDefinition.of("assistant", "You are helpful."));

        assertNotNull(agent);
    }

    @Test
    void constructorRejectsNonPositiveDefaultMaxIterations() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);

        assertThrows(IllegalArgumentException.class, () -> new AgentFactory(builder, 0));
    }
}
