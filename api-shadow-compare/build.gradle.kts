plugins {
    kotlin("jvm") version "2.0.20"
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("plugin.spring") version "2.0.20"
}

repositories {
    mavenCentral()
    maven("https://repo.spring.io/release")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.1")
    implementation("com.github.java-json-tools:json-patch:1.13")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // --- Kotest core ---
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")     // JUnit5 runner
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")   // shouldBe, matchers
    testImplementation("io.kotest:kotest-property:5.9.1")          // property testing (옵션)

    // --- (Spring 환경일 경우) ---
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform() // Kotest는 JUnit5 플랫폼 위에서 실행됨
}