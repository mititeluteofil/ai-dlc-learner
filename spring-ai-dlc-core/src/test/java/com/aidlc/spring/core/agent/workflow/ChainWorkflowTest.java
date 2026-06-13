package com.aidlc.spring.core.agent.workflow;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChainWorkflowTest {

    @Test
    void executePipesOutputOfEachStepIntoTheNext() {
        Workflow upper = input -> input.toUpperCase();
        Workflow exclaim = input -> input + "!";

        ChainWorkflow chain = new ChainWorkflow(List.of(upper, exclaim));

        assertEquals("HELLO!", chain.execute("hello"));
    }

    @Test
    void constructorRejectsEmptySteps() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> new ChainWorkflow(List.of()));
    }
}
