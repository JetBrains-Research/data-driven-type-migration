package org.example;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class FileToPathTest {
    public Path openFile() {
        Path file = Paths.get("file.txt");
        File another_file = new File("files", "file.txt");
        if (Files.exists(file)) {
            System.out.print(file.getFileName());
            if (file.toFile() == null) {
                return null;
            }
        }
        boolean canWriteToFile = Files.isWritable(file);
        Path f1 = file;
        System.out.print(func(f1));
        return f1;
    }

    public String func(Path f) {
        return f.toAbsolutePath().toString();
    }
}