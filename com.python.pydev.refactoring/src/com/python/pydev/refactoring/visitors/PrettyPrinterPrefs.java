/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.util.HashMap;
import java.util.Map;

public class PrettyPrinterPrefs {

    private String newLine;
    private Map<String,String> tokReplacement = new HashMap<String, String>();

    public PrettyPrinterPrefs(String newLine) {
        this.newLine = newLine;
    }

    public String getNewLine() {
        return newLine;
    }

    public String getIndent() {
        return "    ";
    }

    public void setSpacesAfterComma(int i) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(',');
        for (int j = 0; j < i; j++) {
            buffer.append(' ');
        }
        this.tokReplacement.put(",", buffer.toString());
    }

    public String getReplacement(String tok) {
        String r = tokReplacement.get(tok);
        if(r == null){
            return tok;
        }
        return r;
    }
}
