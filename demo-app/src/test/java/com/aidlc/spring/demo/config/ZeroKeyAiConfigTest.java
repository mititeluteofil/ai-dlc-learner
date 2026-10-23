package com.aidlc.spring.demo.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import com.aidlc.spring.demo.agent.ProjectTools;

import static org.assertj.core.api.Assertions.assertThat;

class ZeroKeyAiConfigTest {

    @Test
    void localEmbeddingModelProducesNormalized768DimVectors() {
        ZeroKeyAiConfig.ZeroKeyRoutingEmbeddingModel model =
                new ZeroKeyAiConfig.ZeroKeyRoutingEmbeddingModel(null);

        assertThat(model.dimensions()).isEqualTo(768);

        EmbeddingResponse response = model.call(new EmbeddingRequest(
                List.of("Gradle modules and Spring Boot starter", "Gradle modules in this repository"),
                null));

        assertThat(response.getResults()).hasSize(2);
        float[] v1 = response.getResults().get(0).getOutput();
        float[] v2 = response.getResults().get(1).getOutput();
        assertThat(v1).hasSize(768);
        assertThat(v2).hasSize(768);

        double dot = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += (double) v1[i] * v2[i];
        }
        assertThat(dot).isGreaterThan(0.2);
    }

    @Test
    void chatModelHandlesRouteClassificationWithoutExternalDependencies() {
        ZeroKeyAiConfig.ZeroKeyRoutingChatModel chatModel =
                new ZeroKeyAiConfig.ZeroKeyRoutingChatModel(null, new ProjectTools());

        Prompt projectPrompt = new Prompt(List.of(
                new SystemMessage("Classify the user message into exactly one of these routes: docs, project."),
                new UserMessage("What Gradle modules exist and what is the build status?")));
        ChatResponse projectResponse = chatModel.call(projectPrompt);
        assertThat(projectResponse.getResult().getOutput().getText())
                .isEqualTo("{\"route\":\"project\"}");

        Prompt docsPrompt = new Prompt(List.of(
                new SystemMessage("Classify the user message into exactly one of these routes: docs, project."),
                new UserMessage("How does the RAG ingestion pipeline chunk markdown files?")));
        ChatResponse docsResponse = chatModel.call(docsPrompt);
        assertThat(docsResponse.getResult().getOutput().getText())
                .isEqualTo("{\"route\":\"docs\"}");
    }

    @Test
    void jsonEscapeAndExtractContentRoundTrip() {
        String sampleJson = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello \\\"AI-DLC\\\"\\nLine 2\"}}]}";
        String extracted = ZeroKeyAiConfig.ZeroKeyRoutingChatModel.extractContentField(sampleJson);
        assertThat(extracted).isEqualTo("Hello \"AI-DLC\"\nLine 2");
    }
}
