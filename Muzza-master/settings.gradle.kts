@file:Suppress("UnstableApiUsage")
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "Muzza"
include(":app")
include(":innertube")
include(":kugou")
include(":lrclib")
