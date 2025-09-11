import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

plugins {
    java
    application
    kotlin("jvm") version "2.2.20" // latest stable Kotlin
}

group = "io.pokr"
version = "1.1"

application {
    mainClass.set("io.pokr.PokrioKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // Ktor
    implementation("io.ktor:ktor-server-core:3.0.1")
    implementation("io.ktor:ktor-server-netty:3.0.1")
    implementation("io.ktor:ktor-server-websockets:3.0.1")
    implementation("io.ktor:ktor-client-core:3.0.1")
    implementation("io.ktor:ktor-server-auth:3.0.1")
    implementation("io.ktor:ktor-server-thymeleaf:3.0.1")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // Socket.IO
    implementation("com.corundumstudio.socketio:netty-socketio:2.0.13")

    // Env vars
    implementation("io.github.cdimascio:java-dotenv:5.2.2")

    // Utils
    implementation("org.apache.commons:commons-text:1.12.0")

    // Database
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    implementation("org.jetbrains.exposed:exposed-core:0.53.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.53.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.8")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
}

// Create a single Jar with all dependencies (fatJar)
tasks.register<Jar>("fatJar") {
    manifest {
        attributes(
            "Implementation-Title" to "Pokr.io server",
            "Implementation-Version" to version,
            "Main-Class" to "io.pokr.PokrioKt"
        )
    }
    archiveBaseName.set("pokrio")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Use Java 23 toolchain consistently
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

// Align Kotlin with Java 23
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
    }
}

tasks.test {
    useJUnitPlatform()
}
