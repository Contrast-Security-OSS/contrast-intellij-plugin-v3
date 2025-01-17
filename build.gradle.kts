/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/

val commons_lang3 = "3.17.0"
val slf4j_api = "2.0.16"
val logback_classic = "1.4.6"
val junit = "4.13.2"
val projectlombok_lombok = "1.18.34"
val ehcache = "3.10.8:jakarta"
val mockito_core = "5.13.0"
val junit_jupiter = "5.9.3"
val mockito_junit_jupiter = "5.0.0"

buildscript {
    dependencies {
        classpath("org.yaml:snakeyaml:2.3")
    }
}

plugins {
    java
    id("org.jetbrains.intellij") version "1.17.3"
    kotlin("jvm") version "1.8.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10" // Optional: If you need serialization support
}

configurations.all {
    exclude(group = "commons-logging", module = "commons-logging")
}

group = "com.contrastsecurity"
version = "1.0.0"


repositories {
    mavenCentral()
}

dependencies {

    // Apache Commons Lang3
    implementation("org.apache.commons:commons-lang3:$commons_lang3")

    // SLF4J API
    implementation("org.slf4j:slf4j-api:$slf4j_api")
    implementation("ch.qos.logback:logback-classic:$logback_classic")

    // JUnit (Test Scope)
    testImplementation("junit:junit:$junit")
    testImplementation("org.mockito:mockito-core:$mockito_core")
    testImplementation("org.junit.jupiter:junit-jupiter:$junit_jupiter")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockito_junit_jupiter")

    // Logs
    compileOnly("org.projectlombok:lombok:$projectlombok_lombok")
    annotationProcessor("org.projectlombok:lombok:$projectlombok_lombok")

    // Failsafe
    implementation("net.jodah:failsafe:1.1.1")

    // Contrast SDK
    implementation(files("contrast-plugin-sdk-1.15-SNAPSHOT.jar"))

    // EHCache
    implementation("org.ehcache:ehcache:$ehcache")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2024.3")
    type.set("IC")
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions { freeCompilerArgs = freeCompilerArgs + listOf("-Xno-stdlib", "-Xskip-runtime-version-check") }
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    patchPluginXml {
        sinceBuild.set("242.20224.300")  // Set the minimum supported IntelliJ IDEA build
        untilBuild.set("243.*")           // Set the maximum supported IntelliJ IDEA build
    }
}