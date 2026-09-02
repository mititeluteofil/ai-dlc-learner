package com.aidlc.spring.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiDlcChatAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(AiDlcChatAutoConfiguration.class));

    @Test
    void backsOffWhenNoChatClientBuilder() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ChatClient.class));
    }

    @Test
    void createsDefaultChatClientWhenChatClientBuilderPresent() {
        contextRunner.withUserConfiguration(ChatClientBuilderConfig.class)
                .run(context -> assertThat(context).hasSingleBean(ChatClient.class));
    }

    @Test
    void respectsUserDefinedChatClient() {
        contextRunner.withUserConfiguration(ChatClientBuilderConfig.class, CustomChatClientConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatClient.class);
                    assertThat(context.getBean(ChatClient.class)).isSameAs(CustomChatClientConfig.CUSTOM_CHAT_CLIENT);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class ChatClientBuilderConfig {

        @Bean
        ChatClient.Builder chatClientBuilder() {
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            org.mockito.Mockito.when(builder.defaultAdvisors(org.mockito.Mockito.any(
                    org.springframework.ai.chat.client.advisor.api.Advisor[].class))).thenReturn(builder);
            org.mockito.Mockito.when(builder.build()).thenReturn(mock(ChatClient.class));
            return builder;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomChatClientConfig {

        static final ChatClient CUSTOM_CHAT_CLIENT = mock(ChatClient.class);

        @Bean
        ChatClient chatClient() {
            return CUSTOM_CHAT_CLIENT;
        }
    }
}
