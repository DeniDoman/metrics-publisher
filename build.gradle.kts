val ktor_version: String by project
val kotlin_version: String by project
val exposed_version: String by project
val postgres_version: String by project
val logback_version: String by project
val kotlin_logging_version: String by project
val junit_bom_version: String by project
val junit_jupiter_version: String by project
val mockito_kotlin_version: String by project
val github_version: String by project
val logback_encoder_version: String by project
val h2_version: String by project
val jjwt_version: String by project
val bouncy_castle_version: String by project

plugins {
    kotlin("jvm") version "1.8.21"
    id("io.ktor.plugin") version "2.3.12"
    id("com.google.cloud.tools.jib") version "3.4.4"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.25"
}

group = "com.domanskii"
version = "0.1.0"
application {
    mainClass.set("com.domanskii.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.postgresql:postgresql:$postgres_version")

    implementation("org.kohsuke:github-api:$github_version")

    // JWT and crypto for GitHub App authentication
    implementation("io.jsonwebtoken:jjwt-api:$jjwt_version")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwt_version")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwt_version")
    implementation("org.bouncycastle:bcpkix-jdk18on:$bouncy_castle_version")

    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("net.logstash.logback:logstash-logback-encoder:$logback_encoder_version")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlin_logging_version")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")

    testImplementation("com.h2database:h2:$h2_version") // Add this line

    testImplementation(platform("org.junit:junit-bom:$junit_bom_version"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junit_jupiter_version")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockito_kotlin_version")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}