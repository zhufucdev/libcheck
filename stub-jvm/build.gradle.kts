plugins {
    id("java")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
}

group = "com.sqlmasters"
version = "1.0-SNAPSHOT"

dependencies {
    protobuf(project(":proto"))
    api(libs.kotlinx.coroutines.core)
    api(libs.protobuf.java)
    api(libs.protobuf.kotlin)
    api(libs.grpc.kotlin.stub)
    api(libs.grpc.stub)
    api(libs.grpc.protobuf)
    api(libs.grpc.netty)
    if (JavaVersion.current().isJava9Compatible) {
        // Workaround for @javax.annotation.Generated
        implementation(libs.javax.annotation)
        apiElements(libs.javax.annotation)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }
}

kotlin {
    jvmToolchain(17)
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        create("grpc") {
            artifact = libs.grpc.protoc.gen.java.get().toString()
        }
        create("grpckt") {
            artifact = libs.grpc.protoc.gen.kotlin.get().toString() + ":jdk8@jar"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}
