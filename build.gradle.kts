import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests

plugins {
    kotlin("multiplatform") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
}

group = "com.chh2000day.app"
version = "0.1.0"


repositories {
    mavenCentral()
}

afterEvaluate {
    //Write version info on project configuration
    val versionFile = File(projectDir.absolutePath + "/src/commonMain/kotlin/com/chh2000day/fanboxd/Version.kt")
    versionFile.writeText(
        """
package com.chh2000day.fanboxd
object Version {
    const val versionName="$version"
}""".trimIndent()
    )
}

kotlin {
    val targets = mutableListOf<KotlinNativeTargetWithHostTests>()
    targets.add(linuxX64())
    targets.add(mingwX64())
    targets.forEach() { target ->
        target.apply {
            binaries {
                executable {
                    entryPoint = "com.chh2000day.fanboxd.main"
                }
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio:3.3.0")
                implementation("io.ktor:ktor-client-core:2.2.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0-RC")
                implementation("io.github.microutils:kotlin-logging:3.0.5")
            }
        }
        val commonTest by getting
        val linuxX64Main by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-curl:2.2.2")
            }
        }
        val linuxX64Test by getting
        val mingwX64Main by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-winhttp:2.2.2")
//                implementation("io.ktor:ktor-client-curl:2.2.2")
            }
        }
        val mingwX64Test by getting
    }
}
