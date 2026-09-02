package com.aidlc.spring.core.agent.workflow;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingWorkflowTest {

    @Test
    void executeDelegatesToTheRouteChosenByTheClassifier() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(RoutingWorkflow.RouteClassification.class))
            .thenReturn(new RoutingWorkflow.RouteClassification("docs"));

        Workflow docsWorkflow = input -> "docs-answer:" + input;
        Workflow projectWorkflow = input -> "project-answer:" + input;

        RoutingWorkflow routing = new RoutingWorkflow(chatClient,
            Map.of("docs", docsWorkflow, "project", projectWorkflow), "docs");

        assertEquals("docs-answer:how do I switch providers?", routing.execute("how do I switch providers?"));
    }

    @Test
    void executeFallsBackWhenClassificationIsUnknownRoute() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(RoutingWorkflow.RouteClassification.class))
            .thenReturn(new RoutingWorkflow.RouteClassification("unknown-route"));

        Workflow docsWorkflow = input -> "docs-answer:" + input;
        Workflow projectWorkflow = input -> "project-answer:" + input;

        RoutingWorkflow routing = new RoutingWorkflow(chatClient,
            Map.of("docs", docsWorkflow, "project", projectWorkflow), "docs");

        assertEquals("docs-answer:something else", routing.execute("something else"));
    }
}
