/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * The initial pretty printer approach consisted of going to a scope and then printing things
 * in that scope as it walked the structure, but this approach doesn't seem to work well
 * because of comments, as it depends too much on how the parsing was done and the comments
 * found (and javacc just spits them out and the parser tries to put them in good places, but
 * this is often not what happens)
 * 
 * So, a different approach will be tested:
 * Instead of doing everything in a single pass, we'll traverse the structure once to create
 * a new (flat) structure, in a 2nd step that structure will be filled with comments and in
 * a final step, that intermediary structure will be actually written.
 * 
 * This will also enable the parsing to be simpler (and faster) as it'll not have to move comments
 * around to try to find a suitable position.
 */
public class PrettyPrinterV2 {

    private IPrettyPrinterPrefs prefs;

    private final int LEVEL_PARENS = 0; //()
    private final int LEVEL_BRACKETS = 1; //[]
    private final int LEVEL_BRACES = 2; //{} 

    public static PrettyPrinterPrefsV2 createDefaultPrefs(IGrammarVersionProvider versionProvider,
            IIndentPrefs indentPrefs, String endLineDelim) {
        if (versionProvider == null) {
            versionProvider = new IGrammarVersionProvider() {

                @Override
                public int getGrammarVersion() throws MisconfigurationException {
                    return IGrammarVersionProvider.LATEST_GRAMMAR_VERSION;
                }

                @Override
                public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions()
                        throws MisconfigurationException {
                    return null;
                }
            };
        }
        PrettyPrinterPrefsV2 prettyPrinterPrefs = new PrettyPrinterPrefsV2(endLineDelim,
                indentPrefs.getIndentationString(), versionProvider);

        prettyPrinterPrefs.setSpacesAfterComma(1);
        prettyPrinterPrefs.setSpacesBeforeComment(1);
        prettyPrinterPrefs.setLinesAfterMethod(1);
        prettyPrinterPrefs.setLinesAfterClass(2);
        prettyPrinterPrefs.setLinesAfterSuite(1);
        return prettyPrinterPrefs;
    }

    public PrettyPrinterV2(IPrettyPrinterPrefs prefs) {
        this.prefs = prefs;
    }

