/*
 * Created on Feb 11, 2006
 */
package org.python.pydev.parser.prettyprinter;

import java.util.HashMap;
import java.util.Map;

public class PrettyPrinterPrefs {

    private String newLine;
    private String spacesBeforeComment="";
    private Map<String,String> tokReplacement = new HashMap<String, String>();

    public PrettyPrinterPrefs(String newLine) {
        this.newLine = newLine;
        tokReplacement.put("elif", "elif ");
    }

    public String getNewLine() {
        return newLine;
    }

    public String getIndent() {
        return "    ";
    }

    public void setSpacesAfterComma(int i) {
        this.tokReplacement.put(",", createSpacesStr(i, ","));
    }

    private String createSpacesStr(int i, String startingWith) {
        StringBuffer buffer = new StringBuffer();
        if(startingWith != null){
            buffer.append(startingWith);
        }
        for (int j = 0; j < i; j++) {
            buffer.append(' ');
        }
        return buffer.toString();
    }

    public String getReplacement(String tok) {
        String r = tokReplacement.get(tok);
        if(r == null){
            return tok;
        }
        return r;
    }

    public String getSpacesBeforeComment() {
        return spacesBeforeComment;
    }
    
    public void setSpacesBeforeComment(int i) {
        spacesBeforeComment = createSpacesStr(i, null);
    }
}
