import com.google.protobuf.gradle.id

plugins {
    id("java")
    alias(libs.plugins.protobuf)
}

group = "com.sqlmasters"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(libs.protobuf.java)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.protobuf)
    if (JavaVersion.current().isJava9Compatible) {
        // Workaround for @javax.annotation.Generated
        implementation(libs.javax.annotation)
    }
}

tasks.test {
    useJUnitPlatform()
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        id("grpc") {
            artifact = libs.grpc.protoc.gen.get().toString()
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc") { }
            }
        }
    }
}