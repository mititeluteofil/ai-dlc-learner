package com.aidlc.spring.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aidlc.spring.core.agent.AgentFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiDlcAgentAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AiDlcAgentAutoConfiguration.class));

    @Test
    void backsOffWhenNoChatClientBuilder() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(AgentFactory.class));
    }

    @Test
    void createsAgentFactoryWhenChatClientBuilderPresent() {
        contextRunner.withUserConfiguration(ChatClientBuilderConfig.class)
                .run(context -> assertThat(context).hasSingleBean(AgentFactory.class));
    }

    @Test
    void respectsUserDefinedAgentFactory() {
        contextRunner.withUserConfiguration(ChatClientBuilderConfig.class, CustomAgentFactoryConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentFactory.class);
                    assertThat(context.getBean(AgentFactory.class))
                            .isSameAs(CustomAgentFactoryConfig.CUSTOM_AGENT_FACTORY);
                });
    }

    @Test
    void bindsAgentPropertiesFromConfiguration() {
        contextRunner.withUserConfiguration(ChatClientBuilderConfig.class)
                .withPropertyValues("ai.dlc.agent.max-tool-iterations=9")
                .run(context -> {
                    AiDlcProperties properties = context.getBean(AiDlcProperties.class);
                    assertThat(properties.agent().maxToolIterations()).isEqualTo(9);
                    assertThat(context).hasSingleBean(AgentFactory.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class ChatClientBuilderConfig {

        @Bean
        ChatClient.Builder chatClientBuilder() {
            return mock(ChatClient.Builder.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAgentFactoryConfig {

        static final AgentFactory CUSTOM_AGENT_FACTORY = new AgentFactory(mock(ChatClient.Builder.class));

        @Bean
        AgentFactory agentFactory() {
            return CUSTOM_AGENT_FACTORY;
        }
    }
}
