package com.aidlc.spring.core.agent.workflow;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Runs a list of {@link Workflow} steps concurrently against the same input, using one
 * virtual thread per step, then combines the per-step outputs with {@code aggregator}.
 *
 * <p>When {@code stepTimeout} is non-null, each step must complete within that duration;
 * a step that exceeds it is cancelled and the whole run fails fast rather than blocking
 * indefinitely on a hung step. When {@code stepTimeout} is {@code null} the run waits for
 * every step to finish (legacy behavior).
 */
public record ParallelWorkflow(List<Workflow> steps, Function<List<String>, String> aggregator, Duration stepTimeout)
        implements Workflow {

    public ParallelWorkflow {
        Objects.requireNonNull(steps, "steps must not be null");
        Objects.requireNonNull(aggregator, "aggregator must not be null");
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
        if (stepTimeout != null && (stepTimeout.isZero() || stepTimeout.isNegative())) {
            throw new IllegalArgumentException("stepTimeout must be positive when set");
        }
        steps = List.copyOf(steps);
    }

    /** Creates a workflow with no per-step timeout (waits for every step to finish). */
    public ParallelWorkflow(List<Workflow> steps, Function<List<String>, String> aggregator) {
        this(steps, aggregator, null);
    }

    @Override
    public String execute(String input) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = steps.stream()
                .map(step -> executor.submit(() -> step.execute(input)))
                .toList();

            List<String> results = futures.stream()
                .map(future -> join(future, futures))
                .toList();

            return aggregator.apply(results);
        }
    }

    private String join(Future<String> future, List<Future<String>> all) {
        try {
            return stepTimeout != null
                ? future.get(stepTimeout.toMillis(), TimeUnit.MILLISECONDS)
                : future.get();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for parallel workflow step", e);
        }
        catch (ExecutionException e) {
            throw new IllegalStateException("Parallel workflow step failed", e.getCause());
        }
        catch (TimeoutException e) {
            all.forEach(f -> f.cancel(true));
            throw new IllegalStateException("Parallel workflow step timed out after " + stepTimeout, e);
        }
    }
}
