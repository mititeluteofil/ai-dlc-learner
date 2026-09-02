package com.aidlc.spring.core.agent;

import java.util.List;

import org.springframework.ai.chat.metadata.Usage;

/**
 * Result of {@link Agent#run(String)}: the model's final text answer, the tool calls
 * made along the way (if any were still unresolved when iteration stopped), and token
 * usage for the last model call.
 */
public record AgentResult(String finalText, List<ToolInvocation> toolInvocations, Usage usage) {
}
