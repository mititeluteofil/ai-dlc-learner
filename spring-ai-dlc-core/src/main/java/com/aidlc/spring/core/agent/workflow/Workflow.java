package com.aidlc.spring.core.agent.workflow;

/**
 * A single step in an agentic workflow: takes a text input, produces a text output.
 *
 * <p>Kept deliberately minimal (string in, string out) so agent-backed steps, plain
 * functions, and composite workflows ({@link ChainWorkflow}, {@link ParallelWorkflow},
 * {@link RoutingWorkflow}, {@link EvaluatorOptimizerWorkflow}) are interchangeable.
 */
@FunctionalInterface
public interface Workflow {

    String execute(String input);
}
