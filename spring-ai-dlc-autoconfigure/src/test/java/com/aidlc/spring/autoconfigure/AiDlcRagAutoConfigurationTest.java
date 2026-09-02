package com.aidlc.spring.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aidlc.spring.core.rag.ingest.IngestionPipeline;
import com.aidlc.spring.core.rag.query.RagPipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class AiDlcRagAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AiDlcChatAutoConfiguration.class, AiDlcRagAutoConfiguration.class));

    @Test
    void backsOffWhenNoVectorStoreOrChatClientBuilder() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(IngestionPipeline.class);
            assertThat(context).doesNotHaveBean(RagPipeline.class);
        });
    }

    @Test
    void createsDefaultBeansWhenVectorStoreAndChatClientBuilderPresent() {
        contextRunner.withUserConfiguration(VectorStoreAndChatClientBuilderConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(IngestionPipeline.class);
                    assertThat(context).hasSingleBean(RagPipeline.class);
                });
    }

    @Test
    void respectsUserDefinedBeans() {
        contextRunner.withUserConfiguration(VectorStoreAndChatClientBuilderConfig.class, CustomBeansConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(IngestionPipeline.class);
                    assertThat(context).hasSingleBean(RagPipeline.class);
                    assertThat(context.getBean(IngestionPipeline.class))
                            .isSameAs(CustomBeansConfig.CUSTOM_INGESTION_PIPELINE);
                    assertThat(context.getBean(RagPipeline.class)).isSameAs(CustomBeansConfig.CUSTOM_RAG_PIPELINE);
                });
    }

    @Test
    void bindsRagPropertiesFromConfiguration() {
        contextRunner.withUserConfiguration(VectorStoreAndChatClientBuilderConfig.class)
                .withPropertyValues(
                        "ai.dlc.rag.top-k=7",
                        "ai.dlc.rag.similarity-threshold=0.42",
                        "ai.dlc.rag.query-rewrite=true")
                .run(context -> {
                    AiDlcProperties properties = context.getBean(AiDlcProperties.class);
                    assertThat(properties.rag().topK()).isEqualTo(7);
                    assertThat(properties.rag().similarityThreshold()).isEqualTo(0.42);
                    assertThat(properties.rag().queryRewrite()).isTrue();
                    assertThat(context).hasSingleBean(RagPipeline.class);
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

    @Configuration(proxyBeanMethods = false)
    static class CustomBeansConfig {

        static final IngestionPipeline CUSTOM_INGESTION_PIPELINE = new IngestionPipeline(mock(VectorStore.class));
        static final RagPipeline CUSTOM_RAG_PIPELINE =
                RagPipeline.builder(mock(ChatClient.Builder.class), mock(VectorStore.class)).build();

        @Bean
        IngestionPipeline ingestionPipeline() {
            return CUSTOM_INGESTION_PIPELINE;
        }

        @Bean
        RagPipeline ragPipeline() {
            return CUSTOM_RAG_PIPELINE;
        }
    }
}
