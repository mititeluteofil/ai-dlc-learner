plugins {
    `java-library`
}

description = "Aggregator starter module: depends on AI-DLC core and autoconfigure."

dependencies {
    api(project(":spring-ai-dlc-core"))
    api(project(":spring-ai-dlc-autoconfigure"))
}
