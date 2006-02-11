/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;
import java.io.Writer;

public class WriteState {

    private Writer writer;
    private PrettyPrinterPrefs prefs;
    private StringBuffer indentation = new StringBuffer();
    
    public WriteState(Writer writer, PrettyPrinterPrefs prefs) {
        this.writer = writer;
        this.prefs = prefs;
    }

    public void indent() {
        indentation.append(prefs.getIndent());
    }

    public void dedent() {
        int len = indentation.length();
        indentation.delete(len-prefs.getIndent().length(), len);
    }

    public void writeIndent() throws IOException {
        writer.write(indentation.toString());
    }
    
    public void writeNewLine() throws IOException {
        writer.write(prefs.getNewLine());
    }

    public void writeIndent(int i) throws IOException {
        writeIndent();
        String indent = prefs.getIndent();
        for (int j = 0; j < i; j++) {
            writer.write(indent);
        }
        
    }


}
