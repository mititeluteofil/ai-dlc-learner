plugins {
    java
    alias(libs.plugins.spring.boot)
}

description = "Runnable demo application showcasing the AI-DLC starter."

dependencies {
    implementation(project(":spring-ai-dlc-starter"))
    implementation(libs.spring.boot.starter.web)

    testImplementation(libs.spring.boot.starter.test)
}
