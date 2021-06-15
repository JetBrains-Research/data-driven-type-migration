package org.jetbrains.research;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

class FileToPathTest {
    public File openFile() {
        File file = new File("file.txt", "haha");
        if (file.exists()) {
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

    public void checkMigrationWhenMethodFromAnotherScopeIsUsed(File file) {
        SomeHelperClass.checkIfExists(file);
        System.out.print(file);
    }
}
