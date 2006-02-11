/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

public class PrettyPrinterPrefs {

    private String newLine;

    public PrettyPrinterPrefs(String newLine) {
        this.newLine = newLine;
    }

    public String getNewLine() {
        return newLine;
    }

    public String getIndent() {
        return "    ";
    }

}
