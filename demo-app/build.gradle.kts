plugins {
    java
    alias(libs.plugins.spring.boot)
}

description = "Runnable demo application showcasing the AI-DLC starter."

dependencies {
    implementation(project(":spring-ai-dlc-starter"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)

    // Provides the Ollama ChatModel/EmbeddingModel beans (chat client builder,
    // embeddings for ingestion) consumed by the AI-DLC autoconfiguration.
    implementation(libs.spring.ai.starter.model.ollama)

    // Provides the VectorStore bean required for AiDlcRagAutoConfiguration to activate.
    implementation(libs.spring.ai.starter.vector.store.pgvector)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
}
