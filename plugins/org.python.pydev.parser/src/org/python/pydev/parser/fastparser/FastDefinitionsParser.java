/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.fastparser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.ObjectsInternPool;
import org.python.pydev.core.ObjectsInternPool.ObjectsPoolMap;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.LowMemoryArrayList;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @note: Unfinished
 *
 * This class should be able to gather the definitions found in a module in a very fast way.
 *
 * The target is having a performance around 5x faster than doing a regular parse, focusing on getting
 * the name tokens for:
 *
 * classes, functions, class attributes, instance attributes -- basically the tokens that provide a
 * definition that can be 'globally' accessed.
 *
 * This should work the following way:
 *
 * We should have a single stack where all the statements we find are added. When we find a column
 * which indicates a new statement, we close any statement with a column > than the new statement
 * and in the process add those statements to the parent statement as needed (or in some cases,
 * discard it -- i.e.: method inside method is discarded, but attribute inside method is not).
 *
 * This means that we usually do not put the final element there, but a wrapper which has a body
 * where we can add elements (i.e.: array list), which is converted to a body when its own scope ends.
 *
 * @author Fabio
 */
public final class FastDefinitionsParser {

    private static class NodeEntry {

        public final stmtType node;
        public final List<SimpleNode> body = new LowMemoryArrayList<>();

        public final int logicalColumn;

        /**
         * leadingTabs: how many cols in node.beginColumn were found as tabs.
         */
        public NodeEntry(stmtType stmt, int leadingTabs) {
            this.node = stmt;
            this.logicalColumn = (stmt.beginColumn - leadingTabs) + (leadingTabs * 8);
        }

        /**
         * Assign the body if we have something.
         */
        public void onEndScope() {
            if (body.size() > 0) {
                stmtType[] array = body.toArray(new stmtType[body.size()]);
                if (this.node instanceof ClassDef) {
                    ClassDef classDef = (ClassDef) this.node;
                    classDef.body = array;

                } else if (this.node instanceof FunctionDef) {
                    FunctionDef functionDef = (FunctionDef) this.node;
                    functionDef.body = array;

                } else {
                    String msg = "Assign statement is not expected to have body!";
                    if (throwErrorOnWarnings) {
                        throw new RuntimeException(msg);

                    } else {
                        Log.log(msg);

                    }
                    return;
                }
            }
        }

    }

    /**
     * Set and kept in the constructor
     */

    /**
     * The chars we should iterate through.
     */
    final private char[] cs;

    /**
     * May be null (just used for reporting errors).
     */
    final private File file;

    /**
     * The length of the buffer we're iterating.
     */
    final private int length;

    /**
     * Current iteration index
     */
    private int currIndex = 0;

    /**
     * The current column
     */
    private int col;

    /**
     * How many leading tabs we've found.
     */
    private int leadingTabsInLine;

    /**
     * The current row
     */
    private int row = 0;

    /**
     * The column where the 1st char was found
     */
    private int firstCharCol = 1;

    /**
     * Holds things added to the 'global' module
     */
    private final ArrayList<stmtType> body = new ArrayList<stmtType>(16);

    /**
     * Holds a stack of classes so that we create a new one in each new scope to be filled and when the scope is ended,
     * it should have its body filled with the stackBody contents related to each
     */
    private final FastStack<NodeEntry> stack = new FastStack<NodeEntry>(10);

    /**
     * Buffer with the contents of a line.
     */
    private final FastStringBuffer lineBuffer = new FastStringBuffer();

    private final String moduleName;

    public static boolean throwErrorOnWarnings = false;

    /**
     * Should we debug?
     */
    private final static boolean DEBUG = false;

    private FastDefinitionsParser(char[] cs, String moduleName, File f) {
        this(cs, cs.length, moduleName, f);
    }

    /**
     * Constructor
     *
     * @param cs array of chars that should be considered.
     * @param len the number of chars to be used (usually cs.length).
     */
    private FastDefinitionsParser(char[] cs, int len, String moduleName, File f) {
        this.cs = cs;
        this.length = len;
        this.moduleName = moduleName;
        this.file = f;
    }

