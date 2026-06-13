package com.aidlc.spring.core.agent.workflow;

import java.util.List;
import java.util.Objects;

/**
 * Runs a list of {@link Workflow} steps in sequence, piping each step's output to the
 * next step's input. The chain's output is the last step's output.
 */
public record ChainWorkflow(List<Workflow> steps) implements Workflow {

    public ChainWorkflow {
        Objects.requireNonNull(steps, "steps must not be null");
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
        steps = List.copyOf(steps);
    }

    @Override
    public String execute(String input) {
        String current = input;
        for (Workflow step : steps) {
            current = step.execute(current);
        }
        return current;
    }
}
