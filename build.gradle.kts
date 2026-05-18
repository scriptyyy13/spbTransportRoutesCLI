plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.spring") version "1.9.22"
}

group = "scriptyyy.bd.cli.app"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val springBootVersion = "3.2.3"
val springDataVersion = "3.2.3"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springDataVersion")
    implementation("org.eclipse.persistence:org.eclipse.persistence.jpa:4.0.2")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.4")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.4")
    implementation("com.google.code.gson:gson:2.11.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "scriptyyy.bd.cli.app.MainKt"
    }
}