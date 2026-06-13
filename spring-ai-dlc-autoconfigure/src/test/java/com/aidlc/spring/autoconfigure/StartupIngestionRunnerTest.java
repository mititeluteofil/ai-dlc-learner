package com.aidlc.spring.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aidlc.spring.core.rag.ingest.IngestionPipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class StartupIngestionRunnerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(
                    AiDlcChatAutoConfiguration.class, AiDlcRagAutoConfiguration.class, StartupIngestionRunner.class));

    @Test
    void doesNotRegisterRunnerWhenIngestOnStartupDisabled() {
        contextRunner.withUserConfiguration(VectorStoreAndChatClientBuilderConfig.class)
                .run(context -> assertThat(context).doesNotHaveBean(ApplicationRunner.class));
    }

    @Test
    void registersRunnerWhenIngestOnStartupEnabled() {
        contextRunner.withUserConfiguration(VectorStoreAndChatClientBuilderConfig.class)
                .withPropertyValues(
                        "ai.dlc.rag.ingest-on-startup.enabled=true",
                        "ai.dlc.rag.ingest-on-startup.location-pattern=rag-corpus/**/*.md")
                .run(context -> assertThat(context).hasSingleBean(ApplicationRunner.class));
    }

    @Test
    void doesNotRegisterRunnerWhenIngestionPipelineMissing() {
        contextRunner.withPropertyValues("ai.dlc.rag.ingest-on-startup.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(IngestionPipeline.class);
                    assertThat(context).doesNotHaveBean(ApplicationRunner.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class VectorStoreAndChatClientBuilderConfig {

        @Bean
        VectorStore vectorStore() {
            return mock(VectorStore.class);
        }

        @Bean
        ChatClient.Builder chatClientBuilder() {
            ChatClient.Builder builder = mock(ChatClient.Builder.class, withSettings().defaultAnswer(RETURNS_SELF));
            org.mockito.Mockito.when(builder.build()).thenReturn(mock(ChatClient.class));
            return builder;
        }
    }
}
