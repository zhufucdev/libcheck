import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
}

group = "com.sqlmasters"
version = "1.0-SNAPSHOT"

kotlin {
    jvm("desktop")
    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.serialzation.json)
        }

        desktopMain.dependencies {
            implementation(project(":stub-jvm"))
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlin.reflect)
            implementation(libs.apache.commonsio)
            implementation(libs.mpfilepicker)
            implementation(libs.nativeparameteraccess)
            implementation(libs.javakeyring)
            implementation(libs.crunch)

            implementation(libs.protobuf.java)
            implementation(libs.protobuf.kotlin)
            implementation(libs.grpc.kotlin.stub)
            implementation(libs.grpc.stub)
            implementation(libs.grpc.protobuf)
            implementation(libs.grpc.netty)
            if (JavaVersion.current().isJava9Compatible) {
                // Workaround for @javax.annotation.Generated
                implementation(libs.javax.annotation)
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            modules("java.compiler", "java.instrument", "jdk.security.auth", "jdk.unsupported")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "LibCheck"
            packageVersion = "1.0.0"
            jvmArgs(
                "-Dapple.awt.application.appearance=system"
            )
            macOS {
                iconFile.set(project.file("icons/LibCheck.icns"))
            }
            windows {
                iconFile.set(project.file("icons/LibCheck.ico"))
            }
            linux {
                iconFile.set(project.file("icons/LibCheck.png"))
            }
        }
    }
}

compose.resources {
    packageOfResClass = "resources"
    generateResClass = always
}
