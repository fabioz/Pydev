/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 13/07/2005
 */
package org.python.pydev.parser.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.UnpackInfo;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.ISpecialStr;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.BinOp;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.DictComp;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.comprehensionType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorWithLoop;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.utils.Reflection;

public class NodeUtils {

    /**
     * @param node a function definition (if other will return an empty string)
     * @return a string with the representation of the parameters of the function
     */
    public static String getNodeArgs(SimpleNode node) {
        if (node instanceof ClassDef) {
            node = getClassDefInit((ClassDef) node);
        }

        if (node instanceof FunctionDef) {
            FunctionDef f = (FunctionDef) node;

            String startPar = "( ";
            FastStringBuffer buffer = new FastStringBuffer(startPar, 40);

            for (int i = 0; i < f.args.args.length; i++) {
                if (buffer.length() > startPar.length()) {
                    buffer.append(", ");
                }
                buffer.append(getRepresentationString(f.args.args[i]));
            }
            buffer.append(" )");
            return buffer.toString();
        }
        return "";
    }

    public static String getFullArgs(SimpleNode ast) {
        if (ast != null) {
            if (ast instanceof ClassDef) {
                ast = NodeUtils.getClassDefInit((ClassDef) ast);
            }
            if (ast instanceof FunctionDef) {
                FunctionDef functionDef = (FunctionDef) ast;
                if (functionDef.args != null) {
                    String printed = PrettyPrinterV2.printArguments(new IGrammarVersionProvider() {

                        public int getGrammarVersion() throws MisconfigurationException {
                            return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0;
                        }
                    }, functionDef.args);
                    if (printed != null) {
                        if (!printed.startsWith("(") || !printed.endsWith(")")) {
                            printed = "(" + printed + ")";
                        }
                        return printed;
                    }
                }
            }
        }
        return "";
    }

    public static SimpleNode getClassDefInit(ClassDef classDef) {
        for (stmtType t : classDef.body) {
            if (t instanceof FunctionDef) {
                FunctionDef def = (FunctionDef) t;
                if (((NameTok) def.name).id.equals("__init__")) {
                    return def;
                }
            }
        }
        return null;
    }

    /**
     * Get the representation for the passed parameter (if it is a String, it is itself, if it
     * is a SimpleNode, get its representation
     */
    private static String discoverRep(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        if (o instanceof NameTok) {
            return ((NameTok) o).id;
        }
        if (o instanceof SimpleNode) {
            return getRepresentationString((SimpleNode) o);
        }
        throw new RuntimeException("Expecting a String or a SimpleNode");
    }

    public static String getRepresentationString(SimpleNode node) {
        return getRepresentationString(node, false);
    }

    /**
     * @param node this is the node from whom we want to get the representation
     * @return A suitable String representation for some node.
     */
    public static String getRepresentationString(SimpleNode node, boolean useTypeRepr) {
        if (node instanceof NameTok) {
            NameTok tok = (NameTok) node;
            return tok.id;
        }

        if (node instanceof Name) {
            Name name = (Name) node;
            return name.id;
        }

        if (node instanceof aliasType) {
            aliasType type = (aliasType) node;
            return ((NameTok) type.name).id;
        }
        if (node instanceof Attribute) {
            Attribute attribute = (Attribute) node;
            return discoverRep(attribute.attr);

        }

        if (node instanceof keywordType) {
            keywordType type = (keywordType) node;
            return discoverRep(type.arg);
        }

        if (node instanceof ClassDef) {
            ClassDef def = (ClassDef) node;
            return ((NameTok) def.name).id;
        }

        if (node instanceof FunctionDef) {
            FunctionDef def = (FunctionDef) node;
            return ((NameTok) def.name).id;
        }

        if (node instanceof Call) {
            Call call = ((Call) node);
            return getRepresentationString(call.func, useTypeRepr);
        }

        if (node instanceof org.python.pydev.parser.jython.ast.List || node instanceof ListComp) {
            String val = "[]";
            if (useTypeRepr) {
                val = getBuiltinType(val);
            }
            return val;
        }

        if (node instanceof org.python.pydev.parser.jython.ast.Dict
                || node instanceof org.python.pydev.parser.jython.ast.DictComp) {
            String val = "{}";
            if (useTypeRepr) {
                val = getBuiltinType(val);
            }
            return val;
        }

        if (node instanceof BinOp) {
            BinOp binOp = (BinOp) node;
            if (binOp.left instanceof Str && binOp.op == BinOp.Mod) {
                node = binOp.left;
                //Just change the node... the check below will work with the Str already.
            }
        }

        if (node instanceof Str) {
            String val;
            if (useTypeRepr) {
                val = getBuiltinType("''");
            } else {
                val = "'" + ((Str) node).s + "'";
            }
            return val;
        }

        if (node instanceof Tuple) {
            StringBuffer buf = new StringBuffer();
            Tuple t = (Tuple) node;
            for (exprType e : t.elts) {
                buf.append(getRepresentationString(e, useTypeRepr));
                buf.append(", ");
            }
            if (t.elts.length > 0) {
                int l = buf.length();
                buf.deleteCharAt(l - 1);
                buf.deleteCharAt(l - 2);
            }
            String val = "(" + buf + ")";
            if (useTypeRepr) {
                val = getBuiltinType(val);
            }
            return val;
        }

        if (node instanceof Num) {
            String val = ((Num) node).n.toString();
            if (useTypeRepr) {
                val = getBuiltinType(val);
            }
            return val;
        }

        if (node instanceof Import) {
            aliasType[] names = ((Import) node).names;
            for (aliasType n : names) {
                if (n.asname != null) {
                    return ((NameTok) n.asname).id;
                }
                return ((NameTok) n.name).id;
            }
        }

        if (node instanceof commentType) {
            commentType type = (commentType) node;
            return type.id;
        }

        if (node instanceof excepthandlerType) {
            excepthandlerType type = (excepthandlerType) node;
            return type.name.toString();

        }

        return null;
    }

    /**
     * @param node
     * @param t
     */
    public static String getNodeDocString(SimpleNode node) {
        Str s = getNodeDocStringNode(node);
        if (s != null) {
            return s.s;
        }
        return null;
    }

    public static Str getNodeDocStringNode(SimpleNode node) {
        Str s = null;
        stmtType body[] = null;
        if (node instanceof FunctionDef) {
            FunctionDef def = (FunctionDef) node;
            body = def.body;
        } else if (node instanceof ClassDef) {
            ClassDef def = (ClassDef) node;
            body = def.body;

        }
        if (body != null && body.length > 0) {
            if (body[0] instanceof Expr) {
                Expr e = (Expr) body[0];
                if (e.value instanceof Str) {
                    s = (Str) e.value;
                }
            }
        }
        return s;
    }

