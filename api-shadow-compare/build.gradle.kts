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
}

kotlin {
    jvmToolchain(21)
}
