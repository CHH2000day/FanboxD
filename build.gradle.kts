/*
 *    Copyright 2023 Rengesou(CHH2000day)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */


import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests

plugins {
    kotlin("multiplatform") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
}

group = "com.chh2000day.app"
version = "0.3.3"


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
                implementation("io.ktor:ktor-client-core:2.2.3")
                implementation("io.ktor:ktor-client-content-negotiation:2.2.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0-RC")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("co.touchlab:kermit:1.2.2")
            }
        }
        val commonTest by getting
        val linuxX64Main by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-curl:2.2.3")
            }
        }
        val linuxX64Test by getting
        val mingwX64Main by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-winhttp:2.2.3")
//                implementation("io.ktor:ktor-client-curl:2.2.2")
            }
        }
        val mingwX64Test by getting
    }
}
