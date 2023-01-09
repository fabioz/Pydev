package com.python.pydev.analysis.refactoring.quick_fixes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.ReplaceEdit;
import org.python.pydev.core.docutils.ImportHandle;
import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;
import org.python.pydev.core.docutils.ImportNotRecognizedException;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PyDocIterator;
import org.python.pydev.core.docutils.PyImportsHandling;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.LineStartingScope;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class AddTokenAndImportStatement {

    public static class ComputedInfo {

        public ComputedInfo(String realImportRep, int fReplacementOffset, int fLen, String indentString,
                String fReplacementString, boolean appliedWithTrigger, int importLen, IDocument document) {
            this.realImportRep = realImportRep;
            this.fReplacementOffset = fReplacementOffset;
            this.fLen = fLen;
            this.indentString = indentString;
            this.fReplacementString = fReplacementString;
            this.appliedWithTrigger = appliedWithTrigger;
            this.importLen = importLen;
            this.document = document;
        }

        // immutable
        public final IDocument document;
        public final String realImportRep;
        public final int fReplacementOffset;
        public final int fLen;
        public final String indentString;

        // mutable (changes must be propagated).
        public String fReplacementString;
        public boolean appliedWithTrigger;
        public int importLen;
        public List<ReplaceEdit> replaceEdit = new ArrayList<ReplaceEdit>(2);

        public void replace(int offset, int length, String text) {
            replaceEdit.add(new ReplaceEdit(offset, length, text));
        }

    }

    private IDocument document;
    private char trigger;
    private int offset;
    private boolean addLocalImport;
    private boolean addLocalImportsOnTopOfMethod;
    private boolean groupImports;
    private int maxCols;
    private PySelection selection;
    private LineStartingScope previousLineThatStartsScope = null;
    private PySelection ps = null;
    private List<ImportHandle> importHandles;

    public AddTokenAndImportStatement(IDocument document, char trigger, int offset, boolean addLocalImport,
            boolean addLocalImportsOnTopOfMethod, boolean groupImports, int maxCols) {

        selection = new PySelection(document);

        if (addLocalImport) {
            ps = new PySelection(document, offset);
            int startLineIndex = ps.getStartLineIndex();
            if (startLineIndex == 0) {
                addLocalImport = false;
            } else {
                String[] indentTokens = PySelection.INDENT_TOKENS;
                if (addLocalImportsOnTopOfMethod) {
                    indentTokens = PySelection.FUNC_TOKEN;
                }
                previousLineThatStartsScope = ps.getPreviousLineThatStartsScope(indentTokens,
                        startLineIndex - 1, PySelection.getFirstCharPosition(ps.getCursorLineContents()));
                if (previousLineThatStartsScope == null) {
                    //note that if we have no previous scope, it means we're actually on the global scope, so,
                    //proceed as usual...
                    addLocalImport = false;
                }
            }
        }

        this.document = document;
        this.trigger = trigger;
        this.offset = offset;
        this.addLocalImport = addLocalImport;
        this.addLocalImportsOnTopOfMethod = addLocalImportsOnTopOfMethod;
        this.groupImports = groupImports;
        this.maxCols = maxCols;
    }

    public LineStartingScope getPreviousLineThatStartsScope() {
        return previousLineThatStartsScope;
    }

    public void createTextEdit(ComputedInfo computedInfo) {
        try {
            int lineToAddImport = -1;
            ImportHandleInfo groupInto = null;
            ImportHandleInfo realImportHandleInfo = null;

            if (computedInfo.realImportRep.length() > 0 && !addLocalImport) {

                //Workaround for: https://sourceforge.net/tracker/?func=detail&aid=2697165&group_id=85796&atid=577329
                //when importing from __future__ import with_statement, we actually want to add a 'with' token, not
                //with_statement token.
                boolean isWithStatement = computedInfo.realImportRep.equals("from __future__ import with_statement");
                if (isWithStatement) {
                    computedInfo.fReplacementString = "with";
                }

                if (groupImports) {
                    try {
                        realImportHandleInfo = new ImportHandleInfo(computedInfo.realImportRep);
                        for (Iterator<ImportHandle> it = createimportsHandlingIterator(); it.hasNext();) {
                            ImportHandle handle = it.next();
                            if (handle.contains(realImportHandleInfo)) {
                                lineToAddImport = -2; //signal that there's no need to find a line available to add the import
                                break;

                            } else if (groupInto == null && realImportHandleInfo.getFromImportStr() != null) {
                                List<ImportHandleInfo> handleImportInfo = handle.getImportInfo();

                                for (ImportHandleInfo importHandleInfo : handleImportInfo) {
                                    if (realImportHandleInfo.getFromImportStr() == null) {
                                        continue;
                                    }
                                    if (realImportHandleInfo.getFromImportStrWithoutUnwantedChars().equals(
                                            importHandleInfo.getFromImportStrWithoutUnwantedChars())) {

                                        groupInto = importHandleInfo;
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (ImportNotRecognizedException e1) {
                        Log.log(e1);//that should not happen at this point
                    }
                }

                if (lineToAddImport == -1) {
                    boolean isFutureImport = PySelection.isFutureImportLine(computedInfo.realImportRep);
                    lineToAddImport = getLineAvailableForImport(isFutureImport);
                }
            } else {
                lineToAddImport = -1;
            }
            String delimiter = this.getDelimiter();

            computedInfo.appliedWithTrigger = trigger == '.' || trigger == '(';
            String appendForTrigger = "";
            if (computedInfo.appliedWithTrigger) {
                if (trigger == '(') {
                    appendForTrigger = "()";

                } else if (trigger == '.') {
                    appendForTrigger = ".";
                }
            }

            //if the trigger is ')', just let it apply regularly -- so, ')' will only be added if it's already in the completion.

            //first do the completion
            if (computedInfo.fReplacementString.length() > 0) {
                int dif = offset - computedInfo.fReplacementOffset;
                computedInfo.replace(offset - dif, dif + computedInfo.fLen,
                        computedInfo.fReplacementString + appendForTrigger);
            }
            if (addLocalImport && computedInfo.realImportRep.length() > 0) {
                if (addAsLocalImport(previousLineThatStartsScope, ps, delimiter, computedInfo,
                        addLocalImportsOnTopOfMethod)) {
                    return;
                }
            }

            if (groupInto != null && realImportHandleInfo != null) {
                //let's try to group it
                int endLineNum = groupInto.getEndLine(); // get the number of last line of import
                String endLineWithoutComment = PySelection
                        .getLineWithoutCommentsOrLiterals(PySelection.getLine(document, endLineNum)); // also get the string of last line, but without comments
                String endLineTrimmedWithoutComment = endLineWithoutComment.trim();

                FastStringBuffer lastImportLineBuf = new FastStringBuffer();
                // let's assume that the last import line is the import end line
                lastImportLineBuf.append(endLineWithoutComment).rightTrim();
                int lastImportLineNum = endLineNum;

                if (endLineNum != groupInto.getStartLine()) {
                    // it is a multi-line import
                    String penultLineWithoutComment = PySelection
                            .getLineWithoutCommentsOrLiterals(TextSelectionUtils.getLine(document, endLineNum - 1));
                    if (")".equals(endLineTrimmedWithoutComment)) {
                        /* this is something like:
                         * from mod import (
                         *      XXXXXX
                         * )
                         */
                        // so let's switch the end line with the penult line
                        lastImportLineBuf.clear().append(penultLineWithoutComment);
                        lastImportLineNum--;
                    } else if (endLineTrimmedWithoutComment.isEmpty()
                            && penultLineWithoutComment.trim().endsWith("\\")) {
                        /* this is something like:
                         * from mod import \
                         *      XXXXXX,\
                         *
                         */
                        // so let's switch the end line with the penult line and remove the '\' from it's end
                        lastImportLineBuf.clear().append(penultLineWithoutComment).rightTrim().deleteLast();
                        lastImportLineNum--;
                    }
                }

                // get the last import line length without comments or literals
                int lastImportLineLen = lastImportLineBuf.length();

                if (lastImportLineBuf.endsWith(')') || lastImportLineBuf.endsWith('\\')) {
                    lastImportLineBuf.deleteLast();
                }

                offset = TextSelectionUtils.getAbsoluteCursorOffset(document, lastImportLineNum,
                        lastImportLineBuf.length());
                offset -= lastImportLineBuf.length() - lastImportLineBuf.rightTrim().length();

                while (lastImportLineBuf.endsWith(',')) {
                    offset--;
                    lastImportLineBuf.deleteLast();
                }

                String strToAdd = ", " + realImportHandleInfo.getImportedStr().get(0); // add the standard and the new import

                if (lastImportLineLen + strToAdd.length() > maxCols) {
                    if (endLineTrimmedWithoutComment.endsWith(")")) {
                        strToAdd = "," + delimiter + computedInfo.indentString
                                + realImportHandleInfo.getImportedStr().get(0);
                    } else {
                        strToAdd = ",\\" + delimiter + computedInfo.indentString
                                + realImportHandleInfo.getImportedStr().get(0);
                    }
                    computedInfo.importLen = strToAdd.length();
                    computedInfo.replace(offset, 0, strToAdd);
                    return;
                } else {
                    //regular addition (it won't pass the number of columns expected).
                    computedInfo.importLen = strToAdd.length();
                    computedInfo.replace(offset, 0, strToAdd);
                    return;
                }
            }

            //if we got here, it hasn't been added in a grouped way, so, let's add it in a new import
            if (lineToAddImport >= 0 && lineToAddImport <= document.getNumberOfLines()) {
                IRegion lineInformation = document.getLineInformation(lineToAddImport);
                String strToAdd = computedInfo.realImportRep + delimiter;
                computedInfo.importLen = strToAdd.length();
                computedInfo.replace(lineInformation.getOffset(), 0, strToAdd);
                return;
            }

        } catch (BadLocationException x) {
            Log.log(x);
        }
    }

    private String delimiter;

    private String getDelimiter() {
        if (delimiter == null) {
            delimiter = PySelection.getDelimiter(document);
        }
        return delimiter;
    }

    private Integer lineForFutureImport;
    private Integer lineForNonFutureImport;

    private int getLineAvailableForImport(boolean isFutureImport) {
        if (isFutureImport) {
            if (lineForFutureImport == null) {
                lineForFutureImport = selection.getLineAvailableForImport(isFutureImport);
            }
            return lineForFutureImport;
        } else {
            if (lineForNonFutureImport == null) {
                lineForNonFutureImport = selection.getLineAvailableForImport(isFutureImport);
            }
            return lineForNonFutureImport;
        }

    }

    private Iterator<ImportHandle> createimportsHandlingIterator() {
        if (importHandles == null) {
            PyImportsHandling importsHandling = new PyImportsHandling(document);
            importHandles = new ArrayList<>();
            importsHandling.forEach(importHandles::add);
        }
        return importHandles.iterator();
    }

    private Tuple<String, Integer> localImportsLocation;

    private boolean addAsLocalImport(LineStartingScope previousLineThatStartsScope, PySelection ps,
            String delimiter, ComputedInfo computedInfo, boolean addLocalImportsOnTopOfMethod) {
        //All the code below is because we don't want to work with a generated AST (as it may not be there),
        //so, we go to the previous scope, find out the valid indent inside it and then got backwards
        //from the position we're in now to find the closer location to where we're now where we're
        //actually able to add the import.
        try {
            if (previousLineThatStartsScope != null) {
                if (localImportsLocation == null) {
                    localImportsLocation = computeLocalImportsLocation(ps, previousLineThatStartsScope,
                            addLocalImportsOnTopOfMethod);
                }
                String indent = localImportsLocation.o1;
                int iLine = localImportsLocation.o2;
                String strToAdd = indent + computedInfo.realImportRep + delimiter;
                Tuple<Integer, String> offsetAndContents = PySelection.getOffsetAndContentsToAddLine(ps.getDoc(),
                        ps.getEndLineDelim(), strToAdd, iLine - 1); //Will add it just after the line passed as a parameter.
                if (offsetAndContents != null) {
                    computedInfo.replace(offsetAndContents.o1, 0, offsetAndContents.o2);
                }
                computedInfo.importLen = strToAdd.length();
                return true;
            }
        } catch (Exception e) {
            Log.log(e); //Something went wrong, add it as global (i.e.: BUG)
        }
        return false;
    }

    private static Tuple<String, Integer> computeLocalImportsLocation(PySelection ps,
            LineStartingScope previousLineThatStartsScope,
            boolean addLocalImportsOnTopOfMethod) throws SyntaxErrorException {
        int iLineStartingScope;
        iLineStartingScope = previousLineThatStartsScope.iLineStartingScope;

        // Ok, we have the line where the scope starts... now, we have to check where that declaration
        // is finished (i.e.: def my( \n\n\n ): <- only after the ):
        Tuple<List<String>, Integer> tuple = new PySelection(ps.getDoc(), iLineStartingScope, 0)
                .getInsideParentesisToks(false);
        if (tuple != null) {
            iLineStartingScope = ps.getLineOfOffset(tuple.o2);
        }

        //Go to a non-empty line from the line we have and the line we're currently in.
        int iLine = iLineStartingScope + 1;
        String line = ps.getLine(iLine);
        final int startLineIndex = ps.getStartLineIndex(); // startLineIndex is our cursor line
        while (iLine < startLineIndex && (line.startsWith("#") || line.trim().length() == 0)) {
            iLine++;
            line = ps.getLine(iLine);
        }
        if (iLine >= startLineIndex) {
            //Sanity check!
            iLine = startLineIndex;
            line = ps.getLine(iLine);
        }
        final String indent = line.substring(0, PySelection.getFirstCharPosition(line));

        if (addLocalImportsOnTopOfMethod) {
            if (iLine < startLineIndex) {
                // Ok, should be on top of the function, but still, after the docstring.
                String line2 = ps.getLine(iLine);
                String trimmed = line2.trim();
                if (trimmed.startsWith("'") || trimmed.startsWith("\"")) {
                    ParsingUtils parsingUtils = ParsingUtils.create(ps.getDoc());
                    int index = line2.indexOf("'");
                    int index2 = line2.indexOf("\"");
                    int use;
                    if (index < 0) {
                        use = index2;
                    } else if (index2 < 0) {
                        use = index;
                    } else {
                        use = Math.min(index, index2);
                    }
                    int newOffset = parsingUtils.eatLiterals(null,
                            ps.getAbsoluteCursorOffset(iLine, use));
                    int lineOfOffset = ps.getLineOfOffset(newOffset) + 1;
                    iLine = lineOfOffset;
                }

                // Also, if there's an import block, keep on going (make it the last import).
                int j = iLine;
                while (j < startLineIndex) {
                    line2 = ps.getLine(j);
                    trimmed = line2.trim();
                    if (trimmed.length() == 0) {
                        j++;
                        // Just a new line won't update the iLine
                        continue;
                    }
                    if (PySelection.isImportLine(trimmed)) {
                        PyDocIterator docIterator = new PyDocIterator(ps.getDoc(), true, true, false,
                                false);
                        docIterator.setStartingOffset(ps.getLineOffset(j));
                        String str = docIterator.next();

                        if (str.contains("(")) { //we have something like from os import (pipe,\nfoo)
                            while (docIterator.hasNext()) {
                                if (str.contains(")")) {
                                    j = docIterator.getLastReturnedLine() + 1;
                                    break;
                                } else {
                                    str = docIterator.next();
                                }
                            }
                        } else if (StringUtils.endsWith(str.trim(), '\\')) {
                            while (docIterator.hasNext()) {
                                if (!StringUtils.endsWith(str.trim(), '\\')) {
                                    j = docIterator.getLastReturnedLine() + 1;
                                    break;
                                }
                                str = docIterator.next();
                            }
                        } else {
                            j++;
                        }

                        iLine = j;
                        continue;
                    }
                    break;
                }
                line = ps.getLine(iLine);
            }

        } else {
            //Ok, all good so far, now, this would add the line to the beginning of
            //the element (after the if statement, def, etc.), let's try to put
            //it closer to where we're now (but still in a valid position).
            int firstCharPos = PySelection.getFirstCharPosition(line);
            int j = startLineIndex;
            while (j >= 0) {
                String line2 = ps.getLine(j);
                if (PySelection.getFirstCharPosition(line2) == firstCharPos) {
                    iLine = j;
                    break;
                }
                if (j == iLineStartingScope) {
                    break;
                }
                j--;
            }
        }

        return new Tuple<String, Integer>(indent, iLine);
    }
}