plugins {
    `java-library`
}

description = "Spring Boot autoconfiguration for the AI-DLC starter (ai.dlc.* properties)."

dependencies {
    api(project(":spring-ai-dlc-core"))

    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.ai.autoconfigure.model.chat.client)
    implementation(libs.spring.ai.autoconfigure.model.tool)

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.test)
}