    public static String getFullRepresentationString(SimpleNode node) {
        return getFullRepresentationString(node, false);
    }

    public static String getFullRepresentationString(SimpleNode node, boolean fullOnSubscriptOrCall) {
        if (node instanceof Dict || node instanceof DictComp) {
            return "dict";
        }

        if (node instanceof Str || node instanceof Num) {
            return getRepresentationString(node, true);
        }

        if (node instanceof Tuple) {
            return getRepresentationString(node, true);
        }

        if (node instanceof Subscript) {
            return getFullRepresentationString(((Subscript) node).value);
        }

        if (node instanceof Call) {
            Call c = (Call) node;
            node = c.func;
            if (Reflection.hasAttr(node, "value") && Reflection.hasAttr(node, "attr")) {
                return getFullRepresentationString((SimpleNode) Reflection.getAttrObj(node, "value")) + "."
                        + discoverRep(Reflection.getAttrObj(node, "attr"));
            }
        }

        if (node instanceof Attribute) {
            //attributes are tricky because we only have backwards access initially, so, we have to:

            //get it forwards
            List<SimpleNode> attributeParts = getAttributeParts((Attribute) node);
            StringBuffer buf = new StringBuffer();
            for (Object part : attributeParts) {
                if (part instanceof Call) {
                    //stop on a call (that's what we usually want, since the end will depend on the things that
                    //return from the call).
                    if (!fullOnSubscriptOrCall) {
                        return buf.toString();
                    } else {
                        buf.append("()");//call
                    }

                } else if (part instanceof Subscript) {
                    if (!fullOnSubscriptOrCall) {
                        //stop on a subscript : e.g.: in bb.cc[10].d we only want the bb.cc part
                        return getFullRepresentationString(((Subscript) part).value);
                    } else {
                        buf.append(getFullRepresentationString(((Subscript) part).value));
                        buf.append("[]");//subscript access
                    }

                } else {
                    //otherwise, just add another dot and keep going.
                    if (buf.length() > 0) {
                        buf.append(".");
                    }
                    buf.append(getRepresentationString((SimpleNode) part, true));
                }
            }
            return buf.toString();

        }

        if (node instanceof BinOp) {
            BinOp binOp = (BinOp) node;
            if (binOp.left instanceof Str && binOp.op == BinOp.Mod) {
                //It's something as 'aaa' % (1,2), so, we know it's a string.
                return getRepresentationString(node, true);
            }
        }

        return getRepresentationString(node, true);
    }

    /**
     * line and col start at 1
     */
    public static boolean isWithin(int line, int col, SimpleNode node) {
        int colDefinition = NodeUtils.getColDefinition(node);
        int lineDefinition = NodeUtils.getLineDefinition(node);
        int[] colLineEnd = NodeUtils.getColLineEnd(node, false);

        if (lineDefinition <= line && colDefinition <= col && colLineEnd[0] >= line && colLineEnd[1] >= col) {
            return true;
        }
        return false;
    }

    public static SimpleNode getNameTokFromNode(SimpleNode ast2) {
        if (ast2 instanceof ClassDef) {
            ClassDef c = (ClassDef) ast2;
            return c.name;
        }
        if (ast2 instanceof FunctionDef) {
            FunctionDef c = (FunctionDef) ast2;
            return c.name;
        }
        return ast2;

    }

    public static int getNameLineDefinition(SimpleNode ast2) {
        return getLineDefinition(getNameTokFromNode(ast2));
    }

    public static int getNameColDefinition(SimpleNode ast2) {
        return getColDefinition(getNameTokFromNode(ast2));
    }

    /**
     * @param ast2 the node to work with
     * @return the line definition of a node
     */
    public static int getLineDefinition(SimpleNode ast2) {
        while (ast2 instanceof Attribute) {
            exprType val = ((Attribute) ast2).value;
            if (!(val instanceof Call)) {
                ast2 = val;
            } else {
                break;
            }
        }
        if (ast2 instanceof FunctionDef) {
            return ((FunctionDef) ast2).name.beginLine;
        }
        if (ast2 instanceof ClassDef) {
            return ((ClassDef) ast2).name.beginLine;
        }
        return ast2.beginLine;
    }

    public static int getColDefinition(SimpleNode ast2) {
        return getColDefinition(ast2, true);
    }

    /**
     * @param ast2 the node to work with
     * @return the column definition of a node
     */
    public static int getColDefinition(SimpleNode ast2, boolean always1ForImports) {
        if (ast2 instanceof Attribute) {
            //if it is an attribute, we always have to move backward to the first defined token (Attribute.value)
            exprType value = ((Attribute) ast2).value;
            return getColDefinition(value);
        }

        //call and subscript are special cases, because they are not gotten directly (we have to go to the first
        //part of it (which in turn may be an attribute)
        else if (ast2 instanceof Call) {
            Call c = (Call) ast2;
            return getColDefinition(c.func);

        } else if (ast2 instanceof Subscript) {
            Subscript s = (Subscript) ast2;
            return getColDefinition(s.value);

        } else if (always1ForImports) {
            if (ast2 instanceof Import || ast2 instanceof ImportFrom) {
                return 1;
            }
        }
        return getClassOrFuncColDefinition(ast2);
    }

    public static int getClassOrFuncColDefinition(SimpleNode ast2) {
        if (ast2 instanceof ClassDef) {
            ClassDef def = (ClassDef) ast2;
            return def.name.beginColumn;
        }
        if (ast2 instanceof FunctionDef) {
            FunctionDef def = (FunctionDef) ast2;
            return def.name.beginColumn;
        }
        return ast2.beginColumn;
    }

    public static int[] getColLineEnd(SimpleNode v) {
        return getColLineEnd(v, true);
    }

    /**
     * @param v the token to work with
     * @return a tuple with [line, col] of the definition of a token
     */
    public static int[] getColLineEnd(SimpleNode v, boolean getOnlyToFirstDot) {
        int lineEnd = getLineEnd(v);
        int col = 0;

        if (v instanceof Import || v instanceof ImportFrom) {
            return new int[] { lineEnd, -1 }; //col is -1... import is always full line
        }

        if (v instanceof Str) {
            if (lineEnd == getLineDefinition(v)) {
                String s = ((Str) v).s;
                col = getColDefinition(v) + s.length();
                return new int[] { lineEnd, col };
            } else {
                //it is another line...
                String s = ((Str) v).s;
                int i = s.lastIndexOf('\n');
                String sub = s.substring(i, s.length());

                col = sub.length();
                return new int[] { lineEnd, col };
            }
        }

        col = getEndColFromRepresentation(v, getOnlyToFirstDot);
        return new int[] { lineEnd, col };
    }

