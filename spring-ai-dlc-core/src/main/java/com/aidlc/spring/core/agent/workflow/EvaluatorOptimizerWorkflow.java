package com.aidlc.spring.core.agent.workflow;

import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Generator + evaluator loop: {@code generator} produces a response, an evaluator
 * {@link ChatClient} call grades it (pass/fail + feedback) via structured output. On
 * failure, the generator is re-run with the feedback appended to the input, up to
 * {@code maxIterations} attempts. The last generated response is returned regardless of
 * the final evaluation outcome.
 */
public record EvaluatorOptimizerWorkflow(Workflow generator, ChatClient evaluatorChatClient, int maxIterations)
        implements Workflow {

    public EvaluatorOptimizerWorkflow {
        Objects.requireNonNull(generator, "generator must not be null");
        Objects.requireNonNull(evaluatorChatClient, "evaluatorChatClient must not be null");
        if (maxIterations < 1) {
            throw new IllegalArgumentException("maxIterations must be at least 1");
        }
    }

    @Override
    public String execute(String input) {
        String currentInput = input;
        String response = "";

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            response = generator.execute(currentInput);

            Evaluation evaluation = evaluate(input, response);
            if (evaluation == null || evaluation.passed()) {
                return response;
            }

            currentInput = """
                %s

                Your previous response was:
                %s

                It was rejected for this reason:
                %s

                Please address the feedback and respond again.""".formatted(input, response, evaluation.feedback());
        }

        return response;
    }

    private Evaluation evaluate(String originalInput, String response) {
        return evaluatorChatClient.prompt()
            .system("""
                Evaluate whether the response adequately addresses the request. Respond with
                a pass/fail judgement and, if it fails, feedback explaining what to fix.""")
            .user("""
                Request:
                %s

                Response:
                %s""".formatted(originalInput, response))
            .call()
            .entity(Evaluation.class);
    }

    /** Structured output of the evaluator: pass/fail plus optional feedback for retries. */
    public record Evaluation(boolean passed, String feedback) {
    }
}
