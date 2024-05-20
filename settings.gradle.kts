pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.aliyun.com/repository/public/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://redempt.dev")
        google()
    }
}

rootProject.name = "libcheck"
include(":composeApp")
include(":proto")
include(":stub-jvm")
