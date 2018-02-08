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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.python.pydev.builder.pep8.Pep8Visitor;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPyFormatStdProvider;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.formatter.PyFormatStdManageBlankLines;
import org.python.pydev.core.formatter.PyFormatStdManageBlankLines.LineOffsetAndInfo;
import org.python.pydev.core.formatter.PyFormatter;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PySelectionFromEditor;
import org.python.pydev.parser.prettyprinterv2.IFormatter;
import org.python.pydev.plugin.preferences.PyCodeFormatterPage;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.shared_core.utils.DocUtils.EmptyLinesComputer;

/**
 * @author Fabio Zadrozny
 */
public class PyFormatStd extends PyAction implements IFormatter {

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
            PySelection ps = PySelectionFromEditor.createPySelectionFromEditor(pyEdit);

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

    @Override
    public void formatSelection(IDocument doc, int[] regionsForSave, IPyFormatStdProvider edit, PySelection ps) {
        FormatStd formatStd = getFormat(edit);
        formatSelection(doc, regionsForSave, edit, ps, formatStd);
    }

    /**
     * Formats the given selection
     * @param regionsForSave lines to be formatted (0-based).
     * @see IFormatter
     */
    public static void formatSelection(IDocument doc, int[] regionsForSave, IPyFormatStdProvider edit, PySelection ps,
            FormatStd formatStd) {
        //        Formatter formatter = new Formatter();
        //        formatter.formatSelection(doc, startLine, endLineIndex, edit, ps);
        Assert.isTrue(regionsForSave != null);

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
                formatAll(doc, edit, true, formatStdNew, true, false);
            } catch (SyntaxErrorException e) {
            }
            return;
        }

        String delimiter = PySelection.getDelimiter(doc);
        IDocument formatted;
        String formattedAsStr;
        try {
            boolean allowChangingBlankLines = false;
            formattedAsStr = formatStrAutopep8OrPyDev(formatStd, true, doc, delimiter, allowChangingBlankLines);
            formatted = new Document(formattedAsStr);
        } catch (SyntaxErrorException e) {
            return;
        }
        try {
            // Actually replace the formatted lines: in this formatting, lines don't change, so, this is OK :)
            // Apply the formatting from bottom to top (so that the indexes are still valid).
            int[] regionsReversed = ArrayUtils.reversedCopy(regionsForSave);

            for (int i : regionsReversed) {
                IRegion r = doc.getLineInformation(i);
                int iStart = r.getOffset();
                int iEnd = r.getOffset() + r.getLength();

                String line = PySelection.getLine(formatted, i);
                doc.replace(iStart, iEnd - iStart, line);
            }
            if (formatStd.manageBlankLines) {
                // Now, remove or add blank lines as needed.
                FastStringBuffer buf = new FastStringBuffer(formattedAsStr, 10);
                List<LineOffsetAndInfo> computed = PyFormatStdManageBlankLines
                        .computeBlankLinesAmongMethodsAndClasses(formatStd, buf, delimiter);
                Collections.reverse(computed);
                String delimTwice = delimiter + delimiter;
                String delimTimes3 = delimTwice + delimiter;
                Set<Integer> hashSet = new HashSet<>();
                EmptyLinesComputer emptyLinesComputer = new EmptyLinesComputer(doc);
                for (int i : regionsForSave) {
                    // Note: to properly deal with blank line removal, consider all blank lines as a
                    // single block (otherwise it may be really hard for the code formatter to know
                    // which of those lines should be deleted or to which of those lines a new
                    // line should be added).
                    hashSet.add(i);
                    emptyLinesComputer.addToSetEmptyLinesCloseToLine(hashSet, i);
                }

                for (LineOffsetAndInfo lineOffsetAndInfo : computed) {
                    if (!hashSet.contains(lineOffsetAndInfo.infoFromRealLine)) {
                        continue;
                    }
                    // We're going backwards to keep lines valid...
                    if (lineOffsetAndInfo.delete) {
                        String line = PySelection.getLine(doc, lineOffsetAndInfo.infoFromRealLine);
                        if (line.trim().length() == 0) {
                            // Make sure that we only delete whitespaces.
                            PySelection.deleteLine(doc, lineOffsetAndInfo.infoFromRealLine);
                        }
                    }
                    if (lineOffsetAndInfo.addBlankLines > 0) {
                        String useDelim;
                        if (lineOffsetAndInfo.addBlankLines == 1) {
                            useDelim = delimiter;
                        } else if (lineOffsetAndInfo.addBlankLines == 2) {
                            useDelim = delimTwice;
                        } else if (lineOffsetAndInfo.addBlankLines == 3) {
                            useDelim = delimTimes3;
                        } else {
                            useDelim = new FastStringBuffer().appendN(delimiter, lineOffsetAndInfo.addBlankLines)
                                    .toString();
                        }
                        doc.replace(doc.getLineInformation(lineOffsetAndInfo.infoFromRealLine).getOffset(), 0,
                                useDelim);
                    }
                }
            }

        } catch (BadLocationException | SyntaxErrorException e) {
            Log.log(e);
            return;
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
    @Override
    public void formatAll(IDocument doc, IPyFormatStdProvider edit, IFile f, boolean isOpenedFile,
            boolean throwSyntaxError)
            throws SyntaxErrorException {
        //        Formatter formatter = new Formatter();
        //        formatter.formatAll(doc, edit);

        FormatStd formatStd = (FormatStd) (edit != null ? edit.getFormatStd() : getFormat(f));
        formatAll(doc, edit, isOpenedFile, formatStd, throwSyntaxError, true);

    }

    public static void formatAll(IDocument doc, IPyFormatStdProvider edit, boolean isOpenedFile, FormatStd formatStd,
            boolean throwSyntaxError, boolean allowChangingLines) throws SyntaxErrorException {
        String delimiter = PySelection.getDelimiter(doc);
        String formatted = formatStrAutopep8OrPyDev(formatStd, throwSyntaxError, doc, delimiter, allowChangingLines);

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

    private static String formatStrAutopep8OrPyDev(FormatStd formatStd, boolean throwSyntaxError, IDocument doc,
            String delimiter, boolean allowChangingBlankLines)
            throws SyntaxErrorException {
        String formatted = formatStrAutopep8OrPyDev(doc, formatStd, delimiter, throwSyntaxError,
                allowChangingBlankLines);
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
        formatStd.manageBlankLines = PyCodeFormatterPage.getManageBlankLines(projectAdaptable);
        formatStd.blankLinesTopLevel = PyCodeFormatterPage
                .getBlankLinesTopLevel(projectAdaptable);
        formatStd.blankLinesInner = PyCodeFormatterPage.getBlankLinesInner(projectAdaptable);
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
    /*default*/static String formatStrAutopep8OrPyDev(IDocument doc, FormatStd std, String delimiter,
            boolean throwSyntaxError,
            boolean allowChangingBlankLines) throws SyntaxErrorException {
        if (std.formatWithAutopep8) {
            String parameters = std.autopep8Parameters;
            String formatted = Pep8Visitor.runWithPep8BaseScript(doc, parameters, "autopep8.py");
            if (formatted == null) {
                formatted = doc.get();
            }

            formatted = StringUtils.replaceNewLines(formatted, delimiter);

            return formatted;
        } else {
            FastStringBuffer buf = PyFormatter.formatStr(doc.get(), std, 0, delimiter, throwSyntaxError);
            if (allowChangingBlankLines && std.manageBlankLines) {
                List<LineOffsetAndInfo> computed = PyFormatStdManageBlankLines
                        .computeBlankLinesAmongMethodsAndClasses(std, buf, delimiter);
                return PyFormatStdManageBlankLines
                        .fixBlankLinesAmongMethodsAndClasses(computed, std, doc, buf, delimiter).toString();
            } else {
                return buf.toString();
            }
        }
    }

}
