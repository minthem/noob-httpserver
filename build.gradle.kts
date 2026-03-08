plugins {
    kotlin("jvm") version "2.3.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

group = "io.github.minthem.noob-httpserver"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.koverHtmlReport)
}
kotlin {
    jvmToolchain(25)
}
