package org.example;

import java.io.File;

class FileToPathTest {
    public File openFile() {
        F<caret>ile file = new File("file.txt");
        File another_file = new File("files", "file.txt");
        if (file.exists()) {
            System.out.print(file.getName());
            if (file == null) {
                return null;
            }
        }
        boolean canWriteToFile = file.canWrite();
        File f1 = file;
        System.out.print(func(f1));
        return f1;
    }

    public String func(File f) {
        return f.getAbsolutePath();
    }
}