    public static String printArguments(IGrammarVersionProvider versionProvider, argumentsType args) {
        String newLine = "\n";
        String indent = "    ";
        PrettyPrinterPrefsV2 prefsV2 = new PrettyPrinterPrefsV2(newLine, indent, versionProvider);
        prefsV2.setSpacesAfterComma(1);
        PrettyPrinterV2 printerV2 = new PrettyPrinterV2(prefsV2);
        SimpleNode newArgs = args.createCopy();
        String result = "";
        try {
            result = printerV2.print(newArgs);
        } catch (Exception e) {
            Log.log(e);
        }
        while (result.endsWith("\n") || result.endsWith("\r")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;

    }

    //Used while parsing for (maintained across lines)
    private final int[] LEVELS = new int[] { 0, 0, 0 };
    private int statementLevel = 0;
    WriterEraserV2 writerEraserV2;
    WriteStateV2 writeStateV2;
    Set<Entry<Integer, PrettyPrinterDocLineEntry>> entrySet;
    List<Tuple<PrettyPrinterDocLineEntry, String>> previousLines;
    List<LinePartRequireMark> requireMarks = new ArrayList<LinePartRequireMark>();

    //Restarted for each line
    boolean lastWasComment;
    boolean writtenComment;
    boolean savedLineIndent;
    int indentDiff;

    /**
     * This is the method that manages to call everything else correctly to print the ast.
     */
    public String print(SimpleNode ast) throws IOException {
        PrettyPrinterDocV2 doc = new PrettyPrinterDocV2();
        PrettyPrinterVisitorV2 visitor = new PrettyPrinterVisitorV2(prefs, doc);
        if (ast instanceof argumentsType) {
            visitor.pushTupleNeedsParens();
        }
        try {
            visitor.visitNode(ast);
        } catch (Exception e) {
            Log.log(e);
            return "";
        }

        writerEraserV2 = new WriterEraserV2();
        writeStateV2 = new WriteStateV2(writerEraserV2, prefs);

        //Now that the doc is filled, let's make a string from it.
        entrySet = doc.linesToColAndContents.entrySet();
        previousLines = new ArrayList<Tuple<PrettyPrinterDocLineEntry, String>>();

        doc.validateRequireMarks();

        List<Tuple<ILinePart, PrettyPrinterDocLineEntry>> commentsSkipped = new ArrayList<Tuple<ILinePart, PrettyPrinterDocLineEntry>>();

        for (Entry<Integer, PrettyPrinterDocLineEntry> entry : entrySet) {
            PrettyPrinterDocLineEntry line = entry.getValue();
            List<ILinePart> sortedParts = line.getSortedParts();
            indentDiff = line.getIndentDiff();
            savedLineIndent = false;
            List<ILinePart2> sortedPartsWithILinePart2 = getLineParts2(sortedParts);

            lastWasComment = false;
            writtenComment = false;
            if (sortedParts.size() == 0) {
                continue;
            }
            if (sortedPartsWithILinePart2.size() == 1) {
                //Ok, we need a special treatment for lines that only contain comments.
                //As it doesn't belong in the actual AST (it's just spit out in the middle of the parsing),
                //it can happen that it doesn't belong in the current indentation (and rather to the last indentation
                //found), so, we have to go on and check how we should indent it based on the previous line(s)
                ILinePart linePart = sortedPartsWithILinePart2.get(0);

                if (linePart.getToken() instanceof commentType && linePart instanceof ILinePart2) {
                    String indentWritten = handleSingleLineComment((ILinePart2) linePart, line, commentsSkipped);
                    if (indentWritten != null) {
                        saveLineIndent(line, indentWritten);
                    }
                }
            }

            for (ILinePart linePart : sortedParts) {
                writeLinePart(linePart, commentsSkipped, line);
            }

            if (!savedLineIndent) {
                saveLineIndent(line);
            }

            if (statementLevel != 0 && !lastWasComment) {
                if (!isInLevel()) {
                    continue;//don't write the new line if in a statement and not within parenthesis.
                }
            }
            writeStateV2.writeNewLine();
            int newLinesRequired = line.getNewLinesRequired();
            if (newLinesRequired != 0) {
                for (int i = 0; i < newLinesRequired; i++) {
                    writeStateV2.writeNewLine();
                }
            }
        }

        return writerEraserV2.getBuffer().toString();
    }

    private void saveLineIndent(PrettyPrinterDocLineEntry line) {
        savedLineIndent = true;
        previousLines.add(new Tuple<PrettyPrinterDocLineEntry, String>(line, writeStateV2.getIndentString()));
    }

    private void saveLineIndent(PrettyPrinterDocLineEntry line, String indentWritten) {
        savedLineIndent = true;
        previousLines.add(new Tuple<PrettyPrinterDocLineEntry, String>(line, indentWritten));
    }

    private void writeLinePart(ILinePart linePart, List<Tuple<ILinePart, PrettyPrinterDocLineEntry>> commentsSkipped,
            PrettyPrinterDocLineEntry line) throws IOException {
        boolean isSlash = false;
        if (linePart instanceof ILinePart2 && !writtenComment) {
            String tok = ((ILinePart2) linePart).getString();
            if (tok.charAt(0) == ';') {
                writeStateV2.writeNewLine();
                savedLineIndent = true; //don't save line indent
                return;

            } else if (tok.charAt(0) == '\\') {
                if (isInLevel()) {
                    savedLineIndent = true; //don't save line indent
                    return;
                }
                isSlash = true;
            } else if (tok.charAt(0) == '@') {
                writeStateV2.requireNextNewLine();
            }

            if (linePart.getToken() instanceof commentType) {
                if (statementLevel > 0 && !isInLevel()) {
                    commentsSkipped.add(new Tuple<ILinePart, PrettyPrinterDocLineEntry>(linePart, line));
                    savedLineIndent = true; //don't save line indent
                    return;
                }
                writeStateV2.writeSpacesBeforeComment();
            }

            boolean written = false;
            //Note: on a write, if the last thing was a new line, it'll indent.
            if (tok.length() == 1) {
                Tuple<Integer, Boolean> newLevel = updateLevels(tok);
                if (newLevel != null) {
                    if (!savedLineIndent) {
                        saveLineIndent(line);
                    }

                    if (newLevel.o2) {
                        writeStateV2.write(prefs.getReplacement(tok));
                        writeStateV2.indent();
                        written = true;
                    } else {
                        if (indentDiff == 0) {
                            writeStateV2.dedent();
                        }
                        writeStateV2.write(prefs.getReplacement(tok));
                        if (indentDiff != 0) {
                            writeStateV2.dedent();
                        }
                        written = true;
                    }
                }

            }
            if (!written) {
                written = true;
                writeStateV2.write(prefs.getReplacement(tok));
            }
            if (isSlash) {
                writeStateV2.writeNewLine();
            }
            if (linePart.getToken() instanceof commentType) {
                writeStateV2.requireNextNewLine();
                lastWasComment = true;
            } else {
                lastWasComment = false;

            }

        } else if (linePart instanceof ILinePartIndentMark) {
            ILinePartIndentMark indentMark = (ILinePartIndentMark) linePart;
            if (!savedLineIndent) {
                saveLineIndent(line);
            }
            if (indentMark.isIndent()) {
                if (indentMark.getRequireNewLineOnIndent()) {

                    writeStateV2.requireNextNewLineOrComment();
                }
                writeStateV2.indent();
                indentDiff--;
            } else {
                writeStateV2.dedent();
                indentDiff++;
            }

        } else if (linePart instanceof ILinePartStatementMark) {
            ILinePartStatementMark statementMark = (ILinePartStatementMark) linePart;
            if (statementMark.isStart()) {
                if (statementLevel == 0) {
                    writeStateV2.requireNextNewLineOrComment();
                }
                statementLevel++;
            } else {
                statementLevel--;
            }
        }

        if ((statementLevel == 0 || isInLevel()) && commentsSkipped != null && commentsSkipped.size() > 0) {
            savedLineIndent = true; //We don't want to save line indents at this point.
            for (Tuple<ILinePart, PrettyPrinterDocLineEntry> tup : commentsSkipped) {
                writeLinePart(tup.o1, null, tup.o2);
            }
            commentsSkipped.clear();
        }
    }

    /**
     * @return all the line parts that implement ILinePart2
     */
    private List<ILinePart2> getLineParts2(List<ILinePart> sortedParts) {
        List<ILinePart2> sortedPartsWithILinePart2 = new ArrayList<ILinePart2>();
        for (ILinePart p : sortedParts) {
            if (p instanceof ILinePart2) {
                sortedPartsWithILinePart2.add((ILinePart2) p);
            }
        }
        return sortedPartsWithILinePart2;
    }

    /**
     * Handles a single line comment, putting it in the correct indentation.
     * @param line 
     * @param commentsSkipped 
     * @return the indent used or null if it wasn't written.
     */
    private String handleSingleLineComment(ILinePart2 linePart, PrettyPrinterDocLineEntry line,
            List<Tuple<ILinePart, PrettyPrinterDocLineEntry>> commentsSkipped) throws IOException {
        String indent = null;

        if (statementLevel > 0 && !isInLevel()) {
            commentsSkipped.add(new Tuple<ILinePart, PrettyPrinterDocLineEntry>(linePart, line));
            savedLineIndent = true; //don't save line indent
            writtenComment = true; //make it as if we've written it
            return indent;
        }

        ILinePart2 iLinePart2 = linePart;
        commentType commentType = (commentType) linePart.getToken();
        int col = commentType.beginColumn;
        if (col == 1) { //yes, our indexing starts at 1.
            lastWasComment = true;
            writtenComment = true;
            writeStateV2.writeRaw(iLinePart2.getString());
            indent = "";
        } else {
            Tuple<PrettyPrinterDocLineEntry, String> found = null;
            //Let's go backward in the lines to see one that matches the current indentation.
            ListIterator<Tuple<PrettyPrinterDocLineEntry, String>> it = previousLines
                    .listIterator(previousLines.size());
            while (it.hasPrevious() && found == null) {
                Tuple<PrettyPrinterDocLineEntry, String> previous = it.previous();
                int firstCol = previous.o1.getFirstCol();
                if (firstCol != -1) {
                    if (firstCol == col) {
                        found = previous;
                    }
                }
            }

            if (found != null) {
                lastWasComment = true;
                writtenComment = true;
                writeStateV2.writeRaw(found.o2);
                writeStateV2.writeRaw(iLinePart2.getString());
                indent = found.o2;
            }
        }
        return indent;
    }

    /**
     * @return true if we're within parenthesis, brackets or braces
     */
    private boolean isInLevel() {
        for (int i = 0; i < 3; i++) {
            if (this.LEVELS[i] != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the level for parenthesis, brackets and braces based on the passed token and returns the new level and whether
     * it was increased (or null if nothing happened).
     */
    private Tuple<Integer, Boolean> updateLevels(String tok) {
        int use = -1;
        boolean increaseLevel = true;

        switch (tok.charAt(0)) {
            case '(':
            case ')':
                use = this.LEVEL_PARENS;
                break;

            case '[':
            case ']':
                use = this.LEVEL_BRACKETS;
                break;

            case '{':
            case '}':
                use = this.LEVEL_BRACES;
                break;

        }
        ;
        if (use != -1) {
            switch (tok.charAt(0)) {
                case ']':
                case ')':
                case '}':
                    increaseLevel = false;
            }
            ;

            if (increaseLevel) {
                this.LEVELS[use]++;
            } else {
                this.LEVELS[use]--;
            }
            return new Tuple<Integer, Boolean>(LEVELS[use], increaseLevel);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "PrettyPrinterV2[\n" + this.writeStateV2 + "\n]";
    }

}
