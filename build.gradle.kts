group = "org.jetbrains.research"
version = "1.0-SNAPSHOT"


plugins {
    java
    id("org.jetbrains.intellij") version "0.7.2"
}

intellij {
    type = "IC"
    version = "2020.3.2"
    setPlugins("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson", "gson", "2.8.6")
}

tasks.withType<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask>()
    .forEach { it.enabled = false }