/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.prettyprinterv2.IFormatter;
import org.python.pydev.plugin.preferences.PyCodeFormatterPage;

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

        public boolean addNewLineAtEndOfFile;

        public boolean trimLines;
    }
    
    

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        try {
        	if(!canModifyEditor()){
        		return;
        	}

            PyEdit pyEdit = getPyEdit();
            PySelection ps = new PySelection(pyEdit);
            
            
            try{
                IRegion[] regionsToFormat = null;
                if(ps.getSelLength() > 0){
                    //Create a region with the full lines selected for the formatting.
                    IDocument doc = ps.getDoc();
                    IRegion start = doc.getLineInformation(ps.getStartLineIndex());
                    IRegion end = doc.getLineInformation(ps.getEndLineIndex());
                
                    int iStart = start.getOffset();
                    int iEnd = end.getOffset() + end.getLength();
                    regionsToFormat = new IRegion[]{new Region(iStart, iEnd-iStart)};
                }

                applyFormatAction(pyEdit, ps, regionsToFormat, true);
            }catch(SyntaxErrorException e){
                pyEdit.getStatusLineManager().setErrorMessage(e.getMessage());
            }

        } catch (Exception e) {
            beep(e);
        }
    }

    
    /**
     * This method applies the code-formatting to the document in the PySelection
     * 
     * @param pyEdit used to restore the selection
     * @param ps the selection used (contains the document that'll be changed)
     * @param regionsToFormat if null or empty, the whole document will be formatted, otherwise, only the passed ranges will
     * be formatted. 
     * @throws SyntaxErrorException 
     */
    public void applyFormatAction(PyEdit pyEdit, PySelection ps, IRegion[] regionsToFormat, boolean throwSyntaxError) throws BadLocationException, SyntaxErrorException {
        final IFormatter participant = getFormatter();
        final IDocument doc = ps.getDoc();
        final SelectionKeeper selectionKeeper = new SelectionKeeper(ps);
        
        DocumentRewriteSession session = null;
        try{
        
            if (regionsToFormat == null || regionsToFormat.length == 0) {
                if(doc instanceof IDocumentExtension4){
                    IDocumentExtension4 ext = (IDocumentExtension4) doc;
                    session = ext.startRewriteSession(DocumentRewriteSessionType.STRICTLY_SEQUENTIAL);
                }
                participant.formatAll(doc, pyEdit, true, throwSyntaxError);
            } else {
                if(doc instanceof IDocumentExtension4){
                    IDocumentExtension4 ext = (IDocumentExtension4) doc;
                    session = ext.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
                }
                participant.formatSelection(doc, regionsToFormat, pyEdit, ps);
            }

            //To finish, no matter what kind of formatting was done, check the end of line.
            FormatStd std = getFormat();
            if(std.addNewLineAtEndOfFile){
                try {
                    int len = doc.getLength();
                    char lastChar = doc.getChar(len-1);
                    if(len > 0){
                        if(lastChar != '\r' && lastChar != '\n'){
                            doc.replace(len, 0, PySelection.getDelimiter(doc));
                        }
                    }
                } catch (Throwable e) {
                    Log.log(e);
                }
            }

            
        }finally{
            if(session != null){
                ((IDocumentExtension4)doc).stopRewriteSession(session);
            }
        }

        
        selectionKeeper.restoreSelection(pyEdit.getSelectionProvider(), doc);
    }

    
    /**
     * @return the source code formatter to be used.
     */
    public IFormatter getFormatter() {
        IFormatter participant = (IFormatter) ExtensionHelper.getParticipant(ExtensionHelper.PYDEV_FORMATTER);
        if (participant == null) {
            participant = this;
        }
        return participant;
    }
    

    /**
     * Formats the given selection
     * @see IFormatter
     */
    public void formatSelection(IDocument doc, IRegion[] regionsForSave, IPyEdit edit, PySelection ps) {
//        Formatter formatter = new Formatter();
//        formatter.formatSelection(doc, startLine, endLineIndex, edit, ps);
        
        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<Tuple3<Integer, Integer, String>> replaces = new ArrayList();
        
        
        //Calculate all formatting to take place
        try {
            FormatStd formatStd = getFormat();
            
            for(IRegion r: regionsForSave){
                int iStart = r.getOffset();
                int iEnd = r.getOffset() + r.getLength();
                
                String d = doc.get(iStart, iEnd - iStart);
                String formatted = formatStr(d, formatStd, PySelection.getDelimiter(doc), false);
                replaces.add(new Tuple3<Integer, Integer, String>(iStart, iEnd - iStart, formatted));
            }
            
        } catch (BadLocationException e) {
            Log.log(e);
        }catch(SyntaxErrorException e){
            throw new RuntimeException(e);
        }
        
        
        //Apply the formatting from bottom to top (so that the indexes are still valid).
        Collections.reverse(replaces);
        for (Tuple3<Integer, Integer, String> tup : replaces) {
            try {
                doc.replace(tup.o1, tup.o2, tup.o3);
            } catch (BadLocationException e) {
                Log.log(e);
            }
        }
            
    }

    /**
     * Formats the whole document
     * @throws SyntaxErrorException 
     * @see IFormatter
     */
    public void formatAll(IDocument doc, IPyEdit edit, boolean isOpenedFile, boolean throwSyntaxError) throws SyntaxErrorException {
//        Formatter formatter = new Formatter();
//        formatter.formatAll(doc, edit);
        
        FormatStd formatStd = getFormat();
        formatAll(doc, edit, isOpenedFile, formatStd, throwSyntaxError);
    }
    
    public void formatAll(IDocument doc, IPyEdit edit, boolean isOpenedFile, FormatStd formatStd, boolean throwSyntaxError) throws SyntaxErrorException {
        String d = doc.get();
        String delimiter = PySelection.getDelimiter(doc);
        String formatted = formatStr(d, formatStd, delimiter, throwSyntaxError);
        
        String contents = doc.get();
        if(contents.equals(formatted)){
            return; //it's the same: nothing to do.
        }
        if(!isOpenedFile){
            doc.set(formatted);
        }else{
            //let's try to apply only the differences
            int minorLen;
            int contentsLen = contents.length();
            if(contentsLen > formatted.length()){
                minorLen = formatted.length();
            }else{
                minorLen = contentsLen;
            }
            int applyFrom=0;
            for(;applyFrom<minorLen;applyFrom++){
                if(contents.charAt(applyFrom) == formatted.charAt(applyFrom)){
                    continue;
                }else{
                    //different
                    break;
                }
            }
            
            try {
                doc.replace(applyFrom, contentsLen-applyFrom, formatted.substring(applyFrom));
            } catch (BadLocationException e) {
                Log.log(e);
            }
        }
    }

    
    /**
     * @return the format standard that should be used to do the formatting
     */
    public static FormatStd getFormat() {
        FormatStd formatStd = new FormatStd();
        formatStd.assignWithSpaceInsideParens = PyCodeFormatterPage.useAssignWithSpacesInsideParenthesis();
        formatStd.operatorsWithSpace = PyCodeFormatterPage.useOperatorsWithSpace();
        formatStd.parametersWithSpace = PyCodeFormatterPage.useSpaceForParentesis();
        formatStd.spaceAfterComma = PyCodeFormatterPage.useSpaceAfterComma();
        formatStd.addNewLineAtEndOfFile = PyCodeFormatterPage.getAddNewLineAtEndOfFile();
        formatStd.trimLines = PyCodeFormatterPage.getTrimLines();
        return formatStd;
    }

    /**
     * This method formats a string given some standard.
     * 
     * @param str the string to be formatted
     * @param std the standard to be used
     * @return a new (formatted) string
     * @throws SyntaxErrorException 
     */
    public String formatStr(String str, FormatStd std, String delimiter, boolean throwSyntaxError) throws SyntaxErrorException {
        return formatStr(str, std, 0, delimiter, throwSyntaxError);
    }
    
    /**
     * This method formats a string given some standard.
     * 
     * @param str the string to be formatted
     * @param std the standard to be used
     * @param parensLevel the level of the parenthesis available.
     * @return a new (formatted) string
     * @throws SyntaxErrorException 
     */
    private String formatStr(String str, FormatStd std, int parensLevel, String delimiter, boolean throwSyntaxError) throws SyntaxErrorException {
        char[] cs = str.toCharArray();
        FastStringBuffer buf = new FastStringBuffer();
        ParsingUtils parsingUtils = ParsingUtils.create(cs, throwSyntaxError);
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
                    i = formatForPar(parsingUtils, cs, i, std, buf, parensLevel+1, delimiter, throwSyntaxError);
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
                        }
                        if(localC == '\'' || localC == ')' || localC == ']'){
                            isOperator = true;
                        }
                        
                        //If it got here (i.e.: not whitespace), get out of the for loop.
                        break;
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
                        boolean started = false;
                        
                        for(int j=buf.length()-1;;j--){
                            if(j<0){
                                break;
                            }
                            char localC = buf.charAt(j);
                            if(localC == ' ' || localC == '\t'){
                                if(!started){
                                    continue;
                                }else{
                                    break;
                                }
                            }
                            started = true;
                            if(Character.isJavaIdentifierPart(localC) || localC == '.'){
                                localBufToCheckNumber.append(localC);
                            }else{
                                break;//break for
                            }
                        }
                        boolean isExponential = true;
                        String partialNumber = localBufToCheckNumber.reverse().toString();
                        int partialLen = partialNumber.length();
                        if(partialLen < 2 || !Character.isDigit(partialNumber.charAt(0))){
                            //at least 2 chars: the number and the 'e'
                            isExponential = false;
                        }else{
                            //first char checked... now, if the last is an 'e', we must leave it together no matter what
                            if(partialNumber.charAt(partialLen-1) != 'e' && partialNumber.charAt(partialLen-1) != 'E'){
                                isExponential = false;
                            }
                        }
                        if(isExponential){
                            buf.rightTrim();
                            buf.append(c);
                            //skip the next whitespaces from the buffer
                            int initial = i;
                            do{
                                i++;
                            }while(i<cs.length && (c=cs[i]) == ' ' || c == '\t');
                            if(i > initial){
                                i--;//backup 1 because we walked 1 too much.
                            }
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
                        rightTrimIfNeeded(std, buf);
                    }
                    buf.append(c);
                    
            }
            lastChar = c;

        }
        if(parensLevel == 0){
            rightTrimIfNeeded(std, buf);
        }
        return buf.toString();
    }


    private void rightTrimIfNeeded(FormatStd std, FastStringBuffer buf) {
        if(std.trimLines){
            char tempC;
            while(buf.length() > 0 && ((tempC=buf.lastChar()) ==' ' || tempC == '\t')){
                buf.deleteLast();
            }
        }
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
        boolean isUnaryWithContents = true;
        
        boolean isUnary = false;
        boolean changeWhitespacesBefore = true;
        if(c == '~' || c == '+' || c == '-'){
            //could be an unary operator...
            String trimmedLastWord = buf.getLastWord().trim();
            isUnary = trimmedLastWord.length() == 0 || PySelection.ALL_KEYWORD_TOKENS.contains(trimmedLastWord);
            
            if(!isUnary){
                for(char itChar:buf.reverseIterator()){
                    if(itChar == ' ' || itChar == '\t'){
                        continue;
                    }
                    if(itChar == '=' || itChar == ','){
                        isUnary = true;
                    }
                    
                    switch(itChar){
                        case '[':
                        case '{':
                            changeWhitespacesBefore = false;
                            
                        case '(':
                        case ':':
                            isUnaryWithContents = false;
                            
                        case '>':
                        case '<':
                            
                        case '-':
                        case '+':
                        case '~':
                            
                        case '*':
                        case '/':
                        case '%':
                        case '!':
                        case '&':
                        case '^':
                        case '|':
                        case '=':
                            isUnary = true;
                    }
                    break;
                }
            }else{
                isUnaryWithContents = buf.length() > 0;
            }
        }
        
        if(!isUnary){
            //We don't want to change whitespaces before in a binary operator that is in a new line.
            for(char ch:buf.reverseIterator()){
                if(!Character.isWhitespace(ch)){
                    break;
                }
                if(ch == '\r' || ch == '\n'){
                    changeWhitespacesBefore = false;
                    break;
                }
            }
        }
        
        if(changeWhitespacesBefore){
            while(buf.length() > 0 && (buf.lastChar() == ' ' || buf.lastChar() == ' ')){
                buf.deleteLast();
            }
        }
        
        boolean surroundWithSpaces = std.operatorsWithSpace;
        
        if(changeWhitespacesBefore){
            //add spaces before
            if(isUnaryWithContents && surroundWithSpaces){
                buf.append(' ');
            }
        }
        
        char localC = c;
        char prev = '\0';
        boolean backOne = true;
        while(isOperatorPart(localC, prev)){
            buf.append(localC);
            prev = localC;
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
     * @param prev 
     * @return true if the passed char is part of an operator
     */
    private boolean isOperatorPart(char c, char prev) {
        switch(c){
            case '+':
            case '-':
            case '~':
                if(prev == '\0'){
                    return true;
                }
                return false;
            
        }
        
        switch(c){
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
     * @param throwSyntaxError 
     * @throws SyntaxErrorException 
     */
    private int formatForPar(
            final ParsingUtils parsingUtils, 
            final char[] cs, 
            final int i, 
            final FormatStd std, 
            final FastStringBuffer buf, 
            final int parensLevel, 
            final String delimiter, 
            boolean throwSyntaxError) throws SyntaxErrorException {
        char c = ' ';
        FastStringBuffer locBuf = new FastStringBuffer();

        int j = i + 1;
        int start = j;
        int end = start;
        while (j < cs.length && (c = cs[j]) != ')') {

            j++;

            if (c == '\'' || c == '"') { //ignore comments or multiline comments...
                j = parsingUtils.eatLiterals(null, j - 1) + 1;
                end = j;

            } else if (c == '#') {
                j = parsingUtils.eatComments(null, j - 1) + 1;
                end = j;

            } else if (c == '(') { //open another par.
                if(end > start){
                    locBuf.append(cs, start, end-start);
                    start = end;
                }
                j = formatForPar(parsingUtils, cs, j - 1, std, locBuf, parensLevel+1, delimiter, throwSyntaxError) + 1;
                start = j;

            } else {
                end = j;
                
            }
        }
        if(end > start){
            locBuf.append(cs, start, end-start);
            start = end;
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

            String formatStr = formatStr(trim(locBuf).toString(), std, parensLevel, delimiter, throwSyntaxError);
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
            if(throwSyntaxError){
                throw new SyntaxErrorException("No closing ')' found.");
            }
            //we found no closing parens but we finished looking already, so, let's just add anything without
            //more formatting...
            buf.append('(');
            buf.append(locBuf);
            return j;
        }
    }

    /**
     * We just want to trim whitespaces, not newlines!
     * @param locBuf the buffer to be trimmed
     * @return the same buffer passed as a parameter
     */
    private FastStringBuffer trim(FastStringBuffer locBuf) {
        while (locBuf.length() > 0 && (locBuf.firstChar() == ' ' || locBuf.firstChar() == '\t')) {
            locBuf.deleteCharAt(0);
        }
        rtrim(locBuf);
        return locBuf;
    }
    
    /**
     * We just want to trim whitespaces, not newlines!
     * @param locBuf the buffer to be trimmed
     * @return the same buffer passed as a parameter
     */
    private FastStringBuffer rtrim(FastStringBuffer locBuf) {
        while (locBuf.length() > 0 && (locBuf.lastChar() == ' ' || locBuf.lastChar() == '\t')) {
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
