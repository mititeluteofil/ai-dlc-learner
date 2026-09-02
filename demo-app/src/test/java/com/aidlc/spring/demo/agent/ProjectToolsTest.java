package com.aidlc.spring.demo.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectToolsTest {

    private final ProjectTools projectTools = new ProjectTools();

    @Test
    void listGradleModulesReturnsAllFourModules() {
        String modules = projectTools.listGradleModules();

        assertThat(modules)
                .contains("spring-ai-dlc-core")
                .contains("spring-ai-dlc-autoconfigure")
                .contains("spring-ai-dlc-starter")
                .contains("demo-app");
    }

    @Test
    void getBuildStatusReturnsAStubbedStatus() {
        assertThat(projectTools.getBuildStatus()).isNotBlank();
    }
}
