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
package org.python.pydev.ast.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ITypeInfo;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.TypeInfo;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_core.structure.FastStack;

/**
 * @author Fabio Zadrozny
 */
public class LocalScope implements ILocalScope {

    //the first node from the stack is always the module itself (if it's not there, it means it is a compiled module scope)
    public FastStack<ISimpleNode> scope = new FastStack<ISimpleNode>(20);

    public int scopeEndLine = -1;

    public int ifMainLine = -1;

    public SimpleNode foundAtASTNode;

    private final IPythonNature nature;

    public final IModule module;

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
    public LocalScope(IPythonNature nature, IModule module) {
        this.nature = nature;
        this.module = module;
    }

    public LocalScope(IPythonNature nature, FastStack<SimpleNode> scope, IModule module) {
        this.nature = nature;
        this.scope.addAll(scope);
        this.module = module;
    }

    @Override
    public FastStack<ISimpleNode> getScopeStack() {
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
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer("LocalScope(", 40);
        boolean added = false;
        for (ISimpleNode n : scope) {
            if (!added) {
                added = true;
            } else {
                buf.append(", ");
            }
            buf.append(FullRepIterable.getLastPart(n.getClass().getName()));
            String rep = NodeUtils.getRepresentationString(n);
            if (rep != null) {
                buf.append('(');
                buf.append(rep);
                buf.append(')');
            }
        }
        buf.append(')');
        return buf.toString();
    }

    @Override
    public int hashCode() {
        int hash = 43;
        for (Iterator<ISimpleNode> iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = (SimpleNode) iter.next();

            hash += Integer.hashCode(element.beginColumn);
            hash += Integer.hashCode(element.beginLine);
            hash += element.getClass().hashCode();

            String rep1 = NodeUtils.getFullRepresentationString(element);
            if (rep1 != null) {
                hash += rep1.hashCode();
            }
        }
        return hash;
    }

