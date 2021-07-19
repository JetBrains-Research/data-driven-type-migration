group = rootProject.group
version = rootProject.group

plugins {
    java
    id("org.jetbrains.intellij")
}

intellij {
    type = "IC"
    version = "2021.1"
    setPlugins("java", "git4idea")
}

dependencies {
    implementation("com.google.code.gson", "gson", "2.8.6")
}

tasks.withType<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask>()
    .forEach { it.enabled = false }

tasks {
    test {
        systemProperty("jdk.home.path", System.getProperty("jdk.home.path"))
    }
}