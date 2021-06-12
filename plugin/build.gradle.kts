plugins {
    java
    id("org.jetbrains.intellij")
}

intellij {
    type = "IC"
    version = "2020.3.2"
    setPlugins("java", "git4idea")
}

group = rootProject.group
version = rootProject.group



dependencies {
    implementation("com.google.code.gson", "gson", "2.8.6")
}

tasks.withType<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask>()
    .forEach { it.enabled = false }