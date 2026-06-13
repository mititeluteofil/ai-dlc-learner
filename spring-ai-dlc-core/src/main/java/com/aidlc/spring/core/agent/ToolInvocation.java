package com.aidlc.spring.core.agent;

/**
 * A single tool call made during an {@link Agent} run: the tool name, the JSON
 * arguments the model supplied, and the result returned to the model.
 */
public record ToolInvocation(String toolName, String arguments, String result) {
}
