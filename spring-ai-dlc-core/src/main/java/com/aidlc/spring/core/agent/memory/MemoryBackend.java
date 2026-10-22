package com.aidlc.spring.core.agent.memory;

/**
 * Storage backends supported for conversation memory persistence.
 */
public enum MemoryBackend {

    /**
     * Ephemeral in-memory store. Conversations are discarded on application restart.
     */
    IN_MEMORY,

    /**
     * Relational JDBC store using standard SQL tables via the application's {@code DataSource}.
     */
    JDBC,

    /**
     * Relational store using the pgvector PostgreSQL database shared with RAG storage.
     */
    PGVECTOR
}
