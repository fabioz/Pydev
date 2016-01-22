/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 20, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IToken;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.structure.FastStack;

/**
 * @author Fabio Zadrozny
 */
public class LocalScope implements ILocalScope {

    //the first node from the stack is always the module itself (if it's not there, it means it is a compiled module scope)
    public FastStack<SimpleNode> scope = new FastStack<SimpleNode>(20);

    public int scopeEndLine = -1;

    public int ifMainLine = -1;

    public SimpleNode foundAtASTNode;

    @Override
    public void setFoundAtASTNode(ISimpleNode node) {
        this.foundAtASTNode = (SimpleNode) node;
    }

    @Override
    public SimpleNode getFoundAtASTNode() {
        return foundAtASTNode;
    }

    /**
     * Used to create without an initial scope. It may be changed later by using the getScopeStack() and
     * adding tokens.
     */
    public LocalScope() {

    }

    public LocalScope(FastStack<SimpleNode> scope) {
        this.scope.addAll(scope);
    }

    public FastStack<SimpleNode> getScopeStack() {
        return scope;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LocalScope)) {
            return false;
        }

        LocalScope s = (LocalScope) obj;

        if (this.scope.size() != s.scope.size()) {
            return false;
        }

        return checkIfScopesMatch(s);
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42; // any arbitrary constant will do
    }

    /**
     * @see org.python.pydev.core.ILocalScope#isOuterOrSameScope(org.python.pydev.editor.codecompletion.revisited.visitors.LocalScope)
     */
    public boolean isOuterOrSameScope(ILocalScope s) {
        if (this.scope.size() > s.getScopeStack().size()) {
            return false;
        }

        return checkIfScopesMatch(s);
    }

    /**
     * @param s the scope we're checking for
     * @return if the scope passed as a parameter starts with the same scope we have here. It should not be
     * called if the size of the scope we're checking is bigger than the size of 'this' scope.
     */
    @SuppressWarnings("unchecked")
    private boolean checkIfScopesMatch(ILocalScope s) {
        Iterator<SimpleNode> otIt = s.getScopeStack().iterator();

        for (Iterator<SimpleNode> iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = iter.next();
            SimpleNode otElement = otIt.next();

            if (element.beginColumn != otElement.beginColumn) {
                return false;
            }

            if (element.beginLine != otElement.beginLine) {
                return false;
            }

            if (!element.getClass().equals(otElement.getClass())) {
                return false;
            }

            String rep1 = NodeUtils.getFullRepresentationString(element);
            String rep2 = NodeUtils.getFullRepresentationString(otElement);
            if (rep1 == null || rep2 == null) {
                if (rep1 != rep2) {
                    return false;
                }

            } else if (!rep1.equals(rep2)) {
                return false;
            }

        }
        return true;
    }

    /**
     * @see org.python.pydev.core.ILocalScope#getAllLocalTokens()
     */
    public IToken[] getAllLocalTokens() {
        return getLocalTokens(Integer.MAX_VALUE, Integer.MAX_VALUE, false);
    }

    /**
     * @see org.python.pydev.core.ILocalScope#getLocalTokens(int, int, boolean)
     */
    public IToken[] getLocalTokens(int endLine, int col, boolean onlyArgs) {
        Set<SourceToken> comps = new HashSet<SourceToken>();

        for (Iterator<SimpleNode> iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = iter.next();

            stmtType[] body = null;
            if (element instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) element;
                final argumentsType args = f.args;

                for (int i = 0; i < args.args.length; i++) {
                    String s = NodeUtils.getRepresentationString(args.args[i]);
                    comps.add(new SourceToken(args.args[i], s, "", "", "", IToken.TYPE_PARAM));
                }
                if (args.vararg != null) {
                    String s = NodeUtils.getRepresentationString(args.vararg);
                    comps.add(new SourceToken(args.vararg, s, "", "", "", IToken.TYPE_PARAM));
                }

                if (args.kwarg != null) {
                    String s = NodeUtils.getRepresentationString(args.kwarg);
                    comps.add(new SourceToken(args.kwarg, s, "", "", "", IToken.TYPE_PARAM));
                }
                if (args.kwonlyargs != null) {
                    for (int i = 0; i < args.kwonlyargs.length; i++) {
                        String s = NodeUtils.getRepresentationString(args.kwonlyargs[i]);
                        comps.add(new SourceToken(args.kwonlyargs[i], s, "", "", "", IToken.TYPE_PARAM));
                    }
                }

                if (onlyArgs) {
                    continue;
                }
                body = f.body;
            }

            else if (element instanceof ClassDef && !iter.hasNext()) {
                ClassDef classDef = (ClassDef) element;
                body = classDef.body;
            }

            if (body != null) {
                try {
                    for (int i = 0; i < body.length; i++) {
                        GlobalModelVisitor visitor = new GlobalModelVisitor(GlobalModelVisitor.GLOBAL_TOKENS, "",
                                false, true);
                        stmtType stmt = body[i];
                        if (stmt == null) {
                            continue;
                        }
                        stmt.accept(visitor);
                        List<IToken> t = visitor.tokens;
                        for (Iterator<IToken> iterator = t.iterator(); iterator.hasNext();) {
                            SourceToken tok = (SourceToken) iterator.next();

                            //if it is found here, it is a local type
                            tok.type = IToken.TYPE_LOCAL;
                            if (tok.getAst().beginLine <= endLine) {
                                comps.add(tok);
                            }

                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }

        return comps.toArray(new SourceToken[0]);
    }

    /**
     *
     * @param argName this is the argument (cannot have dots)
     * @param activationToken this is the actual activation token we're looking for
     * (may have dots).
     *
     * Note that argName == activationToken first part before the dot (they may be equal)
     * @return a list of tokens for the local
     */
    public Collection<IToken> getInterfaceForLocal(String activationToken) {
        return getInterfaceForLocal(activationToken, true, true);
    }

    public Collection<IToken> getInterfaceForLocal(String activationToken, boolean addAttributeAccess,
            boolean addLocalsFromHasAttr) {
        Set<SourceToken> comps = new HashSet<SourceToken>();

        Iterator<SimpleNode> it = this.scope.topDownIterator();
        if (!it.hasNext()) {
            return new ArrayList<IToken>();
        }

        SimpleNode element = it.next();

        String dottedActTok = activationToken + '.';
        //ok, that's the scope we have to analyze
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(element);

        ArrayList<Class> classes = new ArrayList<Class>(2);
        if (addAttributeAccess) {
            classes.add(Attribute.class);

        }
        if (addLocalsFromHasAttr) {
            classes.add(Call.class);
        }
        Iterator<ASTEntry> iterator = visitor.getIterator(classes.toArray(new Class[classes.size()]));

        while (iterator.hasNext()) {
            ASTEntry entry = iterator.next();
            if (entry.node instanceof Attribute) {
                String rep = NodeUtils.getFullRepresentationString(entry.node);
                if (rep.startsWith(dottedActTok)) {
                    rep = rep.substring(dottedActTok.length());
                    if (NodeUtils.isValidNameRepresentation(rep)) { //that'd be something that can happen when trying to recreate the parsing
                        comps.add(new SourceToken(entry.node, FullRepIterable.getFirstPart(rep), "", "", "",
                                IToken.TYPE_OBJECT_FOUND_INTERFACE));
                    }
                }
            } else if (entry.node instanceof Call) {
                Call call = (Call) entry.node;
                if ("hasattr".equals(NodeUtils.getFullRepresentationString(call.func)) && call.args != null
                        && call.args.length == 2) {
                    String rep = NodeUtils.getFullRepresentationString(call.args[0]);
                    if (rep.equals(activationToken)) {
                        exprType node = call.args[1];
                        if (node instanceof Str) {
                            Str str = (Str) node;
                            String attrName = str.s;
                            if (NodeUtils.isValidNameRepresentation(attrName)) {
                                comps.add(new SourceToken(node, attrName, "", "", "",
                                        IToken.TYPE_OBJECT_FOUND_INTERFACE));
                            }
                        }
                    }
                }

            }
        }
        return new ArrayList<IToken>(comps);
    }

    /**
     * @see org.python.pydev.core.ILocalScope#getLocalImportedModules(int, int, java.lang.String)
     */
    public List<IToken> getLocalImportedModules(int line, int col, String moduleName) {
        ArrayList<IToken> importedModules = new ArrayList<IToken>();
        for (Iterator<SimpleNode> iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = iter.next();

            if (element instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) element;
                for (int i = 0; i < f.body.length; i++) {
                    stmtType stmt = f.body[i];
                    if (stmt != null) {
                        importedModules.addAll(GlobalModelVisitor.getTokens(stmt, GlobalModelVisitor.ALIAS_MODULES,
                                moduleName, null, false));
                    }
                }
            }
        }
        return importedModules;
    }

    /**
     * @see org.python.pydev.core.ILocalScope#getClassDef()
     */
    public ClassDef getClassDef() {
        for (Iterator<SimpleNode> it = this.scope.topDownIterator(); it.hasNext();) {
            SimpleNode node = it.next();
            if (node instanceof ClassDef) {
                return (ClassDef) node;
            }
        }
        return null;
    }

    /**
     * @see org.python.pydev.core.ILocalScope#isLastClassDef()
     */
    public boolean isLastClassDef() {
        if (this.scope.size() > 0 && this.scope.peek() instanceof ClassDef) {
            return true;
        }
        return false;
    }

    public Iterator iterator() {
        return scope.topDownIterator();
    }

    public int getIfMainLine() {
        return ifMainLine;
    }

    public int getScopeEndLine() {
        return scopeEndLine;
    }

    public void setIfMainLine(int original) {
        this.ifMainLine = original;
    }

    public void setScopeEndLine(int beginLine) {
        this.scopeEndLine = beginLine;
    }

    /**
     * Constant containing the calls that are checked for implementations.
     *
     * Couldn't find anything similar for pyprotocols.
     *
     * Zope has a different heuristic which is also checked:
     * assert Interface.implementedBy(foo)
     *
     * maps the method name to check -> index of the class in the call (or negative if class is the caller)
     *
     * TODO: This should be made public to the user...
     */
    public static final Map<String, Integer> ISINSTANCE_POSSIBILITIES = new HashMap<String, Integer>();

    static {
        ISINSTANCE_POSSIBILITIES.put("isinstance".toLowerCase(), 2);
        ISINSTANCE_POSSIBILITIES.put("IsImplementation".toLowerCase(), 2);
        ISINSTANCE_POSSIBILITIES.put("IsInterfaceDeclared".toLowerCase(), 2);
        ISINSTANCE_POSSIBILITIES.put("implementedBy".toLowerCase(), -1);
    }

    /**
     * @see {@link ILocalScope#getPossibleClassesForActivationToken(String)}
     */
    public List<String> getPossibleClassesForActivationToken(String actTok) {
        ArrayList<String> ret = new ArrayList<String>();

        Iterator<SimpleNode> it = this.scope.topDownIterator();
        if (!it.hasNext()) {
            return ret;
        }
        SimpleNode element = it.next();

        //ok, that's the scope we have to analyze

        //Search for docstrings.
        String typeForParameter = NodeUtils.getTypeForParameterFromDocstring(actTok, element);
        if (typeForParameter != null) {
            ret.add(typeForParameter);
        }

        //Search for assert isinstance().
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(element);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        ArrayList<Object> lst = new ArrayList<Object>();

        Object nameDefinition = null;

        while (iterator.hasNext()) {
            ASTEntry entry = iterator.next();
            if (entry.node.specialsAfter != null) {
                lst.addAll(entry.node.specialsAfter);
            }
            if (entry.node.specialsBefore != null) {
                lst.addAll(entry.node.specialsBefore);
            }

            if (!(entry.node instanceof Assert)) {
                if (entry.node instanceof Str) {
                    lst.add(entry.node);
                }
                if (entry.node instanceof Name) {
                    Name name = (Name) entry.node;
                    if (name.ctx == Name.Load) {
                        if (actTok.equals(name.id)) {
                            nameDefinition = name;
                        }
                    }
                }
                if (entry.node instanceof Expr) {
                    Expr expr = (Expr) entry.node;
                    if (expr.value instanceof Attribute) {
                        Attribute attribute = (Attribute) expr.value;
                        if (actTok.equals(NodeUtils.getFullRepresentationString(attribute))) {
                            nameDefinition = attribute;
                        }
                    }
                }
                continue;
            }
            Assert ass = (Assert) entry.node;
            if (ass.test instanceof Call) {
                Call call = (Call) ass.test;
                String rep = NodeUtils.getFullRepresentationString(call.func);
                if (rep == null) {
                    continue;
                }
                Integer classIndex = ISINSTANCE_POSSIBILITIES.get(FullRepIterable.getLastPart(rep).toLowerCase());
                if (classIndex != null) {
                    if (call.args != null && (call.args.length >= Math.max(classIndex, 1))) {
                        //in all cases, the instance is the 1st parameter.
                        String foundActTok = NodeUtils.getFullRepresentationString(call.args[0]);

                        if (foundActTok != null && foundActTok.equals(actTok)) {
                            if (classIndex > 0) {
                                exprType type = call.args[classIndex - 1];

                                if (type instanceof Tuple) {
                                    //case: isinstance(obj, (Class1,Class2))
                                    Tuple tuple = (Tuple) type;
                                    for (exprType expr : tuple.elts) {
                                        addRepresentationIfPossible(ret, expr);
                                    }
                                } else {
                                    //case: isinstance(obj, Class)
                                    addRepresentationIfPossible(ret, type);
                                }
                            } else {
                                //zope case Interface.implementedBy(obj) -> Interface added
                                ret.add(FullRepIterable.getWithoutLastPart(rep));
                            }
                        }
                    }
                }
            }
        }

        if (nameDefinition != null) {
            int s = lst.size();
            for (int i = 0; i < s; i++) {
                Object object = lst.get(i);
                if (object instanceof commentType) {
                    commentType commentType = (commentType) object;
                    //according to http://sphinx-doc.org/ext/autodoc.html#directive-autoattribute,
                    //to be a valid comment must be before the definition or in the same line.
                    //                    if (Math.abs(commentType.beginLine - nameDefinition.beginLine) <= 2) { --Not checking it (being a bit more lenient -- and if it's defined once in the context we'll be sure it'll be found.
                    if (commentType.id != null) {
                        String trim = commentType.id.trim();
                        if (trim.startsWith("#")) {
                            trim = trim.substring(1).trim();
                        }
                        if (trim.startsWith(":")) {
                            String type = NodeUtils.getTypeForParameterFromDocstring(actTok, trim.substring(1));
                            if (type != null) {
                                ret.add(type);
                            }
                        } else if (trim.startsWith("@")) {
                            String type = NodeUtils.getTypeForParameterFromDocstring(actTok, trim);
                            if (type != null) {
                                ret.add(type);
                            }
                        }
                        //                        }
                    }

                } else if (object instanceof Str) {
                    Str str = (Str) object;
                    //                    if (Math.abs(str.beginLine - nameDefinition.beginLine) <= 2) {
                    if (str.s != null) {
                        String trim = str.s.trim();
                        if (trim.startsWith("#")) {
                            trim = trim.substring(1).trim();
                        }
                        if (trim.startsWith(":")) {
                            String type = NodeUtils.getTypeForParameterFromDocstring(actTok, trim.substring(1));
                            if (type != null) {
                                ret.add(type);
                            }
                        } else if (trim.startsWith("@")) {
                            String type = NodeUtils.getTypeForParameterFromDocstring(actTok, trim);
                            if (type != null) {
                                ret.add(type);
                            }
                        }
                    }
                    //                    }
                }
            }
        }
        return ret;
    }

    /**
     * @param ret the list where the representation should be added
     * @param expr the Name or Attribute that determines the class that should be added
     */
    private void addRepresentationIfPossible(ArrayList<String> ret, exprType expr) {
        if (expr instanceof Name || expr instanceof Attribute) {
            String string = NodeUtils.getFullRepresentationString(expr);
            if (string != null) {
                ret.add(string);
            }
        }
    }

}
