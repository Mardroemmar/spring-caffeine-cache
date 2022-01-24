plugins {
    java
    `java-library`
    jacoco
    signing
    alias(libs.plugins.testlog)
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.publishing)
    alias(libs.plugins.indra.publishing.sonatype)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.depman)
    kotlin("jvm") version libs.versions.kotlin.asProvider()
}

group = "dev.mardroemmar"
version = "1.0.0"
description = "A simple Caffeine caching library implementation."

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.kotlin.logging)

    api(libs.apiguardian)
    api(libs.caffeine)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.starter.cache)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.truth)
    testImplementation(libs.spring.boot.starter.test)
}

val signingKey = System.getenv("SIGNING_KEY")
val signingPassword = System.getenv("SIGNING_PASSWORD")
if (signingKey != null && signingPassword != null) {
    signing.useInMemoryPgpKeys(signingKey, signingPassword)
}

indra {
    github("Mardroemmar", "spring-caffeine-cache") {
        ci(true)
    }

    license {
        spdx("MPL-2.0")
        name("Mozilla Public License 2.0")
        url("https://www.mozilla.org/en-US/MPL/2.0/")
    }

    configurePublications {
        pom {
            developers {
                developer {
                    id.set("Proximyst")
                    name.set("Mariell Hoversholm")
                    timezone.set("Europe/Stockholm")
                }
            }
        }
    }
}

indraSonatype {
    this.useAlternateSonatypeOSSHost("s01")
}

testlogger {
    theme = com.adarshr.gradle.testlogger.theme.ThemeType.PLAIN_PARALLEL
}

configurations {
    testImplementation {
        exclude(group = "junit")
    }
}

tasks {
    withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        // This is what I use as a workaround to not being able to specify the Java version...
        // and is kinda necessary anyways.
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.javaParameters = true
    }

    javadoc {
        val opt = options as StandardJavadocDocletOptions
        opt.addStringOption("Xdoclint:none", "-quiet")

        opt.encoding("UTF-8")
        opt.charSet("UTF-8")
        doFirst {
            opt.links(
                "https://docs.oracle.com/en/java/javase/11/docs/api/",
            )
        }
    }
}
