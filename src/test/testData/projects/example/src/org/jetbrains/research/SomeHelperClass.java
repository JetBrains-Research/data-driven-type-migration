package org.jetbrains.research;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

class SomeHelperClass {
    public void main() {
        File fileResult = this.openFile();
        boolean wasCreated = openFile().mkdir();
    }

    public static boolean checkIfExists(File fileParam) {
        return fileParam.exists();
    }
}