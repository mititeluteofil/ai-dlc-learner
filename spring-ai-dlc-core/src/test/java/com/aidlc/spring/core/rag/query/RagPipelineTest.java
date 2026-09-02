package com.aidlc.spring.core.rag.query;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagPipelineTest {

    @Mock
    private VectorStore vectorStore;

    @Test
    void askReturnsAnswerWithCitationsFromRetrievedDocuments() {
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Advisor[].class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        Document retrieved = Document.builder()
            .text("Spring AI is provider-neutral.")
            .metadata("source", "rag-corpus/intro.md")
            .score(0.92)
            .build();

        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("Use ChatModel."))));
        ChatClientResponse response = ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS, List.of(retrieved))
            .build();
        when(callResponseSpec.chatClientResponse()).thenReturn(response);

        RagPipeline rag = RagPipeline.builder(chatClientBuilder, vectorStore).build();

        RagAnswer answer = rag.ask("How do I switch to OpenAI?");

        assertEquals("Use ChatModel.", answer.answer());
        assertEquals(1, answer.citations().size());
        Citation citation = answer.citations().get(0);
        assertEquals("rag-corpus/intro.md", citation.source());
        assertEquals("Spring AI is provider-neutral.", citation.snippet());
        assertEquals(0.92, citation.score());
    }

    @Test
    void builderOptionsArePropagatedToQuestionAnswerAdvisorSearchRequest() {
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Advisor[].class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("answer"))));
        ChatClientResponse response = ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(Map.of())
            .build();
        when(callResponseSpec.chatClientResponse()).thenReturn(response);

        RagPipeline rag = RagPipeline.builder(chatClientBuilder, vectorStore)
            .topK(3)
            .similarityThreshold(0.75)
            .build();

        rag.ask("question");

        ArgumentCaptor<Advisor[]> advisorCaptor = ArgumentCaptor.forClass(Advisor[].class);
        org.mockito.Mockito.verify(requestSpec).advisors(advisorCaptor.capture());
        Advisor[] advisors = advisorCaptor.getValue();
        assertEquals(1, advisors.length);
        assertTrue(advisors[0] instanceof QuestionAnswerAdvisor);
    }
}
