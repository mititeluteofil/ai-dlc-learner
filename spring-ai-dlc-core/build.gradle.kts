plugins {
    `java-library`
}

description = "Provider-neutral AI-DLC facades over Spring AI core interfaces (RAG, agents, workflows)."

dependencies {
    api(libs.spring.ai.client.chat)
    api(libs.spring.ai.rag)
    api(libs.spring.ai.vector.store)
    api(libs.spring.ai.vector.store.advisor)
    api(libs.spring.ai.model)

    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}
