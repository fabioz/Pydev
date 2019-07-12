/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * Holds the preferences for pretty printing.
 */
public class PrettyPrinterPrefsV2 implements IPrettyPrinterPrefs {

    private String newLine;
    private String spacesBeforeComment = "";
    private Map<String, String> tokReplacement = new HashMap<String, String>();
    private int linesAfterMethod = 0;
    private int linesAfterClass = 0;
    private int linesAfterSuite = 0;
    private String indent;
    private IGrammarVersionProvider versionProvider;

    public PrettyPrinterPrefsV2(String newLine, String indent, IGrammarVersionProvider versionProvider) {
        this.newLine = newLine;
        this.indent = indent;
        Assert.isNotNull(versionProvider);
        this.tokReplacement.put("def", "def ");
        this.tokReplacement.put("class", "class ");
        this.tokReplacement.put("if", "if ");
        this.tokReplacement.put("elif", "elif ");
        this.tokReplacement.put("in", " in ");
        this.tokReplacement.put("as", " as ");
        this.tokReplacement.put("yield", "yield ");
        this.tokReplacement.put("await", "await ");
        this.tokReplacement.put("from", "from ");
        this.tokReplacement.put("del", "del ");
        this.tokReplacement.put("assert", "assert ");
        this.tokReplacement.put("while", "while ");
        this.tokReplacement.put("global", "global ");
        this.tokReplacement.put("with", "with ");
        this.versionProvider = versionProvider;
    }

    @Override
    public int getGrammarVersion() throws MisconfigurationException {
        return versionProvider.getGrammarVersion();
    }

    @Override
    public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions() throws MisconfigurationException {
        return versionProvider.getAdditionalGrammarVersions();
    }

    public final String[] boolOperatorMapping = new String[] { "<undef>", "and", "or", };

    public final String[] unaryopOperatorMapping = new String[] { "<undef>", "~", "not", "+", "-", };

    public final String[] operatorMapping = new String[] { "<undef>", "+", "-", "*", "/", "%", "**", "<<", ">>", "|",
            "^", "&", "//", "@" };

    public final String[] augOperatorMapping = new String[] { "<undef>", "+=", "-=", "*=", "/=", "%=", "**=", "<<=",
            ">>=", "|=", "^=", "&=", "//=", "@=" };

    public static final String[] cmpop = new String[] { "<undef>", "==", "!=", "<", "<=", ">", ">=", "is", "is not",
            "in", "not in", };

    @Override
    public String getBoolOperatorMapping(int op) {
        return " " + boolOperatorMapping[op] + " ";
    }

    @Override
    public String getOperatorMapping(int op) {
        return " " + operatorMapping[op] + " ";
    }

    @Override
    public String getUnaryopOperatorMapping(int op) {
        String str = unaryopOperatorMapping[op];
        if (str.equals("not")) {
            return str + " ";
        }
        return str;
    }

    @Override
    public String getAugOperatorMapping(int op) {
        return " " + augOperatorMapping[op] + " ";
    }

    @Override
    public String getCmpOp(int op) {
        return " " + cmpop[op] + " ";
    }

    @Override
    public String getNewLine() {
        return newLine;
    }

    @Override
    public String getIndent() {
        return indent;
    }

    @Override
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

    @Override
    public String getReplacement(String tok) {
        String r = tokReplacement.get(tok);
        if (r == null) {
            return tok;
        }
        return r;
    }

    //spaces before comment
    public void setSpacesBeforeComment(int i) {
        spacesBeforeComment = createSpacesStr(i, "");
    }

    @Override
    public String getSpacesBeforeComment() {
        return spacesBeforeComment;
    }

    //lines after method
    @Override
    public void setLinesAfterMethod(int i) {
        linesAfterMethod = i;
    }

    @Override
    public int getLinesAfterMethod() {
        return linesAfterMethod;
    }

    //lines after class
    public void setLinesAfterClass(int i) {
        linesAfterClass = i;
    }

    @Override
    public int getLinesAfterClass() {
        return linesAfterClass;
    }

    //lines after any suite (if, for, etc)
    public void setLinesAfterSuite(int i) {
        linesAfterSuite = i;
    }

    @Override
    public int getLinesAfterSuite() {
        return linesAfterSuite;
    }

    @Override
    public String getAssignPunctuation() {
        return " = ";
    }

}