    /**
     * This is the method that actually extracts things from the passed buffer.
     * @throws SyntaxErrorException
     */
    private void extractBody() throws SyntaxErrorException {
        ParsingUtils parsingUtils = ParsingUtils.create(cs, false, length);

        if (currIndex < length) {
            handleNewLine(parsingUtils);
        }
        //in the 1st attempt to handle the 1st line, if it had nothing we could actually go backward 1 char
        if (currIndex < 0) {
            currIndex = 0;
        }

        for (; currIndex < length; currIndex++, col++) {
            char c = cs[currIndex];

            switch (c) {

                case '\'':
                case '"':
                    if (DEBUG) {
                        System.out.println("literal");
                    }
                    //go to the end of the literal
                    int initialIndex = currIndex;
                    currIndex = parsingUtils.getLiteralEnd(currIndex, c);

                    //keep the row count correct
                    updateCountRow(initialIndex, currIndex);
                    break;

                case '#':
                    if (DEBUG) {
                        System.out.println("comment");
                    }
                    //go to the end of the comment
                    while (currIndex < length) {
                        c = cs[currIndex];
                        if (c == '\r' || c == '\n') {
                            currIndex--;
                            break;
                        }
                        currIndex++;
                    }

                    break;

                case '{':
                case '[':
                case '(':
                    //starting some call, dict, list, tuple... those don't count on getting some actual definition
                    initialIndex = currIndex;
                    currIndex = parsingUtils.eatPar(currIndex, null, c);

                    //keep the row count correct
                    updateCountRow(initialIndex, currIndex);
                    break;

                case '\r':
                    if (currIndex < length - 1 && cs[currIndex + 1] == '\n') {
                        currIndex++;
                    }
                    /*FALLTHROUGH**/
                case '\n':
                    currIndex++;
                    handleNewLine(parsingUtils);
                    if (currIndex < length) {
                        c = cs[currIndex];
                    }

                    break;

                case '=':
                    if ((currIndex < length - 1 && cs[currIndex + 1] != '=' && currIndex > 0
                            && cs[currIndex - 1] != '=')) {
                        //should not be ==
                        //other cases such as !=, +=, -= are already treated because they don't constitute valid
                        //chars for an identifier.

                        if (DEBUG) {
                            System.out.println("Found possible attribute:" + lineBuffer + " col:" + firstCharCol);
                        }

                        //if we've an '=', let's get the whole line contents to analyze...
                        //Note: should have stopped just before the new line (so, as we'll do currIndex++ in the
                        //next loop, that's ok).
                        initialIndex = currIndex;
                        currIndex = parsingUtils.getFullFlattenedLine(currIndex, lineBuffer);

                        //keep the row count correct
                        updateCountRow(initialIndex, currIndex);

                        String equalsLine = lineBuffer.toString().trim();
                        if (!PySelection.startsWithIndentToken(equalsLine)) {

                            lineBuffer.clear();

                            final List<String> splitted = StringUtils.split(equalsLine, '=');
                            final int splittedLen = splitted.size();
                            ArrayList<exprType> targets = new ArrayList<exprType>(2);
                            for (int j = 0; j < splittedLen - 1 || (splittedLen == 1 && j == 0); j++) { //we don't want to get the last one.
                                int addCols = 0;
                                if (j > 0) {
                                    for (int k = 0; k < j; k++) {
                                        addCols += splitted.get(j).length();
                                        addCols += 1;
                                    }
                                }
                                String lineContents = splitted.get(j).trim();
                                if (lineContents.length() == 0) {
                                    continue;
                                }
                                int colonIndex = lineContents.indexOf(':');
                                if (colonIndex > 0) {
                                    lineContents = lineContents.substring(0, colonIndex);
                                }

                                boolean add = true;
                                int lineContentsLen = lineContents.length();
                                for (int i = 0; i < lineContentsLen; i++) {
                                    char lineC = lineContents.charAt(i);
                                    //can only be made of valid java chars (no spaces or similar things)
                                    if (lineC != '.' && !Character.isJavaIdentifierPart(lineC)) {
                                        add = false;
                                        break;
                                    }
                                }
                                if (add) {
                                    //only add if it was something valid
                                    if (lineContents.indexOf('.') != -1) {
                                        List<String> dotSplit = StringUtils.dotSplit(lineContents);
                                        if (dotSplit.size() == 2 && dotSplit.get(0).equals("self")) {
                                            Name selfName = new Name("self", Name.Load, false);
                                            NameTok attribName = new NameTok(dotSplit.get(1), NameTok.Attrib);
                                            selfName.beginLine = row;
                                            selfName.beginColumn = this.firstCharCol;
                                            attribName.beginLine = row;
                                            attribName.beginColumn = this.firstCharCol;

                                            Attribute attribute = new Attribute(selfName, attribName, Attribute.Load);
                                            attribute.beginLine = row;
                                            attribute.beginColumn = this.firstCharCol;
                                            targets.add(attribute);
                                        }

                                    } else {
                                        Name name = new Name(lineContents, Name.Store, false);
                                        name.beginLine = row;
                                        name.beginColumn = this.firstCharCol + addCols;
                                        targets.add(name);
                                    }
                                }
                            }

                            if (targets.size() > 0) {
                                Assign assign = new Assign(targets.toArray(new exprType[targets.size()]), null, null);
                                assign.beginColumn = this.firstCharCol;
                                assign.beginLine = this.row;
                                stack.push(new NodeEntry(assign, leadingTabsInLine));
                            }
                        }
                    }
                    //No default
            }
            lineBuffer.append(c);
        }

        endScopesInStack(0);
    }

