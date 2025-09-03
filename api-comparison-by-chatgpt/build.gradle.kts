import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
//    id("org.jetbrains.kotlin.plugin.spring") version "1.9.25" apply false
    id("org.springframework.boot") version "3.3.4" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

allprojects {
    group = "com.example"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

//subprojects {
//    apply(plugin = "org.jetbrains.kotlin.jvm")
//    apply(plugin = "io.spring.dependency-management")
//
//    repositories { mavenCentral() }
//
//    kotlin { jvmToolchain(21) }
//}
