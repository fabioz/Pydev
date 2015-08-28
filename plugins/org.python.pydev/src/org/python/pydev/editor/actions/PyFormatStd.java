/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPyFormatStdProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.prettyprinterv2.IFormatter;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.plugin.preferences.PyCodeFormatterPage;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.SelectionKeeper;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;

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
         * Format with autopep8.py?
         */
        public boolean formatWithAutopep8;

        /**
         * Parameters for autopep8.
         */
        public String autopep8Parameters;

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

        public boolean trimMultilineLiterals;

        public static final int DONT_HANDLE_SPACES = -1;
        /**
         * -1 = don't handle
         * 0 = 0 space
         * 1 = 1 space
         * 2 = 2 spaces
         * ...
         */
        public int spacesBeforeComment = DONT_HANDLE_SPACES;

        /**
         * Spaces after the '#' in a comment. -1 = don't handle.
         */
        public int spacesInStartComment = DONT_HANDLE_SPACES;

        /**
         * This method should be called after all related attributes are set when autopep8 is set to true.
         */
        public void updateAutopep8() {
            if (formatWithAutopep8) {
                spaceAfterComma = true;
                parametersWithSpace = false;
                assignWithSpaceInsideParens = false;
                operatorsWithSpace = true;
                addNewLineAtEndOfFile = true;
                trimLines = true;
                trimMultilineLiterals = false;
                spacesBeforeComment = 2;
                spacesInStartComment = 1;
            }
        }
    }

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(IAction action) {
        try {
            if (!canModifyEditor()) {
                return;
            }

            PyEdit pyEdit = getPyEdit();
            PySelection ps = new PySelection(pyEdit);

            try {
                int[] regionsToFormat = null;
                if (ps.getSelLength() > 0) {
                    int startLineIndex = ps.getStartLineIndex();
                    int endLineIndex = ps.getEndLineIndex();
                    regionsToFormat = new int[endLineIndex - startLineIndex + 1];
                    for (int i = startLineIndex, j = 0; i <= endLineIndex; i++, j++) {
                        regionsToFormat[j] = i;
                    }
                } else {
                    //For full-formatting, we cannot have a syntax error.
                    if (pyEdit.hasSyntaxError(ps.getDoc())) {
                        return;
                    }
                }

                applyFormatAction(pyEdit, ps, regionsToFormat, true, pyEdit.getSelectionProvider());
            } catch (SyntaxErrorException e) {
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
    public void applyFormatAction(IPyFormatStdProvider pyEdit, PySelection ps, int[] regionsToFormat,
            boolean throwSyntaxError,
            ISelectionProvider selectionProvider)
                    throws BadLocationException, SyntaxErrorException {
        final IFormatter participant = getFormatter();
        final IDocument doc = ps.getDoc();
        final SelectionKeeper selectionKeeper = new SelectionKeeper(ps);

        DocumentRewriteSession session = null;
        try {

            if (regionsToFormat == null || regionsToFormat.length == 0) {
                if (doc instanceof IDocumentExtension4) {
                    IDocumentExtension4 ext = (IDocumentExtension4) doc;
                    session = ext.startRewriteSession(DocumentRewriteSessionType.STRICTLY_SEQUENTIAL);
                }
                participant.formatAll(doc, pyEdit, null, true, throwSyntaxError);

            } else {
                if (doc instanceof IDocumentExtension4) {
                    IDocumentExtension4 ext = (IDocumentExtension4) doc;
                    session = ext.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
                }
                participant.formatSelection(doc, regionsToFormat, pyEdit, ps);
            }

        } finally {
            if (session != null) {
                ((IDocumentExtension4) doc).stopRewriteSession(session);
            }
        }
        if (selectionProvider != null) {
            selectionKeeper.restoreSelection(selectionProvider, doc);
        }

    }

    /**
     * @return the source code formatter to be used.
     */
    public IFormatter getFormatter() {
        IFormatter participant = (IFormatter) ExtensionHelper.getParticipant(ExtensionHelper.PYDEV_FORMATTER, false);
        if (participant == null) {
            participant = this;
        }
        return participant;
    }

    public void formatSelection(IDocument doc, int[] regionsForSave, IPyFormatStdProvider edit, PySelection ps) {
        FormatStd formatStd = getFormat(edit);
        formatSelection(doc, regionsForSave, edit, ps, formatStd);
    }

    /**
     * Formats the given selection
     * @see IFormatter
     */
    public void formatSelection(IDocument doc, int[] regionsForSave, IPyFormatStdProvider edit, PySelection ps,
            FormatStd formatStd) {
        //        Formatter formatter = new Formatter();
        //        formatter.formatSelection(doc, startLine, endLineIndex, edit, ps);

        if (formatStd.formatWithAutopep8) {
            // get a copy of formatStd to avoid being overwritten by settings
            FormatStd formatStdNew = (FormatStd) (edit != null ? edit.getFormatStd() : getFormat(null));
            // no need to remember old values, as they'll always be created from scratch
            try {
                // assume it's a continuous region
                if (regionsForSave.length > 0) { // at least one line selected
                    int firstSelectedLine = regionsForSave[0] + 1;
                    int lastSelectedLine = regionsForSave[regionsForSave.length - 1] + 1;
                    // hack, use global settings to pass down argument to formatStr
                    // that possibly overwrites other --range options, but that's highly unlikely
                    // autopep8 says that it accepts line-range, but then it complains in runtime
                    // so range is used instead
                    formatStdNew.autopep8Parameters += " --range " + firstSelectedLine + " " + lastSelectedLine;
                }
                formatAll(doc, edit, true, formatStdNew, true);
            } catch (SyntaxErrorException e) {
            }
            return;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<Tuple3<Integer, Integer, String>> replaces = new ArrayList();

        String docContents = doc.get();
        String delimiter = PySelection.getDelimiter(doc);
        IDocument formatted;
        try {
            formatted = new Document(formatAll(formatStd, true, docContents, delimiter));
        } catch (SyntaxErrorException e) {
            return;
        }
        //Actually replace the formatted lines: in our formatting, lines don't change, so, this is OK :)
        try {
            for (int i : regionsForSave) {
                IRegion r = doc.getLineInformation(i);
                int iStart = r.getOffset();
                int iEnd = r.getOffset() + r.getLength();

                String line = PySelection.getLine(formatted, i);
                replaces.add(new Tuple3<Integer, Integer, String>(iStart, iEnd - iStart, line));
            }

        } catch (BadLocationException e) {
            Log.log(e);
            return;
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

        if (formatStd.addNewLineAtEndOfFile) {
            try {
                int len = doc.getLength();
                if (len > 0) {
                    char lastChar = doc.getChar(len - 1);
                    if (lastChar != '\r' && lastChar != '\n') {
                        doc.replace(len, 0, PySelection.getDelimiter(doc));
                    }
                }
            } catch (Throwable e) {
                Log.log(e);
            }
        }
    }

    /**
     * Formats the whole document
     * @throws SyntaxErrorException
     * @see IFormatter
     */
    public void formatAll(IDocument doc, IPyFormatStdProvider edit, IFile f, boolean isOpenedFile,
            boolean throwSyntaxError)
                    throws SyntaxErrorException {
        //        Formatter formatter = new Formatter();
        //        formatter.formatAll(doc, edit);

        FormatStd formatStd = (FormatStd) (edit != null ? edit.getFormatStd() : getFormat(f));
        formatAll(doc, edit, isOpenedFile, formatStd, throwSyntaxError);

    }

    public void formatAll(IDocument doc, IPyFormatStdProvider edit, boolean isOpenedFile, FormatStd formatStd,
            boolean throwSyntaxError) throws SyntaxErrorException {
        String d = doc.get();
        String delimiter = PySelection.getDelimiter(doc);
        String formatted = formatAll(formatStd, throwSyntaxError, d, delimiter);

        String contents = doc.get();
        if (contents.equals(formatted)) {
            return; //it's the same: nothing to do.
        }
        if (!isOpenedFile) {
            doc.set(formatted);
        } else {
            //let's try to apply only the differences
            TextSelectionUtils.setOnlyDifferentCode(doc, contents, formatted);
        }
    }

    private String formatAll(FormatStd formatStd, boolean throwSyntaxError, String d, String delimiter)
            throws SyntaxErrorException {
        String formatted = formatStr(d, formatStd, delimiter, throwSyntaxError);

        //To finish, check the end of line.
        if (formatStd.addNewLineAtEndOfFile) {
            try {
                int len = formatted.length();
                if (len > 0) {
                    char lastChar = formatted.charAt(len - 1);
                    if (lastChar != '\r' && lastChar != '\n') {
                        formatted += delimiter;
                    }
                }
            } catch (Throwable e) {
                Log.log(e);
            }
        }
        return formatted;
    }

    /**
     * @return the format standard that should be used to do the formatting
     */
    public static FormatStd getFormat(IAdaptable projectAdaptable) {
        FormatStd formatStd = new FormatStd();
        formatStd.assignWithSpaceInsideParens = PyCodeFormatterPage
                .useAssignWithSpacesInsideParenthesis(projectAdaptable);
        formatStd.operatorsWithSpace = PyCodeFormatterPage.useOperatorsWithSpace(projectAdaptable);
        formatStd.parametersWithSpace = PyCodeFormatterPage.useSpaceForParentesis(projectAdaptable);
        formatStd.spaceAfterComma = PyCodeFormatterPage.useSpaceAfterComma(projectAdaptable);
        formatStd.addNewLineAtEndOfFile = PyCodeFormatterPage.getAddNewLineAtEndOfFile(projectAdaptable);
        formatStd.trimLines = PyCodeFormatterPage.getTrimLines(projectAdaptable);
        formatStd.trimMultilineLiterals = PyCodeFormatterPage.getTrimMultilineLiterals(projectAdaptable);
        formatStd.spacesBeforeComment = PyCodeFormatterPage.getSpacesBeforeComment(projectAdaptable);
        formatStd.spacesInStartComment = PyCodeFormatterPage.getSpacesInStartComment(projectAdaptable);
        formatStd.formatWithAutopep8 = PyCodeFormatterPage.getFormatWithAutopep8(projectAdaptable);
        formatStd.autopep8Parameters = PyCodeFormatterPage.getAutopep8Parameters(projectAdaptable);
        formatStd.updateAutopep8();
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
    /*default*/String formatStr(String str, FormatStd std, String delimiter, boolean throwSyntaxError)
            throws SyntaxErrorException {
        if (std.formatWithAutopep8) {
            String parameters = std.autopep8Parameters;
            String formatted = runWithPep8BaseScript(str, parameters, "autopep8.py", str);

            formatted = StringUtils.replaceNewLines(formatted, delimiter);

            return formatted;
        } else {
            return formatStr(str, std, 0, delimiter, throwSyntaxError);
        }
    }

    /**
     * @param fileContents the contents to be passed in the stdin.
     * @param parameters the parameters to pass. Note that a '-' is always added to the parameters to signal we'll pass the file as the input in stdin.
     * @param script i.e.: pep8.py, autopep8.py
     * @return
     */
    public static String runWithPep8BaseScript(String fileContents, String parameters, String script,
            String defaultReturn) {
        File autopep8File;
        try {
            autopep8File = PydevPlugin.getScriptWithinPySrc(new Path("third_party").append("pep8")
                    .append(script).toString());
        } catch (CoreException e) {
            Log.log("Unable to get " + script + " location.");
            return defaultReturn;
        }
        if (!autopep8File.exists()) {
            Log.log("Specified location for " + script + " does not exist (" + autopep8File + ").");
            return defaultReturn;
        }

        SimplePythonRunner simplePythonRunner = new SimplePythonRunner();
        IInterpreterManager pythonInterpreterManager = PydevPlugin.getPythonInterpreterManager();
        IInterpreterInfo defaultInterpreterInfo;
        try {
            defaultInterpreterInfo = pythonInterpreterManager.getDefaultInterpreterInfo(false);
        } catch (MisconfigurationException e) {
            Log.log("No default Python interpreter configured to run " + script);
            return defaultReturn;
        }
        String[] parseArguments = ProcessUtils.parseArguments(parameters);
        List<String> lst = new ArrayList<>(Arrays.asList(parseArguments));
        lst.add("-");

        String[] cmdarray = SimplePythonRunner.preparePythonCallParameters(
                defaultInterpreterInfo.getExecutableOrJar(), autopep8File.toString(),
                lst.toArray(new String[0]));

        Reader inputStreamReader = new StringReader(fileContents);
        String pythonFileEncoding = FileUtils.getPythonFileEncoding(inputStreamReader, null);
        if (pythonFileEncoding == null) {
            pythonFileEncoding = "utf-8";
        }
        final String encodingUsed = pythonFileEncoding;

        SystemPythonNature nature = new SystemPythonNature(pythonInterpreterManager, defaultInterpreterInfo);
        ICallback<String[], String[]> updateEnv = new ICallback<String[], String[]>() {

            @Override
            public String[] call(String[] arg) {
                if (arg == null) {
                    arg = new String[] { "PYTHONIOENCODING=" + encodingUsed };
                } else {
                    arg = ProcessUtils.addOrReplaceEnvVar(arg, "PYTHONIOENCODING", encodingUsed);
                }
                return arg;
            }
        };

        Tuple<Process, String> r = simplePythonRunner.run(cmdarray, autopep8File.getParentFile(), nature,
                new NullProgressMonitor(), updateEnv);
        try {
            r.o1.getOutputStream().write(fileContents.getBytes(pythonFileEncoding));
            r.o1.getOutputStream().close();
        } catch (IOException e) {
            Log.log("Error writing contents to " + script);
            return defaultReturn;
        }
        Tuple<String, String> processOutput = SimplePythonRunner.getProcessOutput(r.o1, r.o2,
                new NullProgressMonitor(), pythonFileEncoding);

        if (processOutput.o2.length() > 0) {
            Log.log(processOutput.o2);
        }
        if (processOutput.o1.length() > 0) {
            return processOutput.o1;
        }
        return defaultReturn;
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
    private String formatStr(String str, FormatStd std, int parensLevel, String delimiter, boolean throwSyntaxError)
            throws SyntaxErrorException {
        char[] cs = str.toCharArray();
        FastStringBuffer buf = new FastStringBuffer();

        //Temporary buffer for some operations. Must always be cleared before it's used.
        FastStringBuffer tempBuf = new FastStringBuffer();

        ParsingUtils parsingUtils = ParsingUtils.create(cs, throwSyntaxError);
        char lastChar = '\0';
        for (int i = 0; i < cs.length; i++) {
            char c = cs[i];

            switch (c) {
                case '\'':
                case '"':
                    //ignore literals and multi-line literals, including comments...
                    i = parsingUtils.eatLiterals(buf, i, std.trimMultilineLiterals);
                    break;

                case '#':
                    i = handleComment(std, cs, buf, tempBuf, parsingUtils, i);
                    break;

                case ',':
                    i = formatForComma(std, cs, buf, i, tempBuf);
                    break;

                case '(':
                    i = formatForPar(parsingUtils, cs, i, std, buf, parensLevel + 1, delimiter, throwSyntaxError);
                    break;

                //Things to treat:
                //+, -, *, /, %
                //** // << >>
                //<, >, !=, <>, <=, >=, //=, *=, /=,
                //& ^ ~ |
                case '*':
                    //for *, we also need to treat when it's used in varargs, kwargs and list expansion
                    boolean isOperator = false;
                    for (int j = buf.length() - 1; j >= 0; j--) {
                        char localC = buf.charAt(j);
                        if (Character.isWhitespace(localC)) {
                            continue;
                        }
                        if (localC == '(' || localC == ',') {
                            //it's not an operator, but vararg. kwarg or list expansion
                        }
                        if (Character.isJavaIdentifierPart(localC)) {
                            //ok, there's a chance that it can be an operator, but we still have to check
                            //the chance that it's a wild import
                            tempBuf.clear();
                            while (Character.isJavaIdentifierPart(localC)) {
                                tempBuf.append(localC);
                                j--;
                                if (j < 0) {
                                    break; //break while
                                }
                                localC = buf.charAt(j);
                            }
                            String reversed = tempBuf.reverse().toString();
                            if (!reversed.equals("import") && !reversed.equals("lambda")) {
                                isOperator = true;
                            }
                        }
                        if (localC == '\'' || localC == ')' || localC == ']') {
                            isOperator = true;
                        }

                        //If it got here (i.e.: not whitespace), get out of the for loop.
                        break;
                    }
                    if (!isOperator) {
                        buf.append('*');
                        break;//break switch
                    }
                    //Otherwise, FALLTHROUGH

                case '+':
                case '-':

                    if (c == '-' || c == '+') { // could also be *

                        //handle exponentials correctly: e.g.: 1e-6 cannot have a space
                        tempBuf.clear();
                        boolean started = false;

                        for (int j = buf.length() - 1;; j--) {
                            if (j < 0) {
                                break;
                            }
                            char localC = buf.charAt(j);
                            if (localC == ' ' || localC == '\t') {
                                if (!started) {
                                    continue;
                                } else {
                                    break;
                                }
                            }
                            started = true;
                            if (Character.isJavaIdentifierPart(localC) || localC == '.') {
                                tempBuf.append(localC);
                            } else {
                                break;//break for
                            }
                        }
                        boolean isExponential = true;
                        String partialNumber = tempBuf.reverse().toString();
                        int partialLen = partialNumber.length();
                        if (partialLen < 2 || !Character.isDigit(partialNumber.charAt(0))) {
                            //at least 2 chars: the number and the 'e'
                            isExponential = false;
                        } else {
                            //first char checked... now, if the last is an 'e', we must leave it together no matter what
                            if (partialNumber.charAt(partialLen - 1) != 'e'
                                    && partialNumber.charAt(partialLen - 1) != 'E') {
                                isExponential = false;
                            }
                        }
                        if (isExponential) {
                            buf.rightTrim();
                            buf.append(c);
                            //skip the next whitespaces from the buffer
                            int initial = i;
                            do {
                                i++;
                            } while (i < cs.length && (c = cs[i]) == ' ' || c == '\t');
                            if (i > initial) {
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
                    if (i < cs.length - 1 && cs[i + 1] == '=') {
                        //if == handle as if a regular operator
                        i = handleOperator(std, cs, buf, parsingUtils, i, c);
                        c = cs[i];
                        break;
                    }

                    while (buf.length() > 0 && buf.lastChar() == ' ') {
                        buf.deleteLast();
                    }

                    boolean surroundWithSpaces = std.operatorsWithSpace;
                    if (parensLevel > 0) {
                        surroundWithSpaces = std.assignWithSpaceInsideParens;
                    }

                    //add space before
                    if (surroundWithSpaces) {
                        buf.append(' ');
                    }

                    //add the operator and the '='
                    buf.append('=');

                    //add space after
                    if (surroundWithSpaces) {
                        buf.append(' ');
                    }

                    i = parsingUtils.eatWhitespaces(null, i + 1);
                    break;

                default:
                    if (c == '\r' || c == '\n') {
                        if (lastChar == ',' && std.spaceAfterComma && buf.lastChar() == ' ') {
                            buf.deleteLast();
                        }
                        if (std.trimLines) {
                            buf.rightTrim();
                        }
                    }
                    buf.append(c);

            }
            lastChar = c;

        }
        if (parensLevel == 0 && std.trimLines) {
            buf.rightTrim();
        }
        return buf.toString();
    }

    /**
     * Handles the case where we found a '#' in the code.
     */
    private int handleComment(FormatStd std, char[] cs, FastStringBuffer buf, FastStringBuffer tempBuf,
            ParsingUtils parsingUtils, int i) {
        if (std.spacesBeforeComment != FormatStd.DONT_HANDLE_SPACES) {
            for (int j = i - 1; j >= 0; j--) {
                char cj = cs[j];
                if (cj == '\t' || cj == ' ') {
                    continue;
                }
                //Ok, found a non-whitespace -- if it's not a new line, we're after some
                //code, in which case we have to put the configured amount of spaces.
                if (cj != '\r' && cj != '\n') {
                    buf.rightTrim();
                    buf.appendN(' ', std.spacesBeforeComment);
                }
                break;
            }
        }

        tempBuf.clear();
        i = parsingUtils.eatComments(tempBuf, i);
        if (std.trimLines) {
            String endLine = "";
            if (tempBuf.endsWith("\r\n")) {
                endLine = "\r\n";
                tempBuf.deleteLastChars(2);
            } else if (tempBuf.endsWith('\r') || tempBuf.endsWith('\n')) {
                endLine += tempBuf.lastChar();
                tempBuf.deleteLast();
            }
            tempBuf.rightTrim();
            tempBuf.append(endLine);
        }

        formatComment(std, tempBuf);

        buf.append(tempBuf);
        return i;
    }

    /**
     * A comment line starting or ending with one of the following will be skipped when adding
     * spaces to the start of a comment.
     */
    private final static String[] BLOCK_COMMENT_SKIPS = new String[] {
            "###",
            "***",
            "---",
            "===",
            "+++",
            "@@@",
            "!!!",
            "~~~",
    };

    /**
     * Adds spaces after the '#' according to the configured settings. The first char of the
     * buffer passed (which is also the output) should always start with a '#'.
     */
    public static void formatComment(FormatStd std, FastStringBuffer bufWithComment) {
        if (std.spacesInStartComment > 0) {
            Assert.isTrue(bufWithComment.charAt(0) == '#');
            int len = bufWithComment.length();

            char firstCharFound = '\n';
            String bufAsString = bufWithComment.toString();
            //handle cases where the code-formatting should not take place
            if (FileUtils.isPythonShebangLine(bufAsString)) {
                return;
            }

            int spacesFound = 0;
            String remainingStringContent = "";
            for (int j = 1; j < len; j++) { //start at 1 because 0 should always be '#'
                if ((firstCharFound = bufWithComment.charAt(j)) != ' ') {
                    remainingStringContent = bufAsString.substring(j).trim();
                    break;
                }
                spacesFound += 1;
            }
            if (firstCharFound != '\r' && firstCharFound != '\n') { //Only add spaces if it wasn't an empty line.

                //handle cases where the code-formatting should not take place
                for (String s : BLOCK_COMMENT_SKIPS) {
                    if (remainingStringContent.endsWith(s) || remainingStringContent.startsWith(s)) {
                        return;
                    }
                }
                int diff = std.spacesInStartComment - spacesFound;
                if (diff > 0) {
                    bufWithComment.insertN(1, ' ', diff);
                }
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
    private int handleOperator(FormatStd std, char[] cs, FastStringBuffer buf, ParsingUtils parsingUtils, int i,
            char c) {
        //let's discover if it's an unary operator (~ + -)
        boolean isUnaryWithContents = true;

        boolean isUnary = false;
        boolean changeWhitespacesBefore = true;
        if (c == '~' || c == '+' || c == '-') {
            //could be an unary operator...
            String trimmedLastWord = buf.getLastWord().trim();
            isUnary = trimmedLastWord.length() == 0 || PySelection.ALL_KEYWORD_TOKENS.contains(trimmedLastWord);

            if (!isUnary) {
                for (char itChar : buf.reverseIterator()) {
                    if (itChar == ' ' || itChar == '\t') {
                        continue;
                    }

                    switch (itChar) {
                        case '[':
                        case '{':
                        case '=':
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
                        case ',':
                            isUnary = true;
                    }
                    break;
                }
            } else {
                isUnaryWithContents = buf.length() > 0;
            }
        }

        //We don't want to change whitespaces before in a binary operator that is in a new line.
        for (char ch : buf.reverseIterator()) {
            if (!Character.isWhitespace(ch)) {
                break;
            }
            if (ch == '\r' || ch == '\n') {
                changeWhitespacesBefore = false;
                break;
            }
        }

        if (changeWhitespacesBefore) {
            while (buf.length() > 0 && (buf.lastChar() == ' ' || buf.lastChar() == ' ')) {
                buf.deleteLast();
            }
        }

        boolean surroundWithSpaces = std.operatorsWithSpace;

        if (changeWhitespacesBefore) {
            //add spaces before
            if (isUnaryWithContents && surroundWithSpaces) {
                buf.append(' ');
            }
        }

        char localC = c;
        char prev = '\0';
        boolean backOne = true;
        while (isOperatorPart(localC, prev)) {
            buf.append(localC);
            prev = localC;
            i++;
            if (i == cs.length) {
                break;
            }
            localC = cs[i];
            if (localC == '=') {
                //when we get to an assign, we have found a full stmt (with assign) -- e.g.: a \\=  a += a ==
                buf.append(localC);
                backOne = false;
                break;
            }
        }
        if (backOne) {
            i--;
        }

        //add space after only if it's not unary
        if (!isUnary && surroundWithSpaces) {
            buf.append(' ');
        }

        i = parsingUtils.eatWhitespaces(null, i + 1);
        return i;
    }

    /**
     * @param c the char to be checked
     * @param prev
     * @return true if the passed char is part of an operator
     */
    private boolean isOperatorPart(char c, char prev) {
        switch (c) {
            case '+':
            case '-':
            case '~':
                if (prev == '\0') {
                    return true;
                }
                return false;

        }

        switch (c) {
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
    private int formatForPar(final ParsingUtils parsingUtils, final char[] cs, final int i, final FormatStd std,
            final FastStringBuffer buf, final int parensLevel, final String delimiter, boolean throwSyntaxError)
                    throws SyntaxErrorException {
        char c = ' ';
        FastStringBuffer locBuf = new FastStringBuffer();

        int j = i + 1;
        int start = j;
        int end = start;
        while (j < cs.length && (c = cs[j]) != ')') {

            j++;

            if (c == '\'' || c == '"') { //ignore comments or multiline comments...
                j = parsingUtils.eatLiterals(null, j - 1, std.trimMultilineLiterals) + 1;
                end = j;

            } else if (c == '#') {
                j = parsingUtils.eatComments(null, j - 1) + 1;
                end = j;

            } else if (c == '(') { //open another par.
                if (end > start) {
                    locBuf.append(cs, start, end - start);
                    start = end;
                }
                j = formatForPar(parsingUtils, cs, j - 1, std, locBuf, parensLevel + 1, delimiter, throwSyntaxError)
                        + 1;
                start = j;

            } else {
                end = j;

            }
        }
        if (end > start) {
            locBuf.append(cs, start, end - start);
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
            if (throwSyntaxError) {
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
    private int formatForComma(FormatStd std, char[] cs, FastStringBuffer buf, int i,
            FastStringBuffer formatForCommaTempBuf) {
        formatForCommaTempBuf.clear();
        char c = '\0';
        while (i < cs.length - 1 && (c = cs[i + 1]) == ' ') {
            formatForCommaTempBuf.append(c);
            i++;
        }

        if (c == '#') {
            //Ok, we have a comment after a comma, let's handle it according to preferences.
            buf.append(',');
            if (std.spacesBeforeComment == FormatStd.DONT_HANDLE_SPACES) {
                //Note: other cases we won't handle here as it should be handled when the start of
                //a comment is found.
                buf.append(formatForCommaTempBuf);
            }
        } else {
            //Default: handle it as usual.
            if (std.spaceAfterComma) {
                buf.append(", ");
            } else {
                buf.append(',');
            }
        }
        return i;
    }
}
