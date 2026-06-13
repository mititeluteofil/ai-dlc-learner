package com.aidlc.spring.core.agent.workflow;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Classifies the input with a structured-output {@link ChatClient} call, then delegates
 * to the matching {@link Workflow} in {@code routes}.
 *
 * <p>The classifier is prompted with the available route names and must respond with
 * exactly one of them, parsed via {@link ChatClient.CallResponseSpec#entity(Class)} into
 * {@link RouteClassification}. If the model returns an unknown route, {@code fallback}
 * is used.
 */
public record RoutingWorkflow(ChatClient chatClient, Map<String, Workflow> routes, String fallbackRoute)
        implements Workflow {

    public RoutingWorkflow {
        Objects.requireNonNull(chatClient, "chatClient must not be null");
        Objects.requireNonNull(routes, "routes must not be null");
        Objects.requireNonNull(fallbackRoute, "fallbackRoute must not be null");
        if (routes.isEmpty()) {
            throw new IllegalArgumentException("routes must not be empty");
        }
        if (!routes.containsKey(fallbackRoute)) {
            throw new IllegalArgumentException("fallbackRoute must be a key of routes");
        }
        routes = Map.copyOf(routes);
    }

    @Override
    public String execute(String input) {
        String route = classify(input);
        Workflow workflow = routes.getOrDefault(route, routes.get(fallbackRoute));
        return workflow.execute(input);
    }

    private String classify(String input) {
        Set<String> routeNames = routes.keySet();
        RouteClassification classification = chatClient.prompt()
            .system("""
                Classify the user message into exactly one of these routes: %s.
                Respond with only the route name.""".formatted(String.join(", ", routeNames)))
            .user(input)
            .call()
            .entity(RouteClassification.class);

        if (classification == null || classification.route() == null) {
            return fallbackRoute;
        }
        return classification.route();
    }

    /** Structured output of the classifier: the chosen route name. */
    public record RouteClassification(String route) {
    }
}
