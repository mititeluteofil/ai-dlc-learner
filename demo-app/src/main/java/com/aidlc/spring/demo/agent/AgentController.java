package com.aidlc.spring.demo.agent;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.aidlc.spring.core.agent.Agent;
import com.aidlc.spring.core.agent.AgentDefinition;
import com.aidlc.spring.core.agent.AgentFactory;
import com.aidlc.spring.core.agent.workflow.RoutingWorkflow;
import com.aidlc.spring.core.agent.workflow.Workflow;
import com.aidlc.spring.core.agent.workflow.Workflows;
import com.aidlc.spring.core.rag.query.RagPipeline;

/**
 * Agent workflow endpoint: routes a question to either the docs-RAG pipeline or a
 * tool-calling agent over {@link ProjectTools}, and demonstrates a small
 * {@link Workflows#chain} example.
 */
@RestController
public class AgentController {

    private static final String DOCS_ROUTE = "docs";
    private static final String PROJECT_ROUTE = "project";

    private final RoutingWorkflow routingWorkflow;
    private final Workflow summaryChain;

    public AgentController(ChatClient chatClient, RagPipeline ragPipeline, AgentFactory agentFactory, ProjectTools projectTools) {
        Workflow docsWorkflow = question -> ragPipeline.ask(question).answer();

        Agent projectAgent = agentFactory.create(AgentDefinition.of(
                "project-assistant",
                "You are an assistant that answers questions about the ai-dlc-learner repository's "
                        + "Gradle modules and build status. Use the available tools to answer accurately.")
                .withTools(java.util.List.of(projectTools)));
        Workflow projectWorkflow = question -> projectAgent.run(question).finalText();

        this.routingWorkflow = new RoutingWorkflow(
                chatClient,
                Map.of(DOCS_ROUTE, docsWorkflow, PROJECT_ROUTE, projectWorkflow),
                DOCS_ROUTE);

        // Illustrative ChainWorkflow: summarize the input, then reformat the summary as
        // bullet points - a small 2-step text transform over the chat model.
        Workflow summarizeStep = text -> chatClient.prompt()
                .system("Summarize the user's text in two sentences or fewer.")
                .user(text)
                .call()
                .content();
        Workflow bulletPointsStep = summary -> chatClient.prompt()
                .system("Rewrite the given text as a concise bullet-point list.")
                .user(summary)
                .call()
                .content();
        this.summaryChain = Workflows.chain(summarizeStep, bulletPointsStep);
    }

    @PostMapping("/api/agent/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        return new AskResponse(routingWorkflow.execute(request.question()));
    }

    @PostMapping("/api/agent/summarize")
    public AskResponse summarize(@RequestBody AskRequest request) {
        return new AskResponse(summaryChain.execute(request.question()));
    }

    public record AskRequest(String question) {
    }

    public record AskResponse(String answer) {
    }
}