    public void updateCountRow(int initialIndex, int currIndex) {
        char c;
        int len = length;
        for (int k = initialIndex; k < len && k <= currIndex; k++) {
            c = cs[k];
            switch (c) {
                case '\n':
                    row += 1;
                    break;

                case '\r':
                    row += 1;
                    if (k < len - 1 && k <= currIndex - 1) {
                        if (cs[k + 1] == '\n') {
                            k++; //skip the \n after the \r
                        }
                    }
                    break;
            }
        }
    }

    /**
     * Called when a new line is found. Tries to make the match of function and class definitions.
     * @throws SyntaxErrorException
     */
    private void handleNewLine(ParsingUtils parsingUtils) throws SyntaxErrorException {
        if (currIndex >= length - 1) {
            return;
        }

        col = 1;
        leadingTabsInLine = 0;
        row++;
        if (DEBUG) {
            System.out.println("Handling new line:" + row);
        }

        lineBuffer.clear();
        char c = cs[currIndex];

        while (currIndex < length - 1 && Character.isWhitespace(c) && c != '\r' && c != '\n') {
            currIndex++;
            col++;
            if (c == '\t') {
                leadingTabsInLine++;
            }
            c = cs[currIndex];
        }

        if (!Character.isWhitespace(c) && c != '#') {
            endScopesInStack((col - leadingTabsInLine) + (leadingTabsInLine * 8));
        }

        int funcDefDeltaIndex = -1;
        firstCharCol = col;
        if (c == 'c' && matchClass()) {
            int startClassCol = col;
            currIndex += 6;
            col += 6;

            if (this.length <= currIndex) {
                return;
            }
            startClass(getNextIdentifier(c), row, startClassCol, col, leadingTabsInLine);

        } else if ((c == 'd' && (funcDefDeltaIndex = matchFunction()) != -1) ||
                (c == 'a' && (funcDefDeltaIndex = matchAsyncFunction()) != -1)) {
            if (DEBUG) {
                System.out.println("Found method");
            }
            int startMethodCol = col;
            currIndex += funcDefDeltaIndex + 1;
            col += funcDefDeltaIndex + 1;

            if (this.length <= currIndex) {
                return;
            }
            startMethod(getNextIdentifier(c), row, startMethodCol, col, leadingTabsInLine);
        }
        if (currIndex < length) {

            //starting some call, dict, list, tuple... those don't count on getting some actual definition
            int initialIndex = currIndex;

            int tempIndex = skipWhitespaces(currIndex);

            if (tempIndex >= length) {
                return;
            }
            c = cs[tempIndex];

            boolean updateIndex = false;
            switch (c) {
                case '(':
                    tempIndex = parsingUtils.eatPar(tempIndex, null, c);

                    if (tempIndex < length) {
                        tempIndex = skipWhitespaces(tempIndex);

                        c = cs[tempIndex];
                        if (c == ')') {
                            tempIndex++;
                        }
                    }

                    if (tempIndex < length) {
                        tempIndex = skipWhitespaces(tempIndex);

                        c = cs[tempIndex];
                        if (c == ':') {
                            tempIndex++;

                            tempIndex = skipWhitespaces(tempIndex);
                            if (tempIndex < length) {
                                c = cs[tempIndex];
                                if (c != '\r' && c != '\n') {
                                    updateIndex = true;
                                }
                            }
                        }
                    }

                    if (updateIndex) {
                        tempIndex = skipWhitespaces(tempIndex);
                        currIndex = tempIndex;
                        //keep the row count correct
                        updateCountRow(initialIndex, currIndex);

                        //now, update the first char col to be the char after the ':' in "def m2(self):", in a line as
                        //def m2(self): self.a = 10 (all in a single line)
                        int i = tempIndex;
                        while (i > 0 && i < length) {
                            c = cs[i];
                            if (c == '\r' || c == '\n') {
                                break;
                            }
                            i--;
                        }
                        firstCharCol = tempIndex - i;
                    } else {
                        currIndex--;
                    }

                    break;

                default:
                    currIndex--;
                    break;

            }
        }
    }

