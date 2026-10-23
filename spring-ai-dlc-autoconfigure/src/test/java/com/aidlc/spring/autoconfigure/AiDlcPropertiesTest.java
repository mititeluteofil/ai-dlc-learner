package com.aidlc.spring.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import com.aidlc.spring.core.agent.memory.MemoryBackend;

import static org.assertj.core.api.Assertions.assertThat;

class AiDlcPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);

    @Test
    void bindsDefaultValuesWhenNoPropertiesSet() {
        contextRunner.run(context -> {
            AiDlcProperties properties = context.getBean(AiDlcProperties.class);
            assertThat(properties.rag().chunkSize()).isEqualTo(800);
            assertThat(properties.rag().chunkOverlap()).isEqualTo(100);
            assertThat(properties.rag().topK()).isEqualTo(4);
            assertThat(properties.rag().similarityThreshold()).isEqualTo(0.0);
            assertThat(properties.rag().queryRewrite()).isFalse();
            assertThat(properties.rag().ingestOnStartup().enabled()).isFalse();
            assertThat(properties.rag().ingestOnStartup().locationPattern()).isEqualTo("rag-corpus/**/*.md");
            assertThat(properties.agent().maxToolIterations()).isEqualTo(5);
            assertThat(properties.agent().memory().enabled()).isFalse();
            assertThat(properties.agent().memory().maxMessages()).isEqualTo(20);
            assertThat(properties.agent().memory().backend()).isEqualTo(MemoryBackend.IN_MEMORY);
            assertThat(properties.agent().memory().initializeSchema()).isTrue();
        });
    }

    @Test
    void bindsOverriddenValues() {
        contextRunner.withPropertyValues(
                "ai.dlc.rag.chunk-size=500",
                "ai.dlc.rag.chunk-overlap=50",
                "ai.dlc.rag.top-k=10",
                "ai.dlc.rag.similarity-threshold=0.8",
                "ai.dlc.rag.query-rewrite=true",
                "ai.dlc.rag.ingest-on-startup.enabled=true",
                "ai.dlc.rag.ingest-on-startup.location-pattern=custom/**/*.md",
                "ai.dlc.agent.max-tool-iterations=10",
                "ai.dlc.agent.memory.enabled=true",
                "ai.dlc.agent.memory.max-messages=50",
                "ai.dlc.agent.memory.backend=pgvector",
                "ai.dlc.agent.memory.initialize-schema=false")
                .run(context -> {
                    AiDlcProperties properties = context.getBean(AiDlcProperties.class);
                    assertThat(properties.rag().chunkSize()).isEqualTo(500);
                    assertThat(properties.rag().chunkOverlap()).isEqualTo(50);
                    assertThat(properties.rag().topK()).isEqualTo(10);
                    assertThat(properties.rag().similarityThreshold()).isEqualTo(0.8);
                    assertThat(properties.rag().queryRewrite()).isTrue();
                    assertThat(properties.rag().ingestOnStartup().enabled()).isTrue();
                    assertThat(properties.rag().ingestOnStartup().locationPattern()).isEqualTo("custom/**/*.md");
                    assertThat(properties.agent().maxToolIterations()).isEqualTo(10);
                    assertThat(properties.agent().memory().enabled()).isTrue();
                    assertThat(properties.agent().memory().maxMessages()).isEqualTo(50);
                    assertThat(properties.agent().memory().backend()).isEqualTo(MemoryBackend.PGVECTOR);
                    assertThat(properties.agent().memory().initializeSchema()).isFalse();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiDlcProperties.class)
    static class PropertiesConfig {
    }
}
