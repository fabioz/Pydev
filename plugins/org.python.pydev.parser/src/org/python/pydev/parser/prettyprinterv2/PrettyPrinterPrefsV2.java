package org.python.pydev.parser.prettyprinterv2;

import java.util.HashMap;
import java.util.Map;

import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.parser.prettyprinter.IPrettyPrinterPrefs;

public class PrettyPrinterPrefsV2 implements IPrettyPrinterPrefs {

    private String newLine;
    private String spacesBeforeComment="";
    private Map<String,String> tokReplacement = new HashMap<String, String>();
    private int linesAfterMethod = 0;
    private int linesAfterClass = 0;
    private int linesAfterSuite=0;
    private String indent;
    
    

    public PrettyPrinterPrefsV2(String newLine, String indent) {
        this.newLine = newLine;
        this.indent = indent;
        this.tokReplacement.put("def", "def ");
        this.tokReplacement.put("class", "class ");
        this.tokReplacement.put("if", "if ");
        this.tokReplacement.put("elif", "elif ");
        this.tokReplacement.put("in", " in ");
        this.tokReplacement.put("as", " as ");
        this.tokReplacement.put("yield", "yield ");
        this.tokReplacement.put("del", "del ");
        this.tokReplacement.put("assert", "assert ");
    }
    
    
    public final String[] boolOperatorMapping = new String[] {
        "<undef>",
        "and",
        "or",
    };
    
    public final String[] unaryopOperatorMapping = new String[] {
        "<undef>",
        "~",
        "not",
        "+",
        "-",
    };

    public final String[] operatorMapping = new String[] {
        "<undef>",
        "+",
        "-",
        "*",
        "/",
        "%",
        "**",
        "<<",
        ">>",
        "|",
        "^",
        "&",
        "//",
    };
    
    public final String[] augOperatorMapping = new String[] {
        "<undef>",
        "+=",
        "-=",
        "*=",
        "/=",
        "%=",
        "**=",
        "<<=",
        ">>=",
        "|=",
        "^=",
        "&=",
        "//=",
    };
    
    public static final String[] cmpop = new String[] {
        "<undef>",
        "==",
        "!=",
        "<",
        "<=",
        ">",
        ">=",
        "is",
        "is not",
        "in",
        "not in",
    };

    
    public String getBoolOperatorMapping(int op) {
        return " "+boolOperatorMapping[op]+" ";
    }

    public String getOperatorMapping(int op) {
        return " "+operatorMapping[op]+" ";
    }

    public String getUnaryopOperatorMapping(int op) {
        String str = unaryopOperatorMapping[op];
        if(str.equals("not")){
            return str+" ";
        }
        return str;
    }
    
    public String getAugOperatorMapping(int op) {
        return " "+augOperatorMapping[op]+" ";
    }

    
    public String getCmpOp(int op) {
        return " "+cmpop[op]+" ";
    }

    public String getNewLine() {
        return newLine;
    }

    public String getIndent() {
        return indent;
    }

    public void setSpacesAfterComma(int i) {
        this.tokReplacement.put(",", createSpacesStr(i, ","));
    }
    
    
    //spaces after colon (dict, lambda)
    public void setSpacesAfterColon(int i) {
        this.tokReplacement.put(":", createSpacesStr(i, ":"));
    }
    

    private String createSpacesStr(int i, String startingWith) {
        FastStringBuffer buf = new FastStringBuffer(startingWith, i);
        buf.appendN(' ', i);
        return buf.toString();
    }

    @Override
    public void setReplacement(String original, String replacement) {
        this.tokReplacement.put(original, replacement);
    }

    
    public String getReplacement(String tok) {
        String r = tokReplacement.get(tok);
        if(r == null){
            return tok;
        }
        return r;
    }
    
    

    //spaces before comment
    public void setSpacesBeforeComment(int i) {
        spacesBeforeComment = createSpacesStr(i, null);
    }
    
    public String getSpacesBeforeComment() {
        return spacesBeforeComment;
    }
    
    

    

    //lines after method
    public void setLinesAfterMethod(int i) {
        linesAfterMethod = i;
    }
    
    public int getLinesAfterMethod(){
        return linesAfterMethod;
    }
    
    
    //lines after class
    public void setLinesAfterClass(int i) {
        linesAfterClass = i;
    }

    public int getLinesAfterClass() {
        return linesAfterClass;
    }

    
    //lines after any suite (if, for, etc)
    public void setLinesAfterSuite(int i) {
        linesAfterSuite = i;
    }
    public int getLinesAfterSuite() {
        return linesAfterSuite;
    }

    @Override
    public String getAssignPunctuation() {
        return " = ";
    }


}