    /**
     * @see org.python.pydev.core.ILocalScope#isOuterOrSameScope(org.python.pydev.ast.codecompletion.revisited.visitors.LocalScope)
     */
    @Override
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
    private boolean checkIfScopesMatch(ILocalScope s) {
        Iterator<ISimpleNode> otIt = s.getScopeStack().iterator();

        for (Iterator<ISimpleNode> iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = (SimpleNode) iter.next();
            SimpleNode otElement = (SimpleNode) otIt.next();

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
    @Override
    public TokensList getAllLocalTokens() {
        return getLocalTokens(Integer.MAX_VALUE, Integer.MAX_VALUE, false);
    }

    /**
     * @see org.python.pydev.core.ILocalScope#getLocalTokens(int, int, boolean)
     */
    @Override
    public TokensList getLocalTokens(int line, int col, boolean onlyArgs) {
        Set<SourceToken> comps = new HashSet<SourceToken>();

        for (Iterator<ISimpleNode> iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = (SimpleNode) iter.next();

            stmtType[] body = null;
            if (element instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) element;
                final argumentsType args = f.args;

                for (int i = 0; i < args.args.length; i++) {
                    String s = NodeUtils.getRepresentationString(args.args[i]);
                    SourceToken sourceToken = new SourceToken(args.args[i], s, "", "", "", IToken.TYPE_PARAM, nature,
                            module);
                    if (args.annotation != null && args.annotation.length > i && args.annotation[i] != null) {
                        exprType[] targets = { args.args[i] };
                        exprType value = null;
                        if (args.defaults != null && args.defaults.length > i && args.defaults[i] != null) {
                            value = args.defaults[i];
                        }
                        exprType type = args.annotation[i];
                        sourceToken.setDummyAssignFromParam(new Assign(targets, value, type));
                    } else if (args.defaults != null && args.defaults.length > i && args.defaults[i] != null) {
                        exprType[] targets = { args.args[i] };
                        exprType value = args.defaults[i];
                        sourceToken.setDummyAssignFromParam(new Assign(targets, value, null));
                    }
                    comps.add(sourceToken);
                }
                if (args.vararg != null) {
                    String s = NodeUtils.getRepresentationString(args.vararg);
                    comps.add(new SourceToken(args.vararg, s, "", "", "", IToken.TYPE_PARAM, nature, module));
                }

                if (args.kwarg != null) {
                    String s = NodeUtils.getRepresentationString(args.kwarg);
                    comps.add(new SourceToken(args.kwarg, s, "", "", "", IToken.TYPE_PARAM, nature, module));
                }
                if (args.kwonlyargs != null) {
                    for (int i = 0; i < args.kwonlyargs.length; i++) {
                        String s = NodeUtils.getRepresentationString(args.kwonlyargs[i]);
                        comps.add(
                                new SourceToken(args.kwonlyargs[i], s, "", "", "", IToken.TYPE_PARAM, nature, module));
                    }
                }

                body = f.body;
            }

            else if (element instanceof ClassDef && !iter.hasNext()) {
                ClassDef classDef = (ClassDef) element;
                body = classDef.body;
            }

            if (onlyArgs) {
                continue;
            }

            if (body != null) {
                try {
                    for (int i = 0; i < body.length; i++) {
                        GlobalModelVisitor visitor = new GlobalModelVisitor(GlobalModelVisitor.GLOBAL_TOKENS, "",
                                false, true, this.nature, this.module);
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
                            if (tok.getAst().beginLine <= line) {
                                comps.add(tok);
                            }

                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }

        return new TokensList(comps.toArray(new SourceToken[0]));
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
    @Override
    public TokensList getInterfaceForLocal(String activationToken) {
        return getInterfaceForLocal(activationToken, true, true);
    }

    public TokensList getInterfaceForLocal(String activationToken, boolean addAttributeAccess,
            boolean addLocalsFromHasAttr) {
        Set<SourceToken> comps = new HashSet<SourceToken>();

        Iterator<ISimpleNode> it = this.scope.topDownIterator();
        if (!it.hasNext()) {
            return new TokensList();
        }

        SimpleNode element = (SimpleNode) it.next();

        String dottedActTok = activationToken + '.';
        //ok, that's the scope we have to analyze
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(element, false);

        ArrayList<Class<? extends SimpleNode>> classes = new ArrayList<>(2);
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
                                IToken.TYPE_OBJECT_FOUND_INTERFACE, this.nature, module));
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
                                        IToken.TYPE_OBJECT_FOUND_INTERFACE, this.nature, module));
                            }
                        }
                    }
                }

            }
        }
        return new TokensList(comps.toArray(new IToken[0]));
    }

    /**
     * @see org.python.pydev.core.ILocalScope#getLocalImportedModules(int, int, java.lang.String)
     */
    @Override
    public TokensList getLocalImportedModules(int line, int col, String moduleName) {
        ArrayList<IToken> importedModules = new ArrayList<IToken>();
        for (Iterator<ISimpleNode> iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = (SimpleNode) iter.next();

            if (element instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) element;
                if (f.body != null) {
                    for (int i = 0; i < f.body.length; i++) {
                        stmtType stmt = f.body[i];
                        if (stmt != null) {
                            importedModules.addAll(GlobalModelVisitor.getTokens(stmt, GlobalModelVisitor.ALIAS_MODULES,
                                    moduleName, null, false, this.nature, this.module));
                        }
                    }
                }
            }
        }
        return new TokensList(importedModules);
    }

    /**
     * @see org.python.pydev.core.ILocalScope#getClassDef()
     */
    @Override
    public ClassDef getClassDef() {
        for (Iterator<ISimpleNode> it = this.scope.topDownIterator(); it.hasNext();) {
            SimpleNode node = (SimpleNode) it.next();
            if (node instanceof ClassDef) {
                return (ClassDef) node;
            }
        }
        return null;
    }

    /**
     * @see org.python.pydev.core.ILocalScope#isLastClassDef()
     */
    @Override
    public boolean isLastClassDef() {
        if (this.scope.size() > 0 && this.scope.peek() instanceof ClassDef) {
            return true;
        }
        return false;
    }

    @Override
    public Iterator<ISimpleNode> iterator() {
        return scope.topDownIterator();
    }

    @Override
    public int getIfMainLine() {
        return ifMainLine;
    }

    @Override
    public int getScopeEndLine() {
        return scopeEndLine;
    }

    @Override
    public void setIfMainLine(int original) {
        this.ifMainLine = original;
    }

    @Override
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
    @Override
    public List<ITypeInfo> getPossibleClassesForActivationToken(String actTok) {
        List<ITypeInfo> ret = new ArrayList<>();

        Iterator<ISimpleNode> it = this.scope.topDownIterator();
        if (!it.hasNext()) {
            return ret;
        }
        SimpleNode element = (SimpleNode) it.next();

        //ok, that's the scope we have to analyze

        ITypeInfo typeForParameter = NodeUtils.getTypeForParameterFromAST(actTok, element);
        if (typeForParameter != null) {
            ret.add(typeForParameter);
        }

        //Search for assert isinstance().
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(element, false);
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

            if (entry.node instanceof TryExcept) {
                excepthandlerType[] handlers = ((TryExcept) entry.node).handlers;
                if (handlers != null) {
                    for (excepthandlerType handle : handlers) {
                        if (handle.name != null && actTok.equals(NodeUtils.getFullRepresentationString(handle.name))) {
                            if (handle.type instanceof Tuple) {
                                exprType[] tupElts = ((Tuple) handle.type).elts;
                                if (tupElts != null) {
                                    for (exprType type : tupElts) {
                                        if (type != null) {
                                            ret.add(new TypeInfo(NodeUtils.getFullRepresentationString(type)));
                                        }
                                    }
                                }
                            } else if (handle.type != null) {
                                ret.add(new TypeInfo(NodeUtils.getFullRepresentationString(handle.type)));
                            }
                        }
                    }
                }
                continue;
            }
            if (entry.node instanceof Assert) {
                Assert assrt = (Assert) entry.node;
                if (assrt.test instanceof Call) {
                    Call call = (Call) assrt.test;
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
                                    ret.add(new TypeInfo(FullRepIterable.getWithoutLastPart(rep)));
                                }
                            }
                        }
                    }
                }
            } else if (entry.node instanceof Call && isTypingCast((Call) entry.node)) {
                Call call = (Call) entry.node;
                if (call.args != null && call.args.length > 0) {
                    exprType type = call.args[0];
                    addRepresentationIfPossible(ret, type);
                }
            } else {
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
                    if (commentType.id != null) {
                        String trim = commentType.id.trim();
                        if (trim.startsWith("#")) {
                            trim = trim.substring(1).trim();
                        }
                        if (trim.startsWith(":")) {
                            String type = NodeUtils.getTypeForParameterFromDocstring(actTok, trim.substring(1));
                            if (type != null) {
                                ret.add(new TypeInfo(type));
                            }
                        } else if (trim.startsWith("@")) {
                            String type = NodeUtils.getTypeForParameterFromDocstring(actTok, trim);
                            if (type != null) {
                                ret.add(new TypeInfo(type));
                            }
                        }
                    }

                } else if (object instanceof Str) {
                    Str str = (Str) object;
                    if (str.s != null) {
                        String trim = str.s.trim();
                        if (trim.startsWith("#")) {
                            trim = trim.substring(1).trim();
                        }
                        if (trim.startsWith(":")) {
                            String type = NodeUtils.getTypeForParameterFromDocstring(actTok, trim.substring(1));
                            if (type != null) {
                                ret.add(new TypeInfo(type));
                            }
                        } else if (trim.startsWith("@")) {
                            String type = NodeUtils.getTypeForParameterFromDocstring(actTok, trim);
                            if (type != null) {
                                ret.add(new TypeInfo(type));
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }

    private boolean isTypingCast(Call call) {
        if (call.func != null) {
            String callFuncRep = NodeUtils.getFullRepresentationString(call.func);
            return "typing.cast".equals(callFuncRep);
        }
        return false;
    }

    /**
     * @param ret the list where the representation should be added
     * @param expr the Name or Attribute that determines the class that should be added
     */
    private void addRepresentationIfPossible(List<ITypeInfo> ret, exprType expr) {
        if (expr instanceof Name || expr instanceof Attribute) {
            String string = NodeUtils.getFullRepresentationString(expr);
            if (string != null) {
                ret.add(new TypeInfo(string));
            }
        }
    }

    public IPythonNature getPythonNature() {
        return this.nature;
    }

    @Override
    public String getScopeStackPathNames() {
        Iterator<ISimpleNode> iterator = this.scope.iterator();
        return nodesIteratorToPathName(iterator);
    }

    private String nodesIteratorToPathName(Iterator<ISimpleNode> iterator) {
        FastStringBuffer buf = new FastStringBuffer();
        if (iterator.hasNext()) {
            SimpleNode next = (SimpleNode) iterator.next();
            if (next instanceof Module) {
                // just skip it
            } else {
                String rep = NodeUtils.getRepresentationString(next);
                if (rep != null && rep.length() > 0) {
                    buf.append(rep);
                }
            }
        }
        while (iterator.hasNext()) {
            SimpleNode next = (SimpleNode) iterator.next();
            String rep = NodeUtils.getRepresentationString(next);
            if (rep != null && rep.length() > 0) {
                if (!buf.isEmpty()) {
                    buf.append('.');
                }
                buf.append(rep);
            } else {
                return "";
            }
        }
        return buf.toString();
    }

    @Override
    public String getScopeStackPathNamesToLastClassDef() {
        List<ISimpleNode> arrayList = new ArrayList<>();
        boolean found = false;
        for (Iterator<ISimpleNode> it = this.scope.iterator(); it.hasNext();) {
            SimpleNode node = (SimpleNode) it.next();
            arrayList.add(node);
            if (node instanceof ClassDef) {
                found = true;
                break;
            }
        }
        if (!found) {
            return "";
        }
        return nodesIteratorToPathName(arrayList.iterator());
    }
}
