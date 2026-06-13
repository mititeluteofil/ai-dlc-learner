plugins {
    java
    alias(libs.plugins.spring.boot)
}

description = "Runnable demo application showcasing the AI-DLC starter."

dependencies {
    implementation(project(":spring-ai-dlc-starter"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)

    // Phase 2: raw Spring AI Ollama starter to de-risk the stack before
    // building the custom core/autoconfigure modules (replaced in Phase 6).
    implementation(libs.spring.ai.starter.model.ollama)

    testImplementation(libs.spring.boot.starter.test)
}
