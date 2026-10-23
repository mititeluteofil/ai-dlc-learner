package com.aidlc.spring.demo.agent;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.aidlc.spring.core.agent.Agent;
import com.aidlc.spring.core.agent.AgentDefinition;
import com.aidlc.spring.core.agent.AgentFactory;
import com.aidlc.spring.core.agent.memory.ConversationMemory;
import com.aidlc.spring.core.agent.workflow.RoutingWorkflow;
import com.aidlc.spring.core.agent.workflow.Workflow;
import com.aidlc.spring.core.agent.workflow.Workflows;
import com.aidlc.spring.core.rag.query.RagPipeline;

/**
 * Agent workflow endpoint: routes a question to either the docs-RAG pipeline or a
 * tool-calling agent over {@link ProjectTools} (with optional conversation memory keyed
 * by {@code conversationId}), and demonstrates a small {@link Workflows#chain} example.
 */
@RestController
public class AgentController {

    private static final String DOCS_ROUTE = "docs";
    private static final String PROJECT_ROUTE = "project";
    private static final String PROJECT_SYSTEM_PROMPT =
            "You are an assistant that answers questions about the ai-dlc-learner repository's "
                    + "Gradle modules and build status. Use the available tools to answer accurately.";

    private final ChatClient chatClient;
    private final Workflow docsWorkflow;
    private final AgentFactory agentFactory;
    private final ProjectTools projectTools;
    private final ObjectProvider<ConversationMemory> conversationMemoryProvider;
    private final Workflow summaryChain;
    private Agent projectAgent;

    public AgentController(
            ChatClient chatClient,
            RagPipeline ragPipeline,
            AgentFactory agentFactory,
            ProjectTools projectTools) {
        this(chatClient, ragPipeline, agentFactory, projectTools, null);
    }

    @Autowired
    public AgentController(
            ChatClient chatClient,
            RagPipeline ragPipeline,
            AgentFactory agentFactory,
            ProjectTools projectTools,
            ObjectProvider<ConversationMemory> conversationMemoryProvider) {
        this.chatClient = chatClient;
        this.agentFactory = agentFactory;
        this.projectTools = projectTools;
        this.conversationMemoryProvider = conversationMemoryProvider;
        this.docsWorkflow = question -> ragPipeline.ask(question).answer();

        this.projectAgent = createProjectAgent();

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
        Workflow projectWorkflow = question -> executeProjectRoute(question, request.conversationId());
        RoutingWorkflow routingWorkflow = new RoutingWorkflow(
                chatClient,
                Map.of(DOCS_ROUTE, docsWorkflow, PROJECT_ROUTE, projectWorkflow),
                DOCS_ROUTE);
        return new AskResponse(routingWorkflow.execute(request.question()));
    }

    @PostMapping("/api/agent/summarize")
    public AskResponse summarize(@RequestBody AskRequest request) {
        return new AskResponse(summaryChain.execute(request.question()));
    }

    private String executeProjectRoute(String question, String conversationId) {
        ConversationMemory memory = conversationMemoryProvider != null
                ? conversationMemoryProvider.getIfAvailable()
                : null;
        if (memory != null && memory.isEnabled() && conversationId != null && !conversationId.isBlank()) {
            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                    .system(PROJECT_SYSTEM_PROMPT)
                    .user(question)
                    .tools(projectTools);
            requestSpec = memory.advise(requestSpec, conversationId);
            String content = requestSpec.call().content();
            return content != null ? content : "";
        }
        Agent agent = this.projectAgent != null ? this.projectAgent : createProjectAgent();
        return agent.run(question).finalText();
    }

    private Agent createProjectAgent() {
        return agentFactory.create(AgentDefinition.of(
                "project-assistant",
                PROJECT_SYSTEM_PROMPT)
                .withTools(List.of(projectTools)));
    }

    public record AskRequest(String question, String conversationId) {
        public AskRequest(String question) {
            this(question, null);
        }
    }

    public record AskResponse(String answer) {
    }
}
