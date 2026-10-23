package com.aidlc.spring.autoconfigure;

import javax.sql.DataSource;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import com.aidlc.spring.core.agent.memory.ConversationMemory;
import com.aidlc.spring.core.agent.memory.MemoryBackend;

/**
 * Autoconfiguration for conversation memory ({@link ChatMemoryRepository}, {@link ChatMemory},
 * and {@link ConversationMemory}), activated when {@code ai.dlc.agent.memory.enabled=true}.
 */
@AutoConfiguration(afterName = {
    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration"
})
@ConditionalOnProperty(prefix = "ai.dlc.agent.memory", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(AiDlcProperties.class)
public class AiDlcMemoryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ChatMemoryRepository chatMemoryRepository(
            AiDlcProperties properties,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            ObjectProvider<DataSource> dataSourceProvider) {
        MemoryBackend backend = properties.agent().memory().backend();
        return switch (backend) {
            case IN_MEMORY -> new InMemoryChatMemoryRepository();
            case JDBC -> buildJdbcRepository(backend, jdbcTemplateProvider, dataSourceProvider, false);
            case PGVECTOR -> buildJdbcRepository(backend, jdbcTemplateProvider, dataSourceProvider, true);
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository, AiDlcProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(properties.agent().memory().maxMessages())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConversationMemory conversationMemory(ChatMemory chatMemory) {
        return new ConversationMemory(chatMemory);
    }

    private static ChatMemoryRepository buildJdbcRepository(
            MemoryBackend backend,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            ObjectProvider<DataSource> dataSourceProvider,
            boolean forcePostgresDialect) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (jdbcTemplate == null && dataSource != null) {
            jdbcTemplate = new JdbcTemplate(dataSource);
        }
        if (jdbcTemplate == null) {
            throw new IllegalStateException(
                    "ai.dlc.agent.memory.backend=" + backend.name().toLowerCase()
                            + " requires a DataSource; add a JDBC driver + spring.datasource.*");
        }
        JdbcChatMemoryRepository.Builder builder = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate);
        if (dataSource != null) {
            builder.dataSource(dataSource);
        }
        if (forcePostgresDialect) {
            builder.dialect(new PostgresChatMemoryRepositoryDialect());
        }
        return builder.build();
    }
}
