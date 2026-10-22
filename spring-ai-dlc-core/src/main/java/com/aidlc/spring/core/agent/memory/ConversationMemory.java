package com.aidlc.spring.core.agent.memory;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

/**
 * Facade wrapping Spring AI's {@link ChatMemory} and {@link MessageChatMemoryAdvisor}.
 *
 * <p>Provides conversation history integration for stateful agent interactions,
 * decorating {@link ChatClient.ChatClientRequestSpec} with a conversation advisor
 * when a non-blank conversation identifier is provided, while degrading gracefully
 * (logging and proceeding without memory context) if the underlying memory store throws.
 */
public final class ConversationMemory {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemory.class);

    private final ChatMemory chatMemory;
    private final MessageChatMemoryAdvisor advisor;
    private final boolean enabled;

    public ConversationMemory(ChatMemory chatMemory) {
        this(chatMemory, chatMemory != null);
    }

    public ConversationMemory(ChatMemory chatMemory, boolean enabled) {
        this.enabled = enabled && chatMemory != null;
        if (this.enabled) {
            this.chatMemory = new SafeChatMemory(chatMemory);
            this.advisor = MessageChatMemoryAdvisor.builder(this.chatMemory).build();
        } else {
            this.chatMemory = null;
            this.advisor = null;
        }
    }

    public static ConversationMemory disabled() {
        return new ConversationMemory(null, false);
    }

    /**
     * Whether conversation memory is enabled and backed by an active store.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the underlying (wrapped) {@link ChatMemory}, or {@code null} if disabled.
     */
    public ChatMemory getChatMemory() {
        return chatMemory;
    }

    /**
     * Decorates the given request specification with conversation memory for the given conversation ID.
     *
     * <p>If memory is disabled, or if {@code conversationId} is {@code null} or blank, the request
     * is returned unchanged. If an exception occurs while advising, the error is logged and the
     * un-advised request is returned to ensure best-effort availability.
     *
     * @param request the request specification to decorate
     * @param conversationId the conversation identifier, or blank/null for stateless requests
     * @return the decorated request spec, or the original request on blank ID or error
     */
    public ChatClient.ChatClientRequestSpec advise(ChatClient.ChatClientRequestSpec request, String conversationId) {
        Objects.requireNonNull(request, "request must not be null");
        if (!isEnabled() || conversationId == null || conversationId.isBlank()) {
            return request;
        }
        try {
            return request.advisors(spec -> {
                spec.advisors(advisor);
                spec.param(ChatMemory.CONVERSATION_ID, conversationId);
            });
        } catch (Exception ex) {
            log.warn("Failed to advise request with conversation memory for conversationId '{}': {}",
                conversationId, ex.getMessage(), ex);
            return request;
        }
    }

    /**
     * Decorator around {@link ChatMemory} that catches exceptions to enable graceful degradation.
     */
    private static final class SafeChatMemory implements ChatMemory {

        private final ChatMemory delegate;

        private SafeChatMemory(ChatMemory delegate) {
            this.delegate = Objects.requireNonNull(delegate, "chatMemory must not be null");
        }

        @Override
        public void add(String conversationId, List<Message> messages) {
            try {
                delegate.add(conversationId, messages);
            } catch (Exception ex) {
                log.warn("Failed to persist conversation memory for conversationId '{}': {}",
                    conversationId, ex.getMessage(), ex);
            }
        }

        @Override
        public List<Message> get(String conversationId) {
            try {
                return delegate.get(conversationId);
            } catch (Exception ex) {
                log.warn("Failed to retrieve conversation memory for conversationId '{}': {}",
                    conversationId, ex.getMessage(), ex);
                return List.of();
            }
        }

        @Override
        public void clear(String conversationId) {
            try {
                delegate.clear(conversationId);
            } catch (Exception ex) {
                log.warn("Failed to clear conversation memory for conversationId '{}': {}",
                    conversationId, ex.getMessage(), ex);
            }
        }
    }
}
