/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public final class FileUtils {
    private static final int BUFFER_SIZE = 1024;

    private FileUtils() {
    }

    /**
     * Read the whole content of a file.
     * 
     * @param file
     * @return the file's content
     * @throws IOException
     */
    public static String read(File file) throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(file));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[BUFFER_SIZE];
            int len;
            while ((len = reader.read(buffer, 0, BUFFER_SIZE)) != -1) {
                builder.append(buffer, 0, len);
            }
            return builder.toString();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * Strips away the extension of the given filename.
     * 
     * @param filename
     * @return
     */
    public static String stripExtension(String filename) {
        int point = filename.lastIndexOf(".");

        return point >= 0 ? filename.substring(0, point) : filename;
    }
}
