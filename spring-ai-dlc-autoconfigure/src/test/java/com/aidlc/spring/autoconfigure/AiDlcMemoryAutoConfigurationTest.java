package com.aidlc.spring.autoconfigure;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import com.aidlc.spring.core.agent.memory.ConversationMemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiDlcMemoryAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AiDlcMemoryAutoConfiguration.class));

    @Test
    void doesNotRegisterBeansWhenDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ChatMemoryRepository.class);
            assertThat(context).doesNotHaveBean(ChatMemory.class);
            assertThat(context).doesNotHaveBean(ConversationMemory.class);
        });
    }

    @Test
    void registersInMemoryRepositoryAndWindowedMemoryByDefaultWhenEnabled() {
        contextRunner.withPropertyValues(
                "ai.dlc.agent.memory.enabled=true",
                "ai.dlc.agent.memory.max-messages=3")
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatMemoryRepository.class);
                    assertThat(context.getBean(ChatMemoryRepository.class))
                            .isInstanceOf(InMemoryChatMemoryRepository.class);

                    assertThat(context).hasSingleBean(ChatMemory.class);
                    ChatMemory chatMemory = context.getBean(ChatMemory.class);
                    assertThat(chatMemory).isInstanceOf(MessageWindowChatMemory.class);

                    // Verify maxMessages window bound
                    chatMemory.add("conv-1", List.of(
                            new UserMessage("m1"),
                            new UserMessage("m2"),
                            new UserMessage("m3"),
                            new UserMessage("m4")));
                    assertThat(chatMemory.get("conv-1")).hasSize(3);

                    assertThat(context).hasSingleBean(ConversationMemory.class);
                    assertThat(context.getBean(ConversationMemory.class).isEnabled()).isTrue();
                });
    }

    @Test
    void registersJdbcRepositoryWhenJdbcBackendConfiguredWithDataSource() {
        DataSource dataSource = mock(DataSource.class);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        contextRunner
                .withPropertyValues(
                        "ai.dlc.agent.memory.enabled=true",
                        "ai.dlc.agent.memory.backend=jdbc")
                .withBean(DataSource.class, () -> dataSource)
                .withBean(JdbcTemplate.class, () -> jdbcTemplate)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatMemoryRepository.class);
                    assertThat(context.getBean(ChatMemoryRepository.class))
                            .isInstanceOf(JdbcChatMemoryRepository.class);
                    assertThat(context).hasSingleBean(ChatMemory.class);
                    assertThat(context).hasSingleBean(ConversationMemory.class);
                });
    }

    @Test
    void registersJdbcRepositoryWithPostgresDialectWhenPgvectorBackendConfigured() {
        DataSource dataSource = mock(DataSource.class);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        contextRunner
                .withPropertyValues(
                        "ai.dlc.agent.memory.enabled=true",
                        "ai.dlc.agent.memory.backend=pgvector")
                .withBean(DataSource.class, () -> dataSource)
                .withBean(JdbcTemplate.class, () -> jdbcTemplate)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatMemoryRepository.class);
                    assertThat(context.getBean(ChatMemoryRepository.class))
                            .isInstanceOf(JdbcChatMemoryRepository.class);
                    assertThat(context).hasSingleBean(ChatMemory.class);
                    assertThat(context).hasSingleBean(ConversationMemory.class);
                });
    }

    @Test
    void failsFastWhenJdbcBackendConfiguredWithoutDataSource() {
        contextRunner
                .withPropertyValues(
                        "ai.dlc.agent.memory.enabled=true",
                        "ai.dlc.agent.memory.backend=jdbc")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .rootCause()
                            .hasMessageContaining("requires a DataSource");
                });
    }

    @Test
    void failsFastWhenPgvectorBackendConfiguredWithoutDataSource() {
        contextRunner
                .withPropertyValues(
                        "ai.dlc.agent.memory.enabled=true",
                        "ai.dlc.agent.memory.backend=pgvector")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .rootCause()
                            .hasMessageContaining("requires a DataSource");
                });
    }

    @Test
    void backsOffWhenCustomChatMemoryAndRepositoryAreProvided() {
        ChatMemoryRepository customRepo = mock(ChatMemoryRepository.class);
        ChatMemory customMemory = mock(ChatMemory.class);

        contextRunner
                .withPropertyValues("ai.dlc.agent.memory.enabled=true")
                .withBean(ChatMemoryRepository.class, () -> customRepo)
                .withBean(ChatMemory.class, () -> customMemory)
                .run(context -> {
                    assertThat(context.getBean(ChatMemoryRepository.class)).isSameAs(customRepo);
                    assertThat(context.getBean(ChatMemory.class)).isSameAs(customMemory);
                    assertThat(context).hasSingleBean(ConversationMemory.class);
                });
    }
}
