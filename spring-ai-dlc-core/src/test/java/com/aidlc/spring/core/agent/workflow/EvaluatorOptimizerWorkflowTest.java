package com.aidlc.spring.core.agent.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluatorOptimizerWorkflowTest {

    @Test
    void executeReturnsImmediatelyWhenEvaluationPasses() {
        ChatClient evaluatorChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(evaluatorChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(EvaluatorOptimizerWorkflow.Evaluation.class))
            .thenReturn(new EvaluatorOptimizerWorkflow.Evaluation(true, null));

        Workflow generator = input -> "answer for: " + input;

        EvaluatorOptimizerWorkflow workflow = new EvaluatorOptimizerWorkflow(generator, evaluatorChatClient, 3);

        assertEquals("answer for: question", workflow.execute("question"));
        verify(requestSpec, times(1)).call();
    }

    @Test
    void executeRetriesWithFeedbackUntilEvaluationPasses() {
        ChatClient evaluatorChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(evaluatorChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(EvaluatorOptimizerWorkflow.Evaluation.class))
            .thenReturn(new EvaluatorOptimizerWorkflow.Evaluation(false, "too short"))
            .thenReturn(new EvaluatorOptimizerWorkflow.Evaluation(true, null));

        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        Workflow generator = input -> "attempt-" + calls.incrementAndGet();

        EvaluatorOptimizerWorkflow workflow = new EvaluatorOptimizerWorkflow(generator, evaluatorChatClient, 3);

        assertEquals("attempt-2", workflow.execute("question"));
        verify(requestSpec, times(2)).call();
    }

    @Test
    void executeStopsAtMaxIterationsWhenEvaluationKeepsFailing() {
        ChatClient evaluatorChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(evaluatorChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(EvaluatorOptimizerWorkflow.Evaluation.class))
            .thenReturn(new EvaluatorOptimizerWorkflow.Evaluation(false, "still not good enough"));

        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        Workflow generator = input -> "attempt-" + calls.incrementAndGet();

        EvaluatorOptimizerWorkflow workflow = new EvaluatorOptimizerWorkflow(generator, evaluatorChatClient, 2);

        assertEquals("attempt-2", workflow.execute("question"));
        verify(requestSpec, times(2)).call();
    }
}
