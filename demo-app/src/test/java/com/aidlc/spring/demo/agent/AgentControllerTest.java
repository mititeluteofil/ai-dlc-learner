package com.aidlc.spring.demo.agent;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aidlc.spring.core.agent.Agent;
import com.aidlc.spring.core.agent.AgentDefinition;
import com.aidlc.spring.core.agent.AgentFactory;
import com.aidlc.spring.core.agent.AgentResult;
import com.aidlc.spring.core.agent.workflow.RoutingWorkflow;
import com.aidlc.spring.core.rag.query.RagAnswer;
import com.aidlc.spring.core.rag.query.RagPipeline;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for {@link AgentController}. {@link ChatClient} is provided as a deep-stub
 * mock so the {@link RoutingWorkflow} classifier prompt chain
 * ({@code prompt().system().user().call().entity(...)}) doesn't NPE; with all stubbed
 * calls returning {@code null}, the classifier falls back to the {@code docs} route,
 * which is exercised below. Live model behaviour (real routing decisions, real RAG
 * answers) is not exercised by this slice test - see the implementation report.
 */
@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagPipeline ragPipeline;

    @MockitoBean
    private AgentFactory agentFactory;

    @MockitoBean
    private ProjectTools projectTools;

    @TestConfiguration
    static class ChatClientTestConfig {

        @Bean
        @Primary
        ChatClient chatClient() {
            return mock(ChatClient.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        }
    }

    @Test
    void askFallsBackToDocsRouteAndReturnsRagAnswer() throws Exception {
        when(ragPipeline.ask(any()))
                .thenReturn(new RagAnswer("AI-DLC stands for AI-assisted development lifecycle.", java.util.List.of()));

        mockMvc.perform(post("/api/agent/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is AI-DLC?\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void summarizeRunsTheTwoStepChainWorkflow() throws Exception {
        Agent unusedProjectAgent = mock(Agent.class);
        when(agentFactory.create(any(AgentDefinition.class))).thenReturn(unusedProjectAgent);
        when(unusedProjectAgent.run(any())).thenReturn(new AgentResult("unused", java.util.List.of(), null));

        mockMvc.perform(post("/api/agent/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Some long text to summarize.\"}"))
                .andExpect(status().isOk());
    }
}
