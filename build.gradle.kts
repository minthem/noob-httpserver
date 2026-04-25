import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "2.3.0"

    id("org.jetbrains.kotlinx.kover") version "0.9.8"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("com.vanniktech.maven.publish") version "0.36.0"
    id("signing")
}

group = "io.github.minthem"
version = System.getenv("CI_TAG") ?: "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.koverHtmlReport)
}
kotlin {
    jvmToolchain(25)
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    debug.set(true)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(group.toString(), "noob-httpserver", version.toString())

    pom {
        name = "noob-httpserver"
        description = "A simple HTTP server library"
        url = "https://github.com/minthem/noob-httpserver"

        licenses {
            license {
                name = "The MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }

        developers {
            developer {
                id = "minthem"
                email = "114272528+minthem@users.noreply.github.com"
            }
        }

        scm {
            connection = "scm:git:https://github.com/minthem/noob-httpserver.git"
            developerConnection = "scm:git:https://github.com/minthem/noob-httpserver.git"
            url = "https://github.com/minthem/noob-httpserver"
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("GPG_PRIVATE_KEY"),
        System.getenv("GPG_PASSPHRASE"),
    )
}