    /**
     * Note that it'll only skip whitespaces (not newlines)
     */
    private int skipWhitespaces(int tempIndex) {
        char c;
        while (tempIndex < length) {
            c = cs[tempIndex];
            if (c == ' ' || c == '\t') {
                tempIndex++;
            } else {
                break;
            }
        }
        return tempIndex;
    }

    /**
     * Get the next identifier available.
     * @param c the current char
     * @return the identifier found
     */
    private String getNextIdentifier(char c) {
        c = this.cs[currIndex];

        while (currIndex < length && Character.isWhitespace(c)) {
            currIndex++;
            c = this.cs[currIndex];
        }

        int currClassNameCol = currIndex;
        while (Character.isJavaIdentifierPart(c)) {
            currIndex++;
            if (currIndex >= length) {
                break;
            }
            c = this.cs[currIndex];
        }
        return ObjectsInternPool.internLocal(interned,
                new String(this.cs, currClassNameCol, currIndex - currClassNameCol));
    }

    private final ObjectsPoolMap interned = new ObjectsPoolMap();

    /**
     * Start a new method scope with the given row and column.
     * @param startMethodRow the row where the scope should start
     * @param startMethodCol the column where the scope should start
     */
    private void startMethod(String name, int startMethodRow, int startMethodCol, int nameCol, int leadingTabs) {
        NameTok nameTok = new NameTok(name, NameTok.ClassName);
        nameTok.beginLine = startMethodRow;
        nameTok.beginColumn = nameCol;

        FunctionDef functionDef = PyAstFactory.createFunctionDef(nameTok);
        functionDef.beginLine = startMethodRow;
        functionDef.beginColumn = startMethodCol;

        stack.push(new NodeEntry(functionDef, leadingTabs));
    }

    /**
     * Start a new class scope with the given row and column.
     * @param startClassRow the row where the scope should start
     * @param startClassCol the column where the scope should start
     * @param leadingTabsInLine2
     */
    private void startClass(String name, int startClassRow, int startClassCol, int nameCol, int leadingTabs) {
        NameTok nameTok = new NameTok(name, NameTok.ClassName);
        nameTok.beginLine = startClassRow;
        nameTok.beginColumn = nameCol;

        ClassDef classDef = new ClassDef(nameTok, null, null, null, null, null, null);
        classDef.beginLine = startClassRow;
        classDef.beginColumn = startClassCol;

        stack.push(new NodeEntry(classDef, leadingTabs));
    }