    /**
     * @param v
     * @return
     */
    private static int getEndColFromRepresentation(SimpleNode v, boolean getOnlyToFirstDot) {
        int col;
        String representationString = getFullRepresentationString(v);
        if (representationString == null) {
            return -1;
        }

        if (getOnlyToFirstDot) {
            int i;
            if ((i = representationString.indexOf('.')) != -1) {
                representationString = representationString.substring(0, i);
            }
        }

        int colDefinition = getColDefinition(v);
        if (colDefinition == -1) {
            return -1;
        }

        col = colDefinition + representationString.length();
        return col;
    }

    public static int getLineEnd(SimpleNode v) {
        if (v instanceof Expr) {
            Expr expr = (Expr) v;
            v = expr.value;
        }
        if (v instanceof ImportFrom) {
            ImportFrom f = (ImportFrom) v;
            FindLastLineVisitor findLastLineVisitor = new FindLastLineVisitor();
            try {
                f.accept(findLastLineVisitor);
                SimpleNode lastNode = findLastLineVisitor.getLastNode();
                ISpecialStr lastSpecialStr = findLastLineVisitor.getLastSpecialStr();
                if (lastSpecialStr != null && lastSpecialStr.toString().equals(")")) {
                    //it was an from xxx import (euheon, utehon)
                    return lastSpecialStr.getBeginLine();
                } else {
                    return lastNode.beginLine;
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        if (v instanceof Import) {
            Import f = (Import) v;
            FindLastLineVisitor findLastLineVisitor = new FindLastLineVisitor();
            try {
                f.accept(findLastLineVisitor);
                SimpleNode lastNode = findLastLineVisitor.getLastNode();
                return lastNode.beginLine;
            } catch (Exception e) {
                Log.log(e);
            }
        }
        if (v instanceof Str) {
            String s = ((Str) v).s;
            int found = 0;
            for (int i = 0; i < s.length(); i++) {

                if (s.charAt(i) == '\n') {
                    found += 1;
                }
            }
            return getLineDefinition(v) + found;
        }
        return getLineDefinition(v);
    }

    /**
     * @return the builtin type (if any) for some token (e.g.: '' would return str, 1.0 would return float...
     */
    public static String getBuiltinType(String tok) {
        if (tok.endsWith("'") || tok.endsWith("\"")) {
            //ok, we are getting code completion for a string.
            return "str";

        } else if (tok.endsWith("]") && tok.startsWith("[")) {
            //ok, we are getting code completion for a list.
            return "list";

        } else if (tok.endsWith("}") && tok.startsWith("{")) {
            //ok, we are getting code completion for a dict.
            return "dict";

        } else if (tok.endsWith(")") && tok.startsWith("(")) {
            //ok, we are getting code completion for a tuple.
            return "tuple";

        } else {
            try {
                Integer.parseInt(tok);
                return "int";
            } catch (Exception e) { //ok, not parsed as int
            }

            try {
                Float.parseFloat(tok);
                return "float";
            } catch (Exception e) { //ok, not parsed as int
            }
        }

        return null;
    }

    public static String getNameFromNameTok(NameTokType tok) {
        return ((NameTok) tok).id;
    }

    public static String getNameFromNameTok(NameTok tok) {
        return tok.id;
    }

    /**
     * Gets all the parts contained in some attribute in the right order (when we visit
     * some attribute, we have to get that in a backwards fashion, since the attribute
     * is only determined in the end of the token in the grammar)
     *
     * @return a list with the attribute parts in its forward order, and not backward as presented
     * in the grammar.
     */
    public static List<SimpleNode> getAttributeParts(Attribute node) {
        ArrayList<SimpleNode> nodes = new ArrayList<SimpleNode>();

        nodes.add(node.attr);
        SimpleNode s = node.value;

        while (true) {
            if (s instanceof Attribute) {
                nodes.add(s);
                s = ((Attribute) s).value;

            } else if (s instanceof Call) {
                nodes.add(s);
                s = ((Call) s).func;

            } else {
                nodes.add(s);
                break;
            }
        }

        Collections.reverse(nodes);

        return nodes;
    }

    /**
     * Gets the parent names for a class definition
     *
     * @param onlyLastSegment determines whether we should return only the last segment if the name
     * of the parent resolves to a dotted name.
     */
    public static List<String> getParentNames(ClassDef def, boolean onlyLastSegment) {
        ArrayList<String> ret = new ArrayList<String>();
        for (exprType base : def.bases) {
            String rep = getFullRepresentationString(base);
            if (onlyLastSegment) {
                rep = FullRepIterable.getLastPart(rep);
            }
            ret.add(rep);
        }
        return ret;
    }

    /**
     * @return true if the node is an import node (and false otherwise).
     */
    public static boolean isImport(SimpleNode ast) {
        if (ast instanceof Import || ast instanceof ImportFrom) {
            return true;
        }
        return false;
    }

    /**
     * @return true if the node is a comment import node (and false otherwise).
     */
    public static boolean isComment(SimpleNode ast) {
        if (ast instanceof commentType) {
            return true;
        }
        return false;
    }

    public static NameTok getNameForAlias(aliasType t) {
        if (t.asname != null) {
            return (NameTok) t.asname;
        } else {
            return (NameTok) t.name;
        }
    }

    public static NameTok getNameForRep(aliasType[] names, String representation) {
        for (aliasType name : names) {
            NameTok nameForAlias = getNameForAlias(name);
            String aliasRep = NodeUtils.getRepresentationString(nameForAlias);
            if (representation.equals(aliasRep)) {
                return nameForAlias;
            }
        }
        return null;
    }

    /**
     * @param lineNumber the line we want to get the context from (starts at 0)
     * @param ast the ast that corresponds to our context
     * @return the full name for the context where we are (in the format Class.method.xxx.xxx)
     */
    public static String getContextName(int lineNumber, SimpleNode ast) {
        if (ast != null) {
            EasyASTIteratorVisitor visitor = EasyASTIteratorVisitor.create(ast);
            Iterator<ASTEntry> classesAndMethodsIterator = visitor.getClassesAndMethodsIterator();
            ASTEntry last = null;
            while (classesAndMethodsIterator.hasNext()) {
                ASTEntry entry = classesAndMethodsIterator.next();
                if (entry.node.beginLine > lineNumber + 1) {
                    //ok, now, let's find out which context actually contains it...
                    break;
                }
                last = entry;
            }

            while (last != null && last.endLine <= lineNumber) {
                last = last.parent;
            }

            if (last != null) {
                return getFullMethodName(last);
            }
        }
        return null;
    }

    /**
     * @param ASTEntry last
     * @return classdef.method_name
     */
    public static String getFullMethodName(ASTEntry last) {
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        while (last != null) {
            String name = last.getName();
            buffer.insert(0, name);
            last = last.parent;
            if (!first) {
                buffer.insert(name.length(), ".");
            }
            first = false;
        }
        return buffer.toString();
    }

    /**
     * Identifies the context for both source and target line
     *
     * @param ASTEntry
     *            ast
     * @param int sourceLine: the line at which debugger is stopped currently
     *        (starts at 1)
     * @param int targetLine: the line at which we need to set next (starts at
     *        0)
     * @return
     */
    public static boolean isValidContextForSetNext(SimpleNode ast, int sourceLine, int targetLine) {
        String sourceFunctionName = NodeUtils.getContextName((sourceLine - 1), ast);
        String targetFunctionName = NodeUtils.getContextName(targetLine, ast);
        if (compareMethodName(sourceFunctionName, targetFunctionName)) {
            ASTEntry sourceAST = NodeUtils.getLoopContextName(sourceLine, ast);
            ASTEntry targetAST = NodeUtils.getLoopContextName(targetLine + 1, ast);

            if (targetAST == null) {
                return true; // Target line is not inside some loop
            }
            if (isValidElseBlock(sourceAST, targetAST, sourceLine, targetLine)) {
                return true; // Debug pointer can be set inside else block of
                             // for..else/while..else
            }
            if (sourceAST == null && targetAST != null) {
                return false; // Source is outside loop and target is inside
                              // loop
            }
            if (sourceAST != null && targetAST != null) {
                // Both Source and Target is inside some loop
                if (sourceAST.equals(targetAST)) {
                    return isValidInterLoopContext(sourceLine, targetLine, sourceAST, targetAST);
                } else {
                    ASTEntry last = sourceAST;
                    boolean retVal = false;
                    while (last != null) {
                        ASTEntry parentAST = last.parent;
                        if (parentAST != null && parentAST.equals(targetAST)) {
                            retVal = true;
                            break;
                        }
                        last = parentAST;
                    }
                    return retVal;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Compare name of two methods. return true if either both methods are same
     * or global context
     *
     * @param sourceMethodName
     * @param targetMethodName
     * @return
     */
    public static boolean compareMethodName(String sourceMethodName, String targetMethodName) {
        if ((sourceMethodName == null && targetMethodName == null)) {
            return true;
        }
        if ((sourceMethodName != null) && sourceMethodName.equals(targetMethodName)) {
            return true;
        }
        return false;
    }

    /**
     * Identifies the for/while/try..except/try..finally and with for a provided
     * line number.
     *
     * @param lineNumber
     *            the line we want to get the loop context (starts at 1)
     * @param ast
     * @return
     */
    public static ASTEntry getLoopContextName(int lineNumber, SimpleNode ast) {
        ASTEntry loopContext = null;
        if (ast != null) {
            int highestBeginLine = 0;
            ArrayList<ASTEntry> contextBlockList = new ArrayList<ASTEntry>();
            EasyASTIteratorWithLoop visitor = EasyASTIteratorWithLoop.create(ast);
            Iterator<ASTEntry> blockIterator = visitor.getIterators();
            while (blockIterator.hasNext()) {
                ASTEntry entry = blockIterator.next();
                if ((entry.node.beginLine) < lineNumber && entry.endLine >= lineNumber) {
                    contextBlockList.add(entry);
                    if (entry.node.beginLine > highestBeginLine) {
                        highestBeginLine = entry.node.beginLine;
                    }
                }
            }
            Iterator<ASTEntry> contextBlockListIterator = contextBlockList.iterator();
            while (contextBlockListIterator.hasNext()) {
                ASTEntry astEntry = contextBlockListIterator.next();
                if (astEntry.node.beginLine == highestBeginLine) {
                    loopContext = astEntry;
                }
            }
        }
        return loopContext;
    }

    /**
     * Set Next into else block of for..else/while..else is also allowed even if
     * current pointer is outside for..else/while..else but current pointer
     * context should be immediate parent of target for..else/while..else
     *
     * @param sourceAST
     * @param targetAST
     * @param sourceLine
     *            : the line at which debugger is stopped currently (starts at
     *            1)
     * @param targetLine
     *            : the line at which we need to set next (starts at 0)
     * @return
     */
    public static boolean isValidElseBlock(ASTEntry sourceAST, ASTEntry targetAST, int sourceLine, int targetLine) {
        boolean retval = false;
        if (targetAST.node instanceof For || targetAST.node instanceof While) {
            int targetElseBeginLine = getElseBeginLine(targetAST);
            if (targetElseBeginLine > 0 && targetLine + 1 > targetElseBeginLine) {
                if ((targetAST.parent == null || targetAST.parent.node instanceof FunctionDef) && sourceAST == null) {
                    retval = true;
                } else if (targetAST.parent != null && targetAST.parent.equals(sourceAST)) {
                    int sourceElseBeginLine = getElseBeginLine(sourceAST);
                    if (sourceLine > sourceElseBeginLine) {
                        retval = false;
                    } else {
                        retval = true;
                    }
                }
            }
        }
        return retval;
    }

    /**
     * Identifies the begin line of else block for for..else/while..else and
     * first exception begin line for try..except..else block
     *
     * @param astEntry
     * @return
     */
    public static int getElseBeginLine(ASTEntry astEntry) {
        int beginLine = 0;
        if (astEntry.node instanceof TryExcept && ((TryExcept) astEntry.node).handlers.length > 0) {
            beginLine = ((TryExcept) astEntry.node).handlers[0].beginLine;
        } else if (astEntry.node instanceof For && ((For) astEntry.node).orelse != null) {
            beginLine = ((For) astEntry.node).orelse.beginLine;
        } else if (astEntry.node instanceof While && ((While) astEntry.node).orelse != null) {
            beginLine = ((While) astEntry.node).orelse.beginLine;
        }
        return beginLine;
    }

    /**
     *
     *
     * @param sourceLine
     * @param targetLine
     * @param sourceAST
     * @param targetAST
     * @return
     */
    public static boolean isValidInterLoopContext(int sourceLine, int targetLine, ASTEntry sourceAST,
            ASTEntry targetAST) {
        boolean retval = true;
        if (sourceAST.node instanceof TryExcept && targetAST.node instanceof TryExcept
                && (!isValidTryExceptContext(sourceAST, targetAST, sourceLine, targetLine))) {
            retval = false;
        } else if (sourceAST.node instanceof For && targetAST.node instanceof For
                && (!isValidForContext(sourceAST, targetAST, sourceLine, targetLine))) {
            retval = false;
        } else if (sourceAST.node instanceof While && targetAST.node instanceof While
                && (!isValidWhileContext(sourceAST, targetAST, sourceLine, targetLine))) {
            retval = false;
        }
        return retval;
    }

    /**
     * Identifies the valid set next target inside Try..except..else block
     *
     * @param sourceAST
     * @param targetAST
     * @param sourceLine
     *            : the line at which debugger is stopped currently (starts at
     *            1)
     * @param targetLine
     *            : the line at which we need to set next (starts at 0)
     * @return
     */
    public static boolean isValidTryExceptContext(ASTEntry sourceAST, ASTEntry targetAST, int sourceLine,
            int targetLine) {

        excepthandlerType[] exceptionHandlers = ((TryExcept) sourceAST.node).handlers;
        if (((TryExcept) sourceAST.node).specialsAfter != null) {
            // Pointer can't be set on comment(s) in try block
            List<Object> specialList = ((TryExcept) sourceAST.node).specialsAfter;
            for (Object obj : specialList) {
                if (obj instanceof commentType && targetLine + 1 == ((commentType) obj).beginLine) {
                    return false;
                }
            }
        }
        for (int i = 0; i < exceptionHandlers.length; i++) {
            excepthandlerType exceptionHandler = exceptionHandlers[i];
            // Pointer can't be set on except... statement(s)
            if (targetLine + 1 == exceptionHandler.beginLine) {
                return false;
            }
        }

        // Pointer can't be moved inside try block from except or else block
        if (exceptionHandlers.length > 0) {
            int exceptionBeginLine = exceptionHandlers[0].beginLine;
            if (targetLine + 1 > ((TryExcept) sourceAST.node).beginLine && targetLine + 1 < exceptionBeginLine
                    && sourceLine >= exceptionBeginLine) {
                return false;
            }
        }
        return true;
    }

    /**
     * Identifies the valid set next target inside while..else block
     *
     * @param sourceAST
     * @param targetAST
     * @param sourceLine
     *            : the line at which debugger is stopped currently (starts at
     *            1)
     * @param targetLine
     *            : the line at which we need to set next (starts at 0)
     * @return
     */
    public static boolean isValidWhileContext(ASTEntry sourceAST, ASTEntry targetAST, int sourceLine, int targetLine) {
        // Pointer can't be moved inside while block from else block
        if (((While) sourceAST.node).orelse != null) {
            int elseBeginLine = ((While) sourceAST.node).orelse.beginLine;
            if (targetLine + 1 > ((While) sourceAST.node).beginLine && targetLine + 1 < elseBeginLine
                    && sourceLine >= elseBeginLine) {
                return false;
            }
        }
        return true;
    }

    /**
     * Identifies the valid set next target inside for..else block
     *
     * @param sourceAST
     * @param targetAST
     * @param sourceLine
     *            : the line at which debugger is stopped currently (starts at
     *            1)
     * @param targetLine
     *            : the line at which we need to set next (starts at 0)
     * @return
     */
    public static boolean isValidForContext(ASTEntry sourceAST, ASTEntry targetAST, int sourceLine, int targetLine) {
        // Pointer can't be moved inside for block from else block
        if (((For) sourceAST.node).orelse != null) {
            int elseBeginLine = ((For) sourceAST.node).orelse.beginLine;
            if (targetLine + 1 > ((For) sourceAST.node).beginLine && targetLine + 1 < elseBeginLine
                    && sourceLine >= elseBeginLine) {
                return false;
            }
        }
        return true;
    }

    protected static final String[] strTypes = new String[] { "'''", "\"\"\"", "'", "\"" };

    public static String getStringToPrint(Str node) {
        StringBuffer buffer = new StringBuffer();
        if (node.unicode) {
            buffer.append("u");
        }
        if (node.binary) {
            buffer.append("b");
        }
        if (node.raw) {
            buffer.append("r");
        }
        final String s = strTypes[node.type - 1];

        buffer.append(s);
        buffer.append(node.s);
        buffer.append(s);
        return buffer.toString();
    }

    /**
     * @param node the if node that we want to check.
     * @return null if the passed node is not
     */
    public static boolean isIfMAinNode(If node) {
        if (node.test instanceof Compare) {
            Compare compareNode = (Compare) node.test;
            // handcrafted structure walking
            if (compareNode.left instanceof Name && ((Name) compareNode.left).id.equals("__name__")
                    && compareNode.ops != null && compareNode.ops.length == 1 && compareNode.ops[0] == Compare.Eq) {

                if (compareNode.comparators != null && compareNode.comparators.length == 1
                        && compareNode.comparators[0] instanceof Str
                        && ((Str) compareNode.comparators[0]).s.equals("__main__")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return true if the given name is a valid python name.
     */
    public static boolean isValidNameRepresentation(String rep) {
        if (rep == null) {
            return false;
        }
        if ("pass".equals(rep) || rep.startsWith("!<") || rep.indexOf(' ') != -1) {
            //Name generated during the parsing (in AbstractPythonGrammar)
            return false;
        }

        return true;
    }

    /**
     * Creates an attribute from the passed string
     *
     * @param attrString: A string as 'a.b.c' or 'self.b' (at least one dot must be in the string) or self.xx()
     * Note that the call is only accepted as the last part.
     * @return an Attribute representing the string.
     */
    public static exprType makeAttribute(String attrString) {
        List<String> dotSplit = StringUtils.dotSplit(attrString);
        Assert.isTrue(dotSplit.size() > 1);

        exprType first = null;
        Attribute last = null;
        Attribute attr = null;
        for (int i = dotSplit.size() - 1; i > 0; i--) {
            Call call = null;
            String part = dotSplit.get(i);
            if (part.endsWith("()")) {
                if (i == dotSplit.size() - 1) {
                    part = part.substring(0, part.length() - 2);
                    call = new Call(null, new exprType[0], new keywordType[0], null, null);
                    first = call;
                } else {
                    throw new RuntimeException("Call only accepted in the last part.");
                }
            }
            attr = new Attribute(null, new NameTok(part, NameTok.Attrib), Attribute.Load);
            if (call != null) {
                call.func = attr;
            }
            if (last != null) {
                last.value = attr;
            }
            last = attr;
            if (first == null) {
                first = last;
            }
        }

        String lastPart = dotSplit.get(0);
        if (lastPart.endsWith("()")) {
            last.value = new Call(new Name(lastPart.substring(0, lastPart.length() - 2), Name.Load, false), null, null,
                    null, null);
        } else {
            last.value = new Name(lastPart, Name.Load, false);
        }
        return first;
    }

    /**
     * @return the body of the passed node (if it doesn't have a body, an empty array is returned).
     */
    public static stmtType[] getBody(SimpleNode node) {
        if (node instanceof Module) {
            Module module = (Module) node;
            return module.body;
        }

        if (node instanceof ClassDef) {
            ClassDef module = (ClassDef) node;
            return module.body;
        }

        if (node instanceof FunctionDef) {
            FunctionDef module = (FunctionDef) node;
            return module.body;
        }

        if (node instanceof excepthandlerType) {
            excepthandlerType module = (excepthandlerType) node;
            return module.body;
        }
        if (node instanceof For) {
            For module = (For) node;
            return module.body;
        }
        if (node instanceof If) {
            If module = (If) node;
            return module.body;
        }
        if (node instanceof Suite) {
            Suite module = (Suite) node;
            return module.body;
        }
        if (node instanceof suiteType) {
            suiteType module = (suiteType) node;
            return module.body;
        }
        if (node instanceof TryExcept) {
            TryExcept module = (TryExcept) node;
            return module.body;
        }
        if (node instanceof TryFinally) {
            TryFinally module = (TryFinally) node;
            return module.body;
        }
        if (node instanceof While) {
            While module = (While) node;
            return module.body;
        }
        if (node instanceof With) {
            With module = (With) node;
            return module.body.body;
        }
        return new stmtType[0];
    }

    /**
     * Sets the body of some node.
     */
    public static void setBody(SimpleNode node, stmtType... body) {
        if (node instanceof Module) {
            Module module = (Module) node;
            module.body = body;
        }

        if (node instanceof ClassDef) {
            ClassDef module = (ClassDef) node;
            module.body = body;
        }

        if (node instanceof FunctionDef) {
            FunctionDef module = (FunctionDef) node;
            module.body = body;
        }

        if (node instanceof excepthandlerType) {
            excepthandlerType module = (excepthandlerType) node;
            module.body = body;
        }
        if (node instanceof For) {
            For module = (For) node;
            module.body = body;
        }
        if (node instanceof If) {
            If module = (If) node;
            module.body = body;
        }
        if (node instanceof Suite) {
            Suite module = (Suite) node;
            module.body = body;
        }
        if (node instanceof suiteType) {
            suiteType module = (suiteType) node;
            module.body = body;
        }
        if (node instanceof TryExcept) {
            TryExcept module = (TryExcept) node;
            module.body = body;
        }
        if (node instanceof TryFinally) {
            TryFinally module = (TryFinally) node;
            module.body = body;
        }
        if (node instanceof While) {
            While module = (While) node;
            module.body = body;
        }
        if (node instanceof With) {
            With module = (With) node;
            module.body.body = body;
        }
    }

    /**
     * @param node This is the node where we should start looking (usually the Module)
     * @param path This is the path for which we want an item in the given node.
     *        E.g.: If we want to find a method testFoo in a class TestCase, we'de pass TestCase.testFoo as the path.
     *
     */
    public static SimpleNode getNodeFromPath(SimpleNode node, String path) {
        SimpleNode leafTestNode = null;

        SimpleNode last = node;
        for (String s : StringUtils.dotSplit(path)) {

            stmtType found = null;
            for (stmtType n : NodeUtils.getBody(last)) {
                if (s.equals(NodeUtils.getRepresentationString(n))) {
                    found = n;
                    last = n;
                    break;
                }
            }

            if (found == null) {
                leafTestNode = null;
                break;
            } else {
                leafTestNode = found;
            }
        }
        return leafTestNode;
    }

    /**
     * Finds the statement that contains the given node.
     *
     * @param source: this is the ast that contains the body with multiple statements.
     * @param ast: This is the ast for which we want the statement.
     */
    public static stmtType findStmtForNode(SimpleNode source, final SimpleNode ast) {
        VisitorBase v = new VisitorBase() {

            private stmtType lastStmtFound;

            @Override
            protected Object unhandled_node(SimpleNode node) throws Exception {
                if (node instanceof stmtType) {
                    lastStmtFound = (stmtType) node;
                }
                if (node.beginColumn == ast.beginColumn && node.beginLine == ast.beginLine
                        && node.getClass() == ast.getClass() && node.toString().equals(ast.toString())) {
                    throw new StopVisitingException(lastStmtFound);
                }
                return null;
            }

            @Override
            public void traverse(SimpleNode node) throws Exception {
                node.traverse(this);
            }
        };
        stmtType[] body = getBody(source);

        stmtType last = null;
        for (stmtType stmtType : body) {
            if (stmtType.beginLine > ast.beginLine) {
                //already passed the possible statement, check the last one (which is the last statement that
                //has a beginLine <= ast.beginLine) and return even if we didn't find it, as we already passed the
                //target line.
                if (last != null) {
                    return checkNode(v, last);
                }
            }
            if (stmtType.beginLine == ast.beginLine) {
                //If we have a case in the same line, we must also check it. Don't mark it as last in this case as we've
                //already checked it.
                stmtType n = checkNode(v, stmtType);
                if (n != null) {
                    return n;
                }
            } else {
                last = stmtType;
            }
        }
        return null;
    }

    private static stmtType checkNode(VisitorBase v, stmtType last) {
        try {
            last.accept(v);
        } catch (StopVisitingException e) {
            if (e.lastStmtFound != null) {
                //it could be that we found a statement inside this statement.
                return e.lastStmtFound;
            } else {
                //Ok, there were no more statements there, so, just go on with the last we received.
                return last;
            }
        } catch (Exception e) {
            Log.log(e);
        }
        //Not found!
        return null;
    }

    public static int getOffset(IDocument doc, SimpleNode node) {
        int nodeOffsetBegin = PySelection.getAbsoluteCursorOffset(doc, node.beginLine - 1, node.beginColumn - 1);
        return nodeOffsetBegin;
    }

    public static String getTypeForParameterFromDocstring(String actTok, SimpleNode node) {
        String nodeDocString = NodeUtils.getNodeDocString(node);
        if (nodeDocString != null) {
            return getTypeForParameterFromDocstring(actTok, nodeDocString);
        }
        return null;
    }

    public static String getTypeForParameterFromDocstring(String actTok, String nodeDocString) {
        String possible = null;
        Iterable<String> iterLines = StringUtils.iterLines(nodeDocString);
        for (String string : iterLines) {
            String trimmed = string.trim();
            if (trimmed.startsWith(":type") || trimmed.startsWith("@type")) {
                trimmed = trimmed.substring(5).trim();
                if (trimmed.startsWith(actTok)) {
                    trimmed = trimmed.substring(actTok.length()).trim();
                    if (trimmed.startsWith(":")) {
                        trimmed = trimmed.substring(1).trim();
                        return fixType(trimmed);
                    }
                }
            } else if (trimmed.startsWith(":param")) {
                //Handle case >>:param type name:
                int i = trimmed.indexOf(':', 2);

                if (i != -1) {
                    trimmed = trimmed.substring(6, i).trim();

                    List<String> split = StringUtils.split(trimmed, ' ');
                    if (split.size() == 2 && split.get(1).equals(actTok)) {
                        //As this is not the default, just mark it as a possibility.
                        possible = split.get(0).trim();
                    }
                }

            } else if (trimmed.startsWith("@param")) {
                //Handle case >>@param name: type
                trimmed = trimmed.substring(6).trim();
                if (trimmed.startsWith(actTok)) {
                    trimmed = trimmed.substring(actTok.length());
                    if (trimmed.startsWith(":")) {
                        trimmed = trimmed.substring(1).trim();
                        if (trimmed.indexOf(' ') == -1 && trimmed.indexOf('\t') == -1) {
                            //As this is not the default, just mark it as a possibility.
                            possible = trimmed;
                        }

                    }
                }
            }
        }
        return fixType(possible);
    }

    private static String fixType(String trimmed) {
        if (trimmed != null) {
            trimmed = trimmed.trim();
            if (trimmed.startsWith(":")) {
                trimmed = trimmed.substring(1);
            }
            int i = trimmed.indexOf(':');
            if (i != -1) {
                trimmed = trimmed.substring(i + 1);
            }
            FastStringBuffer ret = new FastStringBuffer(trimmed, 0);
            HashSet<Character> set = new HashSet<Character>();
            set.add('!');
            set.add('~');
            trimmed = ret.removeChars(set).toString().trim();
            if (trimmed.startsWith("`")) {
                trimmed = trimmed.substring(1);
                if (trimmed.endsWith("`")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1);
                }
                i = trimmed.indexOf(' ');
                if (i != -1) {
                    trimmed = trimmed.substring(i + 1);
                }
            }
        }
        return trimmed;
    }

    public static String getReturnTypeFromDocstring(SimpleNode node) {
        String nodeDocString = NodeUtils.getNodeDocString(node);
        if (nodeDocString == null) {
            return null;
        }
        return getReturnTypeFromDocstring(nodeDocString);
    }

    public static String getReturnTypeFromDocstring(String docstring) {
        String possible = null;
        Iterable<String> iterLines = StringUtils.iterLines(docstring);
        String line0 = null;
        for (String string : iterLines) {
            String trimmed = string.trim();
            if (line0 == null) {
                line0 = trimmed;
            }
            if (trimmed.startsWith(":rtype") || trimmed.startsWith("@rtype")) {
                trimmed = trimmed.substring(6).trim();
                if (trimmed.startsWith(":")) {
                    trimmed = trimmed.substring(1).trim();
                }
                return fixType(trimmed);

            } else if (trimmed.startsWith("@return") || trimmed.startsWith(":return")) {
                //Additional pattern:
                //if we have:
                //@return type:
                //    return comment on new line
                //consider the type there.
                trimmed = trimmed.substring(7).trim();
                if (trimmed.endsWith(":")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1);
                    //must be a single word
                    if (trimmed.indexOf(' ') == -1 && trimmed.indexOf('\t') == -1) {
                        //As this is not the default, just mark it as a possibility.
                        //The default is the @rtype!
                        possible = trimmed;
                    }
                }
            }
        }
        if (possible == null) {
            if (line0 != null) {
                // Many builtins have docstrings such as "S.splitlines(keepends=False) -> list of strings"
                int i = line0.indexOf("->");
                if (i > 0) {
                    possible = line0.substring(i + 2).trim();
                    possible = possible.replace("of strings", "(str)");
                    possible = possible.replace("of string", "(str)");
                    int j = possible.indexOf(" of ");
                    if (j != -1) {
                        possible = possible.replace(" of ", "(") + ")";
                    }
                }
            }
        }
        return fixType(possible);
    }

    public static String getUnpackedTypeFromTypeDocstring(String compoundType, UnpackInfo checkPosForDict) {
        ParsingUtils parsingUtils = ParsingUtils.create(compoundType);
        int len = parsingUtils.len();
        if (checkPosForDict.getUnpackFor()) {
            for (int i = 0; i < len; i++) {
                char c = parsingUtils.charAt(i);
                if (c == '(' || c == '[') {
                    try {
                        int j = parsingUtils.eatPar(i, null, c);
                        if (j != -1) {
                            compoundType = compoundType.substring(i + 1, j);
                        }
                    } catch (SyntaxErrorException e) {
                    }
                    break;
                }
            }
        }
        try {
            //NOTE: the getUnpackTuple(10) isn't really good, but we have to change the strategy
            //to first parse to get what's available to then know the length (so, right now
            //we won't work very well with negative numbers in this use-case).
            return getValueForContainer(compoundType, 0, checkPosForDict.getUnpackTuple(10), -1);
        } catch (SyntaxErrorException e) {
            return "";
        }

    }

    private static String getValueForContainer(String substring, int currentPos, int unpackTuple,
            int foundFirstSeparator)
                    throws SyntaxErrorException {
        if (unpackTuple == -1) {
            return substring;
        }

        ParsingUtils parsingUtils = ParsingUtils.create(substring);
        int len = parsingUtils.len();
        int lastStart = 0;
        for (int i = 0; i < len; i++) {
            char c = parsingUtils.charAt(i);
            if (c == '(' || c == '[') {
                int j = parsingUtils.eatPar(i, null, c);
                if (j != -1) {
                    String searchIn = substring.substring(i + 1, j);
                    if (foundFirstSeparator == -1) {
                        return getValueForContainer(searchIn, currentPos, unpackTuple, 0);
                    } else {
                        i = j;
                        continue;
                    }
                }
            }
            boolean found = c == ':' || c == ',';

            if (!found && c == '-') {
                if (i + 1 < len) {
                    if (parsingUtils.charAt(i + 1) == '>') {
                        found = true;
                    }
                }
            }

            if (found) {
                if (currentPos == unpackTuple) {
                    return substring.substring(lastStart, i).trim();
                }
                if (c == '-') {
                    i++;
                }
                lastStart = i + 1;
                foundFirstSeparator = i;
                currentPos++;
            }
        }
        if (currentPos == unpackTuple) {
            return substring.substring(lastStart, substring.length()).trim();
        }
        return substring;
    }

    public static String getPackedTypeFromDocstring(String docstring) {
        docstring = docstring.trim();
        int i = docstring.indexOf('(');
        int j = docstring.indexOf('[');
        int k = docstring.indexOf(' ');
        if (i == -1 && j == -1 && k == -1) {
            return docstring;
        }
        if (i != -1) {
            if (i == 0) {
                return "tuple";
            }
            return docstring.substring(0, i).trim();
        }
        if (j != -1) {
            if (j == 0) {
                return "list";
            }
            return docstring.substring(0, j).trim();
        }
        if (k != -1) {
            return docstring.substring(0, k).trim();
        }
        throw new RuntimeException("Did not expect to get here");
    }

    public static exprType[] getEltsFromCompoundObject(SimpleNode ast) {
        // Most common at the top!
        if (ast instanceof org.python.pydev.parser.jython.ast.Tuple) {
            org.python.pydev.parser.jython.ast.Tuple tuple = (org.python.pydev.parser.jython.ast.Tuple) ast;
            return tuple.elts;
        }
        if (ast instanceof org.python.pydev.parser.jython.ast.List) {
            org.python.pydev.parser.jython.ast.List list = (org.python.pydev.parser.jython.ast.List) ast;
            return list.elts;
        }

        if (ast instanceof org.python.pydev.parser.jython.ast.ListComp) {
            org.python.pydev.parser.jython.ast.ListComp list = (org.python.pydev.parser.jython.ast.ListComp) ast;
            exprType[] ret = new exprType[] { list.elt };

            if (list.generators != null && list.generators.length == 1) {
                comprehensionType comprehensionType = list.generators[0];
                if (comprehensionType instanceof Comprehension) {
                    Comprehension comprehension = (Comprehension) comprehensionType;
                    exprType iter = comprehension.iter;
                    exprType[] eltsFromIter = getEltsFromCompoundObject(iter);

                    if (comprehension.target instanceof Name && eltsFromIter != null && eltsFromIter.length > 0) {
                        Name name = (Name) comprehension.target;
                        String rep = getRepresentationString(name);
                        if (rep != null) {
                            if (ret.length == 1) {
                                if (ret[0] instanceof Name) {
                                    String nameRep = getRepresentationString(ret[0]);
                                    if (rep.equals(nameRep)) {
                                        ret[0] = eltsFromIter[0]; //Note: mutating ret is Ok (it's a local copy).
                                    }

                                } else if (ret[0] instanceof org.python.pydev.parser.jython.ast.Tuple
                                        || ret[0] instanceof org.python.pydev.parser.jython.ast.List) {
                                    ret[0] = (exprType) ret[0].createCopy(); //Careful: we shouldn't mutate the original AST.
                                    exprType[] tupleElts = getEltsFromCompoundObject(ret[0]);
                                    for (int i = 0; i < tupleElts.length; i++) {
                                        exprType tupleArg = tupleElts[i];
                                        if (tupleArg instanceof Name) {
                                            if (rep.equals(getRepresentationString(tupleArg))) {
                                                tupleElts[i] = eltsFromIter[0];
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return ret;
        }
        if (ast instanceof org.python.pydev.parser.jython.ast.Set) {
            org.python.pydev.parser.jython.ast.Set set = (org.python.pydev.parser.jython.ast.Set) ast;
            return set.elts;
        }
        if (ast instanceof org.python.pydev.parser.jython.ast.Dict) {
            org.python.pydev.parser.jython.ast.Dict dict = (org.python.pydev.parser.jython.ast.Dict) ast;
            return new exprType[] { dict.keys[0], dict.values[0] };
        }
        if (ast instanceof org.python.pydev.parser.jython.ast.DictComp) {
            org.python.pydev.parser.jython.ast.DictComp dict = (org.python.pydev.parser.jython.ast.DictComp) ast;
            return new exprType[] { dict.key, dict.value };
        }
        if (ast instanceof org.python.pydev.parser.jython.ast.SetComp) {
            org.python.pydev.parser.jython.ast.SetComp set = (org.python.pydev.parser.jython.ast.SetComp) ast;
            return new exprType[] { set.elt };
        }
        if (ast instanceof Call) {
            Call call = (Call) ast;
            exprType func = call.func;
            if (func instanceof Name) {
                Name name = (Name) func;
                if ("dict".equals(name.id) || "list".equals(name.id) || "tuple".equals(name.id)
                        || "set".equals(name.id)) {
                    //A dict call
                    exprType[] args = call.args;
                    if (args != null && args.length > 0) {
                        return getEltsFromCompoundObject(args[0]);
                    }
                }
            }
            if (func instanceof Attribute) {
                Attribute attribute = (Attribute) func;
                if (attribute.value instanceof Dict) {
                    Dict dict = (Dict) attribute.value;
                    String representationString = getRepresentationString(attribute.attr);
                    if ("keys".equals(representationString) || "iterkeys".equals(representationString)) {
                        return dict.keys;
                    }
                    if ("values".equals(representationString) || "itervalues".equals(representationString)) {
                        return dict.values;
                    }
                    if ("items".equals(representationString) || "iteritems".equals(representationString)) {
                        if (dict.keys != null && dict.values != null && dict.keys.length > 0
                                && dict.values.length > 0) {
                            return new exprType[] { dict.keys[0], dict.values[0] };
                        }
                    }
                }

                if (attribute.value instanceof DictComp) {
                    DictComp dict = (DictComp) attribute.value;
                    String representationString = getRepresentationString(attribute.attr);
                    if ("keys".equals(representationString) || "iterkeys".equals(representationString)) {
                        return new exprType[] { dict.key };
                    }
                    if ("values".equals(representationString) || "itervalues".equals(representationString)) {
                        return new exprType[] { dict.value };
                    }
                    if ("items".equals(representationString) || "iteritems".equals(representationString)) {
                        if (dict.key != null && dict.value != null) {
                            return new exprType[] { dict.key, dict.value };
                        }
                    }
                }
            }
        }
        return null;
    }

}
