package com.aidlc.spring.autoconfigure;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Provides a default {@link ChatClient} with request/response logging when none is defined.
 */
@AutoConfiguration
@ConditionalOnBean(ChatClient.Builder.class)
public class AiDlcChatAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor()).build();
    }
}
