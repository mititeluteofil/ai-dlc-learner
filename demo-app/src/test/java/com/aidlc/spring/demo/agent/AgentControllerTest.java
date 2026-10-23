package com.aidlc.spring.demo.agent;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aidlc.spring.core.agent.Agent;
import com.aidlc.spring.core.agent.AgentDefinition;
import com.aidlc.spring.core.agent.AgentFactory;
import com.aidlc.spring.core.agent.AgentResult;
import com.aidlc.spring.core.agent.memory.ConversationMemory;
import com.aidlc.spring.core.agent.workflow.RoutingWorkflow;
import com.aidlc.spring.core.rag.query.RagAnswer;
import com.aidlc.spring.core.rag.query.RagPipeline;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for {@link AgentController}.
 */
@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatClient chatClient;

    @MockitoBean
    private RagPipeline ragPipeline;

    @MockitoBean
    private AgentFactory agentFactory;

    @MockitoBean
    private ProjectTools projectTools;

    @MockitoBean
    private ConversationMemory conversationMemory;

    @TestConfiguration
    static class ChatClientTestConfig {

        @Bean
        @Primary
        ChatClient chatClient() {
            return mock(ChatClient.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        }
    }

    @BeforeEach
    void resetMocks() {
        reset(chatClient);
    }

    @Test
    void askFallsBackToDocsRouteAndReturnsRagAnswer() throws Exception {
        when(ragPipeline.ask(any()))
                .thenReturn(new RagAnswer("AI-DLC stands for AI-assisted development lifecycle.", List.of()));

        mockMvc.perform(post("/api/agent/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is AI-DLC?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("AI-DLC stands for AI-assisted development lifecycle."));

        verify(conversationMemory, never()).advise(any(), any());
    }

    @Test
    void askOnProjectRouteWithConversationIdAdvisesMemory() throws Exception {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(RoutingWorkflow.RouteClassification.class))
                .thenReturn(new RoutingWorkflow.RouteClassification("project"));

        ChatClient.ChatClientRequestSpec projectSpec =
                mock(ChatClient.ChatClientRequestSpec.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(chatClient.prompt().system(anyString()).user("What modules exist?").tools(projectTools))
                .thenReturn(projectSpec);
        when(projectSpec.call().content()).thenReturn("4 modules: core, autoconfigure, starter, demo-app.");

        when(conversationMemory.isEnabled()).thenReturn(true);
        when(conversationMemory.advise(any(), eq("conv-42"))).thenReturn(projectSpec);

        mockMvc.perform(post("/api/agent/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What modules exist?\",\"conversationId\":\"conv-42\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("4 modules: core, autoconfigure, starter, demo-app."));

        verify(conversationMemory).advise(projectSpec, "conv-42");
    }

    @Test
    void askOnProjectRouteWithoutConversationIdRemainsStateless() throws Exception {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(RoutingWorkflow.RouteClassification.class))
                .thenReturn(new RoutingWorkflow.RouteClassification("project"));

        Agent projectAgent = mock(Agent.class);
        when(agentFactory.create(any(AgentDefinition.class))).thenReturn(projectAgent);
        when(projectAgent.run("What modules exist?"))
                .thenReturn(new AgentResult("Stateless module list", List.of(), null));
        when(conversationMemory.isEnabled()).thenReturn(true);

        mockMvc.perform(post("/api/agent/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What modules exist?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Stateless module list"));

        verify(conversationMemory, never()).advise(any(), any());
        verify(projectAgent).run("What modules exist?");
    }

    @Test
    void summarizeRunsTheTwoStepChainWorkflowUnaffectedByMemory() throws Exception {
        when(conversationMemory.isEnabled()).thenReturn(true);

        mockMvc.perform(post("/api/agent/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Some long text to summarize.\",\"conversationId\":\"conv-99\"}"))
                .andExpect(status().isOk());

        verify(conversationMemory, never()).advise(any(), any());
    }
}
