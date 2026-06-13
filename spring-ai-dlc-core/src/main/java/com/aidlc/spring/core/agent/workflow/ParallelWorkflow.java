package com.aidlc.spring.core.agent.workflow;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Runs a list of {@link Workflow} steps concurrently against the same input, using one
 * virtual thread per step, then combines the per-step outputs with {@code aggregator}.
 */
public record ParallelWorkflow(List<Workflow> steps, Function<List<String>, String> aggregator) implements Workflow {

    public ParallelWorkflow {
        Objects.requireNonNull(steps, "steps must not be null");
        Objects.requireNonNull(aggregator, "aggregator must not be null");
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
        steps = List.copyOf(steps);
    }

    @Override
    public String execute(String input) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = steps.stream()
                .map(step -> executor.submit(() -> step.execute(input)))
                .toList();

            List<String> results = futures.stream()
                .map(this::join)
                .toList();

            return aggregator.apply(results);
        }
    }

    private String join(Future<String> future) {
        try {
            return future.get();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for parallel workflow step", e);
        }
        catch (ExecutionException e) {
            throw new IllegalStateException("Parallel workflow step failed", e.getCause());
        }
    }
}
