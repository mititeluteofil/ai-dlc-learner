package com.aidlc.spring.autoconfigure;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.aidlc.spring.core.agent.AgentFactory;

/**
 * Autoconfiguration for {@link AgentFactory}, activated when a {@link ChatClient.Builder} is available.
 */
@AutoConfiguration(after = AiDlcChatAutoConfiguration.class)
@ConditionalOnBean(ChatClient.Builder.class)
@EnableConfigurationProperties(AiDlcProperties.class)
public class AiDlcAgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentFactory agentFactory(ChatClient.Builder chatClientBuilder, AiDlcProperties properties) {
        return new AgentFactory(chatClientBuilder, properties.agent().maxToolIterations());
    }
}