    private void endScopesInStack(int currLogicalCol) {
        while (stack.size() > 0) {
            NodeEntry peek = stack.peek();
            if (peek.logicalColumn < currLogicalCol) {
                break;
            }
            NodeEntry currNode = stack.pop();
            currNode.onEndScope();

            if (stack.size() > 0) {
                NodeEntry parentNode = stack.peek();
                if (parentNode.node instanceof FunctionDef) {
                    // Inside a function def, only deal with attributes (if func inside class)
                    if (currNode.node instanceof Assign) {
                        if (stack.size() > 1) {
                            Assign assign = (Assign) currNode.node;
                            exprType target = assign.targets[0];
                            if (target instanceof Attribute) {
                                NodeEntry parentParents = stack.peek(1);
                                if (parentParents.node instanceof ClassDef) {
                                    parentNode.body.add(currNode.node);
                                }
                            }
                        }
                    }
                } else if (parentNode.node instanceof ClassDef) {
                    parentNode.body.add(currNode.node);
                } else {
                    String msg = "Did not expect to find item below node: " + parentNode.node + " (module: "
                            + this.moduleName + " file: " + this.file + " row: " + row
                            + ").";
                    if (throwErrorOnWarnings) {
                        throw new RuntimeException(msg);

                    } else {
                        Log.log(msg);

                    }
                }
            } else {
                body.add(currNode.node);
            }
        }
    }

    /**
     * @return true if we have a match for 'class' in the current index (the 'c' must be already matched at this point)
     */
    private boolean matchClass() {
        return ParsingUtils.matchClass(this.currIndex, this.cs, this.length) != -1;
    }

    /**
     * @return a positive int if we have a match for 'def' in the current index (the 'd' must be already matched at this point)
     * -1 means it was not matched.
     */
    private int matchFunction() {
        int matchAt = ParsingUtils.matchFunction(this.currIndex, this.cs, this.length);
        if (matchAt != -1) {
            return matchAt - this.currIndex;
        }
        return matchAt;
    }

    /**
     * @return a positive int if we have a match for 'async def' in the current index (the 'a' must be already matched at this point)
     * -1 means it was not matched.
     */
    private int matchAsyncFunction() {
        int matchAt = ParsingUtils.matchAsyncFunction(this.currIndex, this.cs, this.length);
        if (matchAt != -1) {
            return matchAt - this.currIndex;
        }
        return matchAt;
    }

    /**
     * Callbacks called just before returning a parsed object. Used for tests
     */
    public static List<ICallback<Object, Tuple<String, SimpleNode>>> parseCallbacks = new ArrayList<ICallback<Object, Tuple<String, SimpleNode>>>();

    /**
     * Convenience method for parse(s.toCharArray())
     * @param s the string to be parsed
     * @return a Module node with the structure found
     */
    public static SimpleNode parse(String s, String moduleName, File f) {
        return parse(s.toCharArray(), moduleName, f);
    }

    /**
     * This method will parse the char array passed and will build a structure with the contents of the file.
     * @param cs the char array to be parsed
     * @return a Module node with the structure found
     */
    public static SimpleNode parse(char[] cs, String moduleName, File f) {
        return parse(cs, moduleName, cs.length, f);
    }

    public static SimpleNode parse(char[] cs, String moduleName, int len, File f) {
        FastDefinitionsParser parser = new FastDefinitionsParser(cs, len, moduleName, f);
        try {
            parser.extractBody();
        } catch (SyntaxErrorException e) {
            throw new RuntimeException(e);
        } catch (StackOverflowError e) {
            RuntimeException runtimeException = new RuntimeException(e);
            Log.log("Error parsing: " + moduleName + " - " + f + "\nContents:\n"
                    + new String(cs, 0, len > 1000 ? 1000 : len),
                    runtimeException); //report at most 1000 chars...
            throw runtimeException;
        }
        List<stmtType> body = parser.body;
        Module ret = new Module(body.toArray(new stmtType[body.size()]));
        ret.beginLine = 1;
        ret.beginColumn = 1;
        if (parseCallbacks.size() > 0) {
            Tuple<String, SimpleNode> arg = new Tuple<String, SimpleNode>(moduleName, ret);
            for (ICallback<Object, Tuple<String, SimpleNode>> c : parseCallbacks) {
                c.call(arg);
            }
        }
        return ret;
    }

    public static SimpleNode parse(String s) {
        return parse(s.toCharArray(), null, null);
    }

}
