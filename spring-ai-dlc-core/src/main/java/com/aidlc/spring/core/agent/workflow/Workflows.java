package com.aidlc.spring.core.agent.workflow;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Static factory methods for the workflow patterns from Anthropic's "Building Effective
 * Agents": chaining, parallelization, routing, and evaluator-optimizer loops.
 */
public final class Workflows {

    private Workflows() {
    }

    /** Runs {@code steps} in sequence, piping each output to the next input. */
    public static Workflow chain(Workflow... steps) {
        return new ChainWorkflow(List.of(steps));
    }

    /** Runs {@code steps} in sequence, piping each output to the next input. */
    public static Workflow chain(List<Workflow> steps) {
        return new ChainWorkflow(steps);
    }

    /** Runs {@code steps} concurrently against the same input and joins the outputs with newlines. */
    public static Workflow parallel(Workflow... steps) {
        return new ParallelWorkflow(List.of(steps), results -> String.join("\n", results));
    }

    /** Runs {@code steps} concurrently against the same input and combines outputs with {@code aggregator}. */
    public static Workflow parallel(List<Workflow> steps, Function<List<String>, String> aggregator) {
        return new ParallelWorkflow(steps, aggregator);
    }

    /**
     * Classifies the input with {@code chatClient} and delegates to the matching entry in
     * {@code routes}, falling back to {@code fallbackRoute} for unrecognized classifications.
     */
    public static Workflow routing(ChatClient chatClient, Map<String, Workflow> routes, String fallbackRoute) {
        return new RoutingWorkflow(chatClient, routes, fallbackRoute);
    }

    /**
     * Repeatedly runs {@code generator} and evaluates the result with {@code evaluatorChatClient},
     * retrying with feedback up to {@code maxIterations} times.
     */
    public static Workflow evaluatorOptimizer(Workflow generator, ChatClient evaluatorChatClient, int maxIterations) {
        return new EvaluatorOptimizerWorkflow(generator, evaluatorChatClient, maxIterations);
    }
}
