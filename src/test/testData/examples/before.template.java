package org.jetbrains.research;

import java.io.File;
import java.nio.file.Path;


class FileToPathTest {
    public File openFile() {
        F<caret>ile file = new File("file.txt", "haha");
        if (checkIfExists(file)) {
            System.out.print(file.getName());
        }
        boolean canWriteToFile = file.canWrite();
        File f1 = file;
        return f1;
    }

    public void main() {
        File fileResult = this.openFile();
        boolean wasCreated = openFile().mkdir();
    }

    public static boolean checkIfExists(File fileParam) {
        return fileParam.exists();
    }
}
