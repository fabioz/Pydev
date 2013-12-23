/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

import com.python.pydev.analysis.IAnalysisPreferences;

public abstract class AbstractMessage implements IMessage {

    public static final Map<Integer, String> messages = new HashMap<Integer, String>();

    private final int type;

    private final int severity;

    private IToken generator;

    private List<String> additionalInfo;

    private int startLine = -1;

    /**
     * @param generator needed to get the lines/cols for the message (an alternate constructor is given if 
     * it's already known).
     */
    public AbstractMessage(int type, IToken generator, IAnalysisPreferences prefs) {
        this.severity = prefs.getSeverityForType(type);
        this.type = type;
        this.generator = generator;
        try {
            Assert.isNotNull(generator);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param startLine starts at 1
     * @param endLine starts at 1
     * @param startCol starts at 1
     * @param endCol starts at 1
     */
    public AbstractMessage(int type, int startLine, int endLine, int startCol, int endCol, IAnalysisPreferences prefs) {
        this.severity = prefs.getSeverityForType(type);
        this.type = type;
        Assert.isTrue(startLine >= 0);
        Assert.isTrue(startCol >= 0);
        this.startLine = startLine;
        this.startCol = startCol;
        this.endLine = endLine;
        this.endCol = endCol;
    }

    private String getTypeStr() {
        if (messages.size() == 0) {
            messages.put(IAnalysisPreferences.TYPE_UNUSED_IMPORT, "Unused import: %s");
            messages.put(IAnalysisPreferences.TYPE_UNUSED_WILD_IMPORT, "Unused in wild import: %s");
            messages.put(IAnalysisPreferences.TYPE_UNUSED_VARIABLE, "Unused variable: %s");
            messages.put(IAnalysisPreferences.TYPE_UNUSED_PARAMETER, "Unused parameter: %s");
            messages.put(IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE, "Undefined variable: %s");
            messages.put(IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE, "Duplicated signature: %s");
            messages.put(IAnalysisPreferences.TYPE_REIMPORT, "Import redefinition: %s");
            messages.put(IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT, "Unresolved import: %s");
            messages.put(IAnalysisPreferences.TYPE_NO_SELF, "Method '%s' should have %s as first parameter");
            messages.put(IAnalysisPreferences.TYPE_UNDEFINED_IMPORT_VARIABLE, "Undefined variable from import: %s");
            messages.put(IAnalysisPreferences.TYPE_NO_EFFECT_STMT, "Statement apppears to have no effect");
            messages.put(IAnalysisPreferences.TYPE_INDENTATION_PROBLEM, "%s");
            messages.put(IAnalysisPreferences.TYPE_ASSIGNMENT_TO_BUILT_IN_SYMBOL,
                    "Assignment to reserved built-in symbol: %s");
            messages.put(IAnalysisPreferences.TYPE_PEP8, "%s");
            messages.put(IAnalysisPreferences.TYPE_ARGUMENTS_MISATCH, "%s");
        }
        return messages.get(getType());

    }

    public int getSeverity() {
        return severity;
    }

    public int getType() {
        return type;
    }

    public int getStartLine(IDocument doc) {
        if (startLine < 0) {
            startLine = getStartLine(generator, doc);
        }
        return startLine;
    }

    /**
     * @return the starting line fro the given token (starting at 1)
     */
    public static int getStartLine(IToken generator, IDocument doc) {
        return getStartLine(generator, doc, generator.getRepresentation());
    }

    public static int getStartLine(IToken generator, IDocument doc, String shortMessage) {
        return getStartLine(generator, doc, shortMessage, false);

    }

    public static int getStartLine(IToken generator, IDocument doc, String shortMessage, boolean returnAsName) {
        if (!generator.isImport()) {
            return generator.getLineDefinition();
        }

        //ok, it is an import... (can only be a source token)
        SourceToken s = (SourceToken) generator;

        SimpleNode ast = s.getAst();
        if (ast instanceof ImportFrom) {
            ImportFrom i = (ImportFrom) ast;
            //if it is a wild import, it starts on the module name
            if (AbstractVisitor.isWildImport(i)) {
                return i.module.beginLine;
            } else {
                //no wild import, let's check the 'as name'
                return getNameForRepresentation(i, shortMessage, returnAsName).beginLine;
            }

        } else if (ast instanceof Import) {
            return getNameForRepresentation(ast, shortMessage, returnAsName).beginLine;

        } else {
            throw new RuntimeException("It is not an import");
        }
    }

    int startCol = -1;

    /**
     * gets the start col of the message (starting at 1)
     *  
     * @see com.python.pydev.analysis.messages.IMessage#getStartCol(org.eclipse.jface.text.IDocument)
     */
    public int getStartCol(IDocument doc) {
        if (startCol >= 0) {
            return startCol;
        }
        startCol = getStartCol(generator, doc, getShortMessageStr());
        return startCol;

    }

    private String getShortMessageStr() {
        Object msg = getShortMessage();
        if (msg instanceof Object[]) {
            Object[] msgs = (Object[]) msg;
            FastStringBuffer buffer = new FastStringBuffer();
            for (Object o : msgs) {
                buffer.append(o.toString());
            }
            return buffer.toString();
        } else {
            return msg.toString();
        }
    }

    /**
     * @return the starting column for the given token (1-based)
     */
    public static int getStartCol(IToken generator, IDocument doc) {
        return getStartCol(generator, doc, generator.getRepresentation());
    }

    /**
     * @return the starting column for the given token (1-based)
     */
    public static int getStartCol(IToken generator, IDocument doc, String shortMessage) {
        return getStartCol(generator, doc, shortMessage, false);

    }

    /**
     * @return the starting column for the given token (1-based)
     */
    public static int getStartCol(IToken generator, IDocument doc, String shortMessage, boolean returnAsName) {

        //not import...
        if (!generator.isImport()) {
            return generator.getColDefinition();
        }

        //ok, it is an import... (can only be a source token)
        SourceToken s = (SourceToken) generator;

        SimpleNode ast = s.getAst();
        if (ast instanceof ImportFrom) {
            ImportFrom i = (ImportFrom) ast;
            //if it is a wild import, it starts on the module name
            if (AbstractVisitor.isWildImport(i)) {
                return i.module.beginColumn;
            } else {
                //no wild import, let's check the 'as name'
                return getNameForRepresentation(i, shortMessage, returnAsName).beginColumn;
            }

        } else if (ast instanceof Import) {
            return getNameForRepresentation(ast, shortMessage, returnAsName).beginColumn;

        } else {
            throw new RuntimeException("It is not an import");
        }
    }

    /**
     * @param imp this is the import ast
     * @param fullRep this is the representation we are looking for
     * @param returnAsName defines if we should return the asname or only the name (depending on what we are
     * analyzing -- the start or the end of the representation).
     * 
     * @return the name tok for the representation in a given import
     */
    private static NameTok getNameForRepresentation(SimpleNode imp, String rep, boolean returnAsName) {

        aliasType[] names;
        if (imp instanceof Import) {
            names = ((Import) imp).names;
        } else if (imp instanceof ImportFrom) {
            names = ((ImportFrom) imp).names;
        } else {
            throw new RuntimeException("import expected");
        }

        for (aliasType alias : names) {
            if (alias.asname != null) {
                if (((NameTok) alias.asname).id.equals(rep)) {
                    if (returnAsName) {
                        return (NameTok) alias.asname;
                    } else {
                        return (NameTok) alias.name;
                    }
                }
            } else { //let's check for the name

                String fullRepNameId = ((NameTok) alias.name).id;

                //we have to get all representations, since an import such as import os.path would 
                //have to match os and os.path
                for (String repId : new FullRepIterable(fullRepNameId)) {

                    if (repId.equals(rep)) {
                        return (NameTok) alias.name;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @see com.python.pydev.analysis.messages.IMessage#getEndLine(org.eclipse.jface.text.IDocument)
     */
    int endLine = -1;

    public int getEndLine(IDocument doc) {
        return getEndLine(doc, true);
    }

    public int getEndLine(IDocument doc, boolean getOnlyToFirstDot) {
        if (endLine < 0) {
            endLine = getEndLine(generator, doc, getOnlyToFirstDot);
        }
        return endLine;

    }

    public static int getEndLine(IToken generator, IDocument doc, boolean getOnlyToFirstDot) {
        if (generator instanceof SourceToken) {
            if (!generator.isImport()) {
                return ((SourceToken) generator).getLineEnd(getOnlyToFirstDot);
            }
            return getStartLine(generator, doc); //for an import, the endline == startline

        } else {
            return -1;
        }
    }

    int endCol = -1;

    public int getEndCol(IDocument doc) {
        return getEndCol(doc, true);
    }

    public int getEndCol(IDocument doc, boolean getOnlyToFirstDot) {
        if (endCol >= 0) {
            return endCol;
        }
        endCol = getEndCol(generator, doc, getShortMessageStr(), getOnlyToFirstDot);
        return endCol;

    }

    /**
     * @param generator is the token that generated this message
     * @param doc is the document where this message will be put
     * @param shortMessage is used when it is an import ( = foundTok.getRepresentation())
     * 
     * @return the end column for this message
     *  
     * @see com.python.pydev.analysis.messages.IMessage#getEndCol(org.eclipse.jface.text.IDocument)
     */
    public static int getEndCol(IToken generator, IDocument doc, String shortMessage, boolean getOnlyToFirstDot) {
        int endCol = -1;
        if (generator.isImport()) {
            //ok, it is an import... (can only be a source token)
            SourceToken s = (SourceToken) generator;

            SimpleNode ast = s.getAst();

            if (ast instanceof ImportFrom) {
                ImportFrom i = (ImportFrom) ast;
                //ok, now, this depends on the name
                NameTok it = getNameForRepresentation(i, shortMessage, true);
                if (it != null) {
                    endCol = it.beginColumn + shortMessage.length();
                    return endCol;
                }

                //if still not returned, it is a wild import... find the '*'
                try {
                    IRegion lineInformation = doc.getLineInformation(i.module.beginLine - 1);
                    //ok, we have the line... now, let's find the absolute offset
                    int absolute = lineInformation.getOffset() + i.module.beginColumn - 1;
                    while (doc.getChar(absolute) != '*') {
                        absolute++;
                    }
                    int absoluteCol = absolute + 1; //1 for the *
                    IRegion region = doc.getLineInformationOfOffset(absoluteCol);
                    endCol = absoluteCol - region.getOffset() + 1; //1 because we should return as if starting in 1 and not 0
                    return endCol;
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }

            } else if (ast instanceof Import) {
                NameTok it = getNameForRepresentation(ast, shortMessage, true);
                endCol = it.beginColumn + shortMessage.length();
                return endCol;
            } else {
                throw new RuntimeException("It is not an import");
            }
        }

        //no import... make it regular
        if (generator instanceof SourceToken) {
            return ((SourceToken) generator).getColEnd(getOnlyToFirstDot);
        }
        return -1;
    }

    @Override
    public String toString() {
        return getMessage();
    }

    public List<String> getAdditionalInfo() {
        return additionalInfo;
    }

    public void addAdditionalInfo(String info) {
        if (this.additionalInfo == null) {
            this.additionalInfo = new ArrayList<String>();
        }
        this.additionalInfo.add(info);
    }

    String message = null;

    public String getMessage() {
        if (message != null) {
            return message;
        }

        String typeStr = getTypeStr();
        if (typeStr == null) {
            throw new AssertionError("Unable to get message for type: " + getType());
        }
        Object shortMessage = getShortMessage();
        if (shortMessage == null) {
            throw new AssertionError("Unable to get shortMessage (" + typeStr + ")");
        }
        if (shortMessage instanceof Object[]) {
            Object[] o = (Object[]) shortMessage;

            //if we have the same number of %s as objects in the array, make the format
            int countPercS = StringUtils.countPercS(typeStr);
            if (countPercS == o.length) {
                return StringUtils.format(typeStr, o);

            } else if (countPercS == 1) {
                //if we have only 1, all parameters should be concatenated in a single string
                FastStringBuffer buf = new FastStringBuffer();
                for (int i = 0; i < o.length; i++) {
                    buf.append(o[i].toString());
                    if (i != o.length - 1) {
                        buf.append(" ");
                    }
                }
                shortMessage = buf.toString();

            } else {
                throw new AssertionError("The number of %s is not the number of passed parameters nor 1");
            }
        }
        message = StringUtils.format(typeStr, shortMessage);
        return message;
    }

    public IToken getGenerator() {
        return generator;
    }
}
