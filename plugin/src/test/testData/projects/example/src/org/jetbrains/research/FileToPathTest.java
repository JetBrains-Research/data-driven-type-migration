package org.jetbrains.research;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

class FileToPathTest {
    public File openFile() {
        F<caret>ile file = new File("file.txt", "haha");
        if (checkIfExists(file)) {
            System.out.print(file.getName());
        } else {
            if (file == null) {
                return null;
            }
        }
        boolean canWriteToFile = file.canWrite();
        File f1 = file;
        checkMigrationWhenMethodFromAnotherScopeIsUsed(f1);
        return f1;
    }

    public void main() {
        File fileResult = this.openFile();
        boolean wasCreated = openFile().mkdir();
    }

    public static boolean checkIfExists(File fileParam) {
        return fileParam.exists();
    }

    public void checkMigrationWhenMethodFromAnotherScopeIsUsed(File file) {
        SomeHelperClass.checkIfExists(file);
        System.out.print(file);
    }
}
