/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.cadixdev.gradle.licenser.LicenseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application

    kotlin("jvm")
    kotlin("plugin.serialization")

    id("com.google.devtools.ksp")
    id("com.github.jakemarsden.git-hooks")
    id("com.github.johnrengelman.shadow")
    id("io.gitlab.arturbosch.detekt")
    id("com.expediagroup.graphql")
    id("org.cadixdev.licenser")
}

group = "org.quiltmc.community"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    google()

    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }

    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net")
    }

    maven {
        name = "QuiltMC (Releases)"
        url = uri("https://maven.quiltmc.org/repository/release/")
    }

    maven {
        name = "QuiltMC (Snapshots)"
        url = uri("https://maven.quiltmc.org/repository/snapshot/")
    }

    maven {
        name = "JitPack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    detektPlugins(libs.detekt)

    ksp(libs.kordex.annotationProcessor)

    implementation(libs.kmongo)
    implementation(libs.rgxgen)

    implementation(libs.kordex.annotations)
    implementation(libs.kordex.core)
    implementation(libs.kordex.mappings)
    implementation(libs.kordex.phishing)

    implementation(libs.commons.text)
    implementation(libs.jansi)
    implementation(libs.logback)
    implementation(libs.logging)
    implementation(libs.groovy)

    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kx.ser)
    implementation(libs.graphql)
}

graphql {
    client {
        sdlEndpoint = "https://docs.github.com/public/schema.docs.graphql"
        packageName = "quilt.ghgen"
        serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.KOTLINX
    }

}

configurations.all {
    resolutionStrategy.cacheDynamicVersionsFor(10, "seconds")
    resolutionStrategy.cacheChangingModulesFor(10, "seconds")
}

application {
    // This is deprecated, but the Shadow plugin requires it
    mainClassName = "org.quiltmc.community.AppKt"
}

gitHooks {
    setHooks(
        mapOf("pre-commit" to "updateLicense detekt")
    )
}

tasks.withType<KotlinCompile> {
    // Current LTS version of Java
    kotlinOptions.jvmTarget = "17"

    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "org.quiltmc.community.AppKt"
        )
    }
}

// add detekt checks when building for testing (in the IDE and such)
tasks.getByName("classes").dependsOn("detekt")

java {
    // Current LTS version of Java
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

detekt {
    buildUponDefaultConfig = true
    config = rootProject.files("detekt.yml")
}

// Credit to ZML for this workaround.
// https://github.com/CadixDev/licenser/issues/6#issuecomment-817048318
extensions.configure(LicenseExtension::class.java) {
    exclude {
        it.file.startsWith(buildDir)
    }
}

sourceSets {
    main {
        java {
            srcDir(file("$buildDir/generated/ksp/main/kotlin/"))
        }
    }

    test {
        java {
            srcDir(file("$buildDir/generated/ksp/test/kotlin/"))
        }
    }
}
