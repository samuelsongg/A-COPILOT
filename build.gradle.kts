buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://plugins.gradle.org/m2/")

        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
    dependencies {
        classpath("com.google.gms:google-services:4.4.1")
        classpath("io.github.c0nnor263:obfustring-plugin:12.0.0")
        classpath("com.github.CodingGay:BlackObfuscator-ASPlugin:3.9")
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false

    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.1" apply false
}