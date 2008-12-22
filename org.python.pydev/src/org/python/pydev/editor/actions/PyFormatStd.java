/*
 * Created on Feb 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.prettyprinter.IFormatter;
import org.python.pydev.plugin.PyCodeFormatterPage;

/**
 * @author Fabio Zadrozny
 */
public class PyFormatStd extends PyAction implements IFormatter {

    /**
     * Class that defines the format standard to be used
     *
     * @author Fabio
     */
    public static class FormatStd {
        
        /**
         * Defines whether spaces should be added after a comma
         */
        public boolean spaceAfterComma;

        /**
         * Defines whether ( and ) should have spaces
         */
        public boolean parametersWithSpace;
        
        /**
         * Defines whether = should be spaces surrounded when inside of a parens (function call)
         * (as well as others related: *= +=, -=, !=, ==, etc).
         */
        public boolean assignWithSpaceInsideParens;
        
        /**
         * Defines whether operators should be spaces surrounded:
         * + - * / // ** | & ^ ~ =
         */
        public boolean operatorsWithSpace;
    }

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        try {
            IFormatter participant = (IFormatter) ExtensionHelper.getParticipant(ExtensionHelper.PYDEV_FORMATTER);
            if (participant == null) {
                participant = this;
            }
            PySelection ps = new PySelection(getTextEditor());
            IDocument doc = ps.getDoc();

            int startLine = ps.getStartLineIndex();
            PyEdit pyEdit = getPyEdit();
            if (ps.getTextSelection().getLength() == 0) {
                participant.formatAll(doc, pyEdit);
            } else {
                participant.formatSelection(doc, startLine, ps.getEndLineIndex(), pyEdit, ps);
            }

            if (startLine >= doc.getNumberOfLines()) {
                startLine = doc.getNumberOfLines() - 1;
            }
            TextSelection sel = new TextSelection(doc, doc.getLineOffset(startLine), 0);
            getTextEditor().getSelectionProvider().setSelection(sel);

        } catch (Exception e) {
            beep(e);
        }
    }
    

    /**
     * Formats the given selection
     * @see IFormatter
     */
    public void formatSelection(IDocument doc, int startLine, int endLineIndex, IPyEdit edit, PySelection ps) {
//        Formatter formatter = new Formatter();
//        formatter.formatSelection(doc, startLine, endLineIndex, edit, ps);
        
        try {
            IRegion start = doc.getLineInformation(startLine);
            IRegion end = doc.getLineInformation(endLineIndex);
        
            int iStart = start.getOffset();
            int iEnd = end.getOffset() + end.getLength();
        
            String d = doc.get(iStart, iEnd - iStart);
            FormatStd formatStd = getFormat();
            String formatted = formatStr(d, formatStd);
        
            doc.replace(iStart, iEnd - iStart, formatted);
        
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Formats the whole document
     * @see IFormatter
     */
    public void formatAll(IDocument doc, IPyEdit edit) {
//        Formatter formatter = new Formatter();
//        formatter.formatAll(doc, edit);
        
        String d = doc.get();
        FormatStd formatStd = getFormat();
        String formatted = formatStr(d, formatStd);
        doc.set(formatted);
    }

    
    /**
     * @return the format standard that should be used to do the formatting
     */
    private FormatStd getFormat() {
        FormatStd formatStd = new FormatStd();
        formatStd.assignWithSpaceInsideParens = PyCodeFormatterPage.useAssignWithSpacesInsideParenthesis();
        formatStd.operatorsWithSpace = PyCodeFormatterPage.useOperatorsWithSpace();
        formatStd.parametersWithSpace = PyCodeFormatterPage.useSpaceForParentesis();
        formatStd.spaceAfterComma = PyCodeFormatterPage.useSpaceAfterComma();
        return formatStd;
    }

    /**
     * This method formats a string given some standard.
     * 
     * @param str the string to be formatted
     * @param std the standard to be used
     * @return a new (formatted) string
     */
    public String formatStr(String str, FormatStd std) {
        return formatStr(str, std, 0);
    }
    
    /**
     * This method formats a string given some standard.
     * 
     * @param str the string to be formatted
     * @param std the standard to be used
     * @param parensLevel the level of the parenthesis available.
     * @return a new (formatted) string
     */
    private String formatStr(String str, FormatStd std, int parensLevel) {
        char[] cs = str.toCharArray();
        FastStringBuffer buf = new FastStringBuffer();
        ParsingUtils parsingUtils = ParsingUtils.create(cs);
        char lastChar = '\0';
        for (int i = 0; i < cs.length; i++) {
            char c = cs[i];

            switch(c){
                case '\'':
                case '"':
                  //ignore comments or multiline comments...
                    i = parsingUtils.eatLiterals(buf, i);
                    break;

                    
                case '#':
                    i = parsingUtils.eatComments(buf, i);
                    break;

                case ',':
                    i = formatForComma(std, cs, buf, i);
                    break;

                case '(':
                    i = formatForPar(parsingUtils, cs, i, std, buf, parensLevel+1);
                    break;
                    
                    
                //Things to treat:
                //+, -, *, /, %
                //** // << >>
                //<, >, !=, <>, <=, >=, //=, *=, /=,
                //& ^ ~ |
                case '*':
                    //for *, we also need to treat when it's used in varargs, kwargs and list expansion
                    boolean isOperator = false;
                    for(int j=buf.length()-1;j>=0;j--){
                        char localC = buf.charAt(j);
                        if(Character.isWhitespace(localC)){
                            continue;
                        }
                        if(localC == '(' || localC == ','){
                            //it's not an operator, but vararg. kwarg or list expansion
                            break; //break for
                        }
                        if(Character.isJavaIdentifierPart(localC)){
                            //ok, there's a chance that it can be an operator, but we still have to check
                            //the chance that it's a wild import
                            FastStringBuffer localBufToCheckWildImport = new FastStringBuffer();
                            while(Character.isJavaIdentifierPart(localC)){
                                localBufToCheckWildImport.append(localC);
                                j--;
                                if(j < 0){
                                    break; //break while
                                }
                                localC = buf.charAt(j);
                            }
                            if(!localBufToCheckWildImport.reverse().toString().equals("import")){
                                isOperator = true;
                            }
                            break;
                        }
                    }
                    if(!isOperator){
                        buf.append('*');
                        break;//break switch
                    }
                    //Otherwise, FALLTHROUGH
                    
                case '+':
                case '-':
                    
                    if(c == '-' || c == '+'){ // could also be *
                        
                        //handle exponentials correctly: e.g.: 1e-6 cannot have a space
                        FastStringBuffer localBufToCheckNumber = new FastStringBuffer();
                        for(int j=buf.length()-1;j>=0;j--){
                            char localC = buf.charAt(j);
                            if(Character.isJavaIdentifierPart(localC)){
                                localBufToCheckNumber.append(localC);
                            }else{
                                break;//break for
                            }
                        }
                        boolean isExponential = true;;
                        String partialNumber = localBufToCheckNumber.reverse().toString();
                        int partialLen = partialNumber.length();
                        if(partialLen < 2 || !Character.isDigit(partialNumber.charAt(0))){
                            //at least 2 chars: the number and the 'e'
                            isExponential = false;
                        }else{
                            //first char checked... now, if the last is an 'e', we must leave it together no matter what
                            if(partialNumber.charAt(partialLen-1) != 'e'){
                                isExponential = false;
                            }
                        }
                        if(isExponential){
                            buf.append(c);
                            break;//break switch
                        }
                        //Otherwise, FALLTHROUGH
                    }
                    
                case '/':
                case '%':
                case '<':
                case '>':
                case '!':
                case '&':
                case '^':
                case '~':
                case '|':
                    
                    i = handleOperator(std, cs, buf, parsingUtils, i, c);
                    c = cs[i];
                    break;

                    
                //check for = and == (other cases that have an = as the operator should already be treated)
                case '=':
                    if(i < cs.length-1 && cs[i+1] == '='){
                        //if == handle as if a regular operator
                        i = handleOperator(std, cs, buf, parsingUtils, i, c);
                        c = cs[i];
                        break;
                    }
                        
                    while(buf.length() > 0 && buf.lastChar() == ' '){
                        buf.deleteLast();
                    }
                    
                    boolean surroundWithSpaces = std.operatorsWithSpace;
                    if(parensLevel > 0){
                        surroundWithSpaces = std.assignWithSpaceInsideParens;
                    }
                    
                    //add space before
                    if(surroundWithSpaces){
                        buf.append(' ');
                    }
                    
                    //add the operator and the '='
                    buf.append('=');
                    
                    //add space after
                    if(surroundWithSpaces){
                        buf.append(' ');
                    }
                    
                    i = parsingUtils.eatWhitespaces(null, i+1);
                    break;
                    
                default:
                    if (c == '\r' || c == '\n') {
                        if (lastChar == ',' && std.spaceAfterComma && buf.lastChar() == ' ') {
                            buf.deleteLast();
                        }
                    }
                    buf.append(c);
                    
            }
            lastChar = c;

        }
        return buf.toString();
    }


    /**
     * Handles having an operator
     * 
     * @param std the coding standard to be used
     * @param cs the contents of the string
     * @param buf the buffer where the contents should be added 
     * @param parsingUtils helper to get the contents
     * @param i current index
     * @param c current char
     * @return the new index after handling the operator
     */
    private int handleOperator(FormatStd std, char[] cs, FastStringBuffer buf, ParsingUtils parsingUtils, int i, char c) {
        //let's discover if it's an unary operator (~ + -)
        
        boolean isUnary = false;
        if(c == '~' || c == '+' || c == '-'){
            //could be an unary operator...
            isUnary = buf.length() == 0;
        }
        
        
        while(buf.length() > 0 && buf.lastChar() == ' '){
            buf.deleteLast();
        }
        
        boolean surroundWithSpaces = std.operatorsWithSpace;
        
        //add space before
        if(!isUnary && surroundWithSpaces){
            buf.append(' ');
        }
        
        char localC = c;
        boolean backOne = true;
        while(isOperatorPart(localC)){
            buf.append(localC);
            i++;
            if(i == cs.length){
                break;
            }
            localC = cs[i];
            if(localC == '='){
                //when we get to an assign, we have found a full stmt (with assign) -- e.g.: a \\=  a += a ==
                buf.append(localC);
                backOne = false;
                break;
            }
        }
        if(backOne){
            i--;
        }
        
        //add space after only if it's not unary
        if(!isUnary && surroundWithSpaces){
            buf.append(' ');
        }
       
        i = parsingUtils.eatWhitespaces(null, i+1);
        return i;
    }


    /**
     * @param c the char to be checked
     * @return true if the passed char is part of an operator
     */
    private boolean isOperatorPart(char c) {
        switch(c){
        case '+':
        case '-':
        case '*':
        case '/':
        case '%':
        case '<':
        case '>':
        case '!':
        case '&':
        case '^':
        case '~':
        case '|':
        case '=':
            return true;
        }
        return false;
    }


    /**
     * Formats the contents for when a parenthesis is found (so, go until the closing parens and format it accordingly)
     * @param cs
     * @param i
     * @param parensLevel 
     */
    private int formatForPar(ParsingUtils parsingUtils, char[] cs, int i, FormatStd std, FastStringBuffer buf, int parensLevel) {
        char c = ' ';
        FastStringBuffer locBuf = new FastStringBuffer();

        int j = i + 1;
        while (j < cs.length && (c = cs[j]) != ')') {

            j++;

            if (c == '\'' || c == '"') { //ignore comments or multiline comments...
                j = parsingUtils.eatLiterals(locBuf, j - 1) + 1;

            } else if (c == '#') {
                j = parsingUtils.eatComments(locBuf, j - 1) + 1;

            } else if (c == '(') { //open another par.
                j = formatForPar(parsingUtils, cs, j - 1, std, locBuf, parensLevel+1) + 1;

            } else {
                locBuf.append(c);
                
            }
        }

        if (c == ')') {

            //Now, when a closing parens is found, let's see the contents of the line where that parens was found
            //and if it's only whitespaces, add all those whitespaces (to handle the following case:
            //a(a, 
            //  b
            //   ) <-- we don't want to change this one.
            char c1;
            FastStringBuffer buf1 = new FastStringBuffer();

            if (locBuf.indexOf('\n') != -1 || locBuf.indexOf('\r') != -1) {
                for (int k = locBuf.length(); k > 0 && (c1 = locBuf.charAt(k - 1)) != '\n' && c1 != '\r'; k--) {
                    buf1.insert(0, c1);
                }
            }

            String formatStr = formatStr(trim(locBuf).toString(), std, parensLevel);
            FastStringBuffer formatStrBuf = trim(new FastStringBuffer(formatStr, 10));

            String closing = ")";
            if (buf1.length() > 0 && PySelection.containsOnlyWhitespaces(buf1.toString())) {
                formatStrBuf.append(buf1);

            } else if (std.parametersWithSpace) {
                closing = " )";
            }

            if (std.parametersWithSpace) {
                if (formatStrBuf.length() == 0) {
                    buf.append("()");

                } else {
                    buf.append("( ");
                    buf.append(formatStrBuf);
                    buf.append(closing);
                }
            } else {
                buf.append('(');
                buf.append(formatStrBuf);
                buf.append(closing);
            }
            return j;
        } else {
            return i;
        }
    }

    /**
     * We just want to trim whitespaces, not newlines!
     * @param locBuf the buffer to be trimmed
     * @return the same buffer passed as a parameter
     */
    private FastStringBuffer trim(FastStringBuffer locBuf) {
        while (locBuf.length() > 0 && locBuf.firstChar() == ' ') {
            locBuf.deleteCharAt(0);
        }
        while (locBuf.length() > 0 && locBuf.lastChar() == ' ') {
            locBuf.deleteLast();
        }
        return locBuf;
    }

    /**
     * When a comma is found, it's formatted accordingly (spaces added after it).
     * 
     * @param std the coding standard to be used
     * @param cs the contents of the document to be formatted
     * @param buf the buffer where the comma should be added
     * @param i the current index
     * @return the new index on the original doc.
     */
    private int formatForComma(FormatStd std, char[] cs, FastStringBuffer buf, int i) {
        while (i < cs.length - 1 && (cs[i + 1]) == ' ') {
            i++;
        }

        if (std.spaceAfterComma) {
            buf.append(", ");
        } else {
            buf.append(',');
        }
        return i;
    }
}
