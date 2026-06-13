plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

allprojects {
    group = "com.aidlc"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(rootProject.libs.versions.java.get()))
        }
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom(rootProject.libs.spring.boot.dependencies.get().toString())
            mavenBom(rootProject.libs.spring.ai.bom.get().toString())
            mavenBom(rootProject.libs.junit.bom.get().toString())
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"(rootProject.libs.junit.jupiter)
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}
