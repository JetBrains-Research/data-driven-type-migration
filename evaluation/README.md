## Evaluation CLI Runner

- Run the script via:

```
./gradlew :evaluation:cli -PsourceProjectsPath=path/to/projects/for/evaluation -PjdkPath=path/to/jdk8
```

- Gradle task arguments:
    - `-PsourceProjectsPath`: path to the directory with cloned and prepared projects for evaluation. The plugin expects
      that at least one `.java` file will contain `<caret>` tag on some type element to extract the migrating usages
      from it.
    - `-PjdkPath`: path to the installed JDK 1.8. Usually, it should be something
      like `/usr/lib/jvm/java-8-openjdk-amd64`