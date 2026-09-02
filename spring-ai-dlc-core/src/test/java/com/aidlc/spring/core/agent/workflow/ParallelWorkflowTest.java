package com.aidlc.spring.core.agent.workflow;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParallelWorkflowTest {

    @Test
    void executeRunsAllStepsAndAggregatesResultsInOrder() {
        Workflow first = input -> "first:" + input;
        Workflow second = input -> "second:" + input;

        ParallelWorkflow parallel = new ParallelWorkflow(List.of(first, second), results -> String.join(",", results));

        assertEquals("first:x,second:x", parallel.execute("x"));
    }
}
