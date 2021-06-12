group = "org.jetbrains.research"
version = "1.0-SNAPSHOT"

plugins {
    java
    id("org.jetbrains.intellij") version "0.7.2"
}

allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
}