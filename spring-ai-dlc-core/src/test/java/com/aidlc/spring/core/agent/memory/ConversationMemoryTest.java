package com.aidlc.spring.core.agent.memory;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationMemoryTest {

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Test
    @DisplayName("null request throws NullPointerException")
    void nullRequestThrows() {
        ConversationMemory memory = new ConversationMemory(chatMemory);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> memory.advise(null, "conv-1"));
        assertTrue(ex.getMessage().contains("request must not be null"));
    }

    @Test
    @DisplayName("attaches advisor and conversationId parameter for non-blank id")
    @SuppressWarnings("unchecked")
    void advisesNonBlankConversationId() {
        ConversationMemory memory = new ConversationMemory(chatMemory);
        assertTrue(memory.isEnabled());

        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);

        ChatClient.ChatClientRequestSpec result = memory.advise(requestSpec, "conv-123");

        assertSame(requestSpec, result);

        ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(requestSpec).advisors(captor.capture());

        ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);
        captor.getValue().accept(advisorSpec);

        verify(advisorSpec).param(ChatMemory.CONVERSATION_ID, "conv-123");
        verify(advisorSpec).advisors(any(org.springframework.ai.chat.client.advisor.api.Advisor.class));
    }

    @Test
    @DisplayName("no-ops and returns request unchanged when conversationId is null or blank")
    @SuppressWarnings("unchecked")
    void noopsOnBlankConversationId() {
        ConversationMemory memory = new ConversationMemory(chatMemory);

        assertSame(requestSpec, memory.advise(requestSpec, null));
        assertSame(requestSpec, memory.advise(requestSpec, ""));
        assertSame(requestSpec, memory.advise(requestSpec, "   "));

        verify(requestSpec, never()).advisors(any(Consumer.class));
    }

    @Test
    @DisplayName("no-ops and returns request unchanged when memory is disabled")
    @SuppressWarnings("unchecked")
    void noopsWhenDisabled() {
        ConversationMemory memory = new ConversationMemory(chatMemory, false);
        assertFalse(memory.isEnabled());

        ChatClient.ChatClientRequestSpec result = memory.advise(requestSpec, "conv-123");

        assertSame(requestSpec, result);
        verify(requestSpec, never()).advisors(any(Consumer.class));
    }

    @Test
    @DisplayName("ConversationMemory.disabled() factory creates disabled instance")
    @SuppressWarnings("unchecked")
    void disabledFactory() {
        ConversationMemory memory = ConversationMemory.disabled();
        assertFalse(memory.isEnabled());
        assertNull(memory.getChatMemory());

        ChatClient.ChatClientRequestSpec result = memory.advise(requestSpec, "conv-123");
        assertSame(requestSpec, result);
        verify(requestSpec, never()).advisors(any(Consumer.class));
    }

    @Test
    @DisplayName("degrades gracefully if advisor configuration throws an exception")
    @SuppressWarnings("unchecked")
    void degradesGracefullyOnAdvisorsException() {
        ConversationMemory memory = new ConversationMemory(chatMemory);
        when(requestSpec.advisors(any(Consumer.class))).thenThrow(new RuntimeException("advisor chain error"));

        ChatClient.ChatClientRequestSpec result = memory.advise(requestSpec, "conv-123");

        assertSame(requestSpec, result);
    }

    @Test
    @DisplayName("degrades gracefully when underlying chatMemory throws on get or add")
    @SuppressWarnings("unchecked")
    void degradesGracefullyOnChatMemoryStoreErrors() {
        ConversationMemory memory = new ConversationMemory(chatMemory);
        ChatMemory safeMemory = memory.getChatMemory();
        assertNotNull(safeMemory);

        when(chatMemory.get("conv-error")).thenThrow(new RuntimeException("Database connection lost"));
        doThrow(new RuntimeException("Disk full")).when(chatMemory).add(any(String.class), any(List.class));
        doThrow(new RuntimeException("Lock timeout")).when(chatMemory).clear(any(String.class));

        // Should return empty list without propagating exception
        List<Message> history = safeMemory.get("conv-error");
        assertTrue(history.isEmpty());

        // Should not throw
        safeMemory.add("conv-error", List.of(new UserMessage("hello")));
        safeMemory.clear("conv-error");
    }
}
