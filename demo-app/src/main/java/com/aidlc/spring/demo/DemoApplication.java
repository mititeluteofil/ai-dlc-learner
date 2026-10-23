package com.aidlc.spring.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the AI-DLC demo application.
 *
 * <p>Automatically detects at startup whether local PostgreSQL ({@code localhost:5432}) and
 * local Ollama ({@code localhost:11434}) are running. When neither is running, the app
 * seamlessly boots in standalone zero-API-key mode using an in-memory {@code SimpleVectorStore}
 * and the keyless LLM adapter in {@link com.aidlc.spring.demo.config.ZeroKeyAiConfig}.
 */
@SpringBootApplication
public class DemoApplication {

    private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        boolean postgresUp = isPortOpen("localhost", 5432, 300);
        boolean ollamaUp = isPortOpen("localhost", 11434, 300);

        List<String> excludes = new ArrayList<>();
        if (!postgresUp) {
            log.info("PostgreSQL not detected on localhost:5432 -> activating in-memory SimpleVectorStore mode.");
            excludes.add("org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration");
            excludes.add("org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration");
            excludes.add("org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration");
            excludes.add("org.springframework.ai.model.chat.memory.repository.jdbc.autoconfigure.JdbcChatMemoryRepositoryAutoConfiguration");
        }

        if (!ollamaUp) {
            log.info("Local Ollama not detected on localhost:11434 -> enabling keyless LLM fallback mode.");
            System.setProperty("spring.ai.ollama.init.pull-model-strategy", "never");
        }

        if (!excludes.isEmpty() && System.getProperty("spring.autoconfigure.exclude") == null) {
            System.setProperty("spring.autoconfigure.exclude", String.join(",", excludes));
        }

        SpringApplication.run(DemoApplication.class, args);
    }

    public static boolean isPortOpen(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
