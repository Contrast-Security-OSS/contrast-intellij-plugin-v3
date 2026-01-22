/*******************************************************************************
 * Copyright © 2026 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.util.Locale

val commons_lang3 = "3.17.0"
val slf4j_api = "2.0.16"
val logback_classic = "1.4.6"
val junit = "4.13.2"
val contrast_plugin_sdk = "1.15"
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
    id("org.jetbrains.intellij.platform") version "2.10.5"  // Change from 2.1.0
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

configurations.all {
    exclude("commons-logging", "commons-logging")
}

group = "com.contrastsecurity"
version = "1.0.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// IntelliJ Platform Configuration (2.x)
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251.23774"
            untilBuild = "253.*"
        }
    }
}

// Dependencies
dependencies {

    intellijPlatform {
        create("IC", "2025.1")
        // instrumentationTools() is deprecated in 2.10.4, no longer needed
    }

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

    // Contrast SDK
    implementation(files("contrast-plugin-sdk-1.15-SNAPSHOT.jar"))

    // EHCache
    implementation("org.ehcache:ehcache:$ehcache")

    implementation("net.jodah:failsafe:1.1.1")
}

tasks {
    // disable the risky searchable options task that often fails headlessly
    buildSearchableOptions {
        enabled = false
    }
}

// Misc: disable searchable options task explicitly (extra precaution)
tasks.buildSearchableOptions {
    enabled = false
}

// Java toolchain, compiler and Kotlin settings
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // recommended for IntelliJ 2025.1
    }
}

tasks.withType<JavaCompile> {
    // IntelliJ 2025.1 requires Java 21
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.encoding = "UTF-8"

    // suppress unchecked/raw warnings globally if you want to skip them
    options.compilerArgs.add("-Xlint:-unchecked")
    options.compilerArgs.add("-Xlint:-rawtypes")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)  // Change to 21
        freeCompilerArgs.addAll(
            "-Xno-stdlib",
            "-Xskip-runtime-version-check"
        )
    }
}

// ---------- Test configuration ---------
tasks.test {
    useJUnitPlatform()
    // If IntelliJ-platform tests cause Index:1 errors for 2025.1, run with -x test or set enabled=false while iterating.
}

tasks.named<org.jetbrains.intellij.platform.gradle.tasks.SignPluginTask>("signPlugin") {
    val certChain = System.getenv("CERTIFICATE_CHAIN")
    val privKey = System.getenv("PRIVATE_KEY")
    val privPwd = System.getenv("PRIVATE_KEY_PASSWORD")
    if (!certChain.isNullOrBlank()) certificateChain.set(certChain)
    if (!privKey.isNullOrBlank()) privateKey.set(privKey)
    if (!privPwd.isNullOrBlank()) password.set(privPwd)
}

tasks.named<org.jetbrains.intellij.platform.gradle.tasks.PublishPluginTask>("publishPlugin") {
    val token = System.getenv("PUBLISH_TOKEN")
    if (!token.isNullOrBlank()) this.token.set(token)
}