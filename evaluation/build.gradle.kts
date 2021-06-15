plugins {
    java
    id("org.jetbrains.intellij")
}

intellij {
    type = "IC"
    version = "2020.3.2"
    setPlugins("java")
}

group = "org.jetbrains.research"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(rootProject)
    compileOnly("commons-cli:commons-cli:1.4")
}

tasks {
    runIde {
        val sourceProjectsPath: String? by project
        val jdkPath: String? by project
        val offset: String? by project
        args = listOfNotNull(
            "evaluation",
            sourceProjectsPath?.let { "--src-projects-dir=$it" },
            jdkPath?.let { "--jdk-path=$it" },
            offset?.let { "--offset=$it" }
        )
        jvmArgs = listOf("-Djava.awt.headless=true")
        standardInput = System.`in`
        standardOutput = System.`out`
    }

    register("cli") {
        dependsOn("runIde")
    }
}