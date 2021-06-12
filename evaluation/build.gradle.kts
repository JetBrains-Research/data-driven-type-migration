plugins {
    java
    id("org.jetbrains.intellij")
}

intellij {
    type = "IC"
    version = "2020.3.2"
    setPlugins("java")
}

group = rootProject.group
version = rootProject.version



dependencies {
    implementation(project(":plugin"))
    compileOnly("commons-cli:commons-cli:1.4")
}

tasks {
    runIde {
        val sourceProjectsPath: String? by project
        val jdkPath: String? by project
        args = listOfNotNull(
            "evaluation",
            sourceProjectsPath?.let { "--src-projects-dir=$it" },
            jdkPath?.let { "--jdk-path=$it" }
        )
        jvmArgs = listOf("-Djava.awt.headless=true")
        standardInput = System.`in`
        standardOutput = System.`out`
    }

    register("cli") {
        dependsOn("runIde")
    }
}

tasks.withType<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask>()
    .forEach { it.enabled = false }