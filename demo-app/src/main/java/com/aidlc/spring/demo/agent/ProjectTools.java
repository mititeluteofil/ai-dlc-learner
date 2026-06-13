package com.aidlc.spring.demo.agent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Tool-calling surface for the "project question" route of the agent workflow demo.
 *
 * <p>Deliberately simple and local-only (no external API calls), per the implementation
 * plan: a tool-calling agent can answer questions about this repo's Gradle modules and a
 * stubbed build status.
 */
@Component
public class ProjectTools {

    /**
     * The Gradle modules that make up this repo, as declared in {@code settings.gradle.kts}.
     */
    @Tool(description = "List the Gradle module names that make up this repository")
    public String listGradleModules() {
        return String.join(", ", "spring-ai-dlc-core", "spring-ai-dlc-autoconfigure", "spring-ai-dlc-starter", "demo-app");
    }

    /**
     * Stubbed build status - no external CI API calls, per the implementation plan.
     */
    @Tool(description = "Get the current build status of the project")
    public String getBuildStatus() {
        return "All modules last built successfully (./gradlew build): BUILD SUCCESSFUL";
    }
}
