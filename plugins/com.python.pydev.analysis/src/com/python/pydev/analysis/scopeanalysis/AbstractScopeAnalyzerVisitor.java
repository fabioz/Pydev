/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 16, 2006
 */
package com.python.pydev.analysis.scopeanalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.codecompletion.revisited.CompletionCache;
import org.python.pydev.ast.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.ast.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.ast.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.ast.codecompletion.revisited.visitors.LocalScope;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.TupleN;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.DictComp;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.SetComp;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.comprehensionType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_core.structure.StringToIntCounterSmallSet;

import com.python.pydev.analysis.visitors.Found;
import com.python.pydev.analysis.visitors.GenAndTok;
import com.python.pydev.analysis.visitors.Scope;
import com.python.pydev.analysis.visitors.ScopeItems;

/**
 * This is a visitor that traverses the scopes available and is able to provide information
 * on all the scopes (subclasses should implement the specifics about it).
 *
 * @author Fabio
 */
public abstract class AbstractScopeAnalyzerVisitor extends VisitorBase {

    /**
     * nature is needed for imports
     */
    public final IPythonNature nature;

    /**
     * this is the name of the module we are visiting
     */
    public final String moduleName;

    /**
     * manage the scopes...
     */
    public final Scope scope;

    /**
     * this should get the tokens that are probably not used, but may be if they are defined
     * later (e.g.: if we have a method call inside a scope and the method is defined later)
     *
     * objects should not be added to it if we are at the global scope.
     */
    protected final List<Found> probablyNotDefined = new ArrayList<Found>();

    /**
     * this is the module we are visiting
     */
    public final IModule current;

    /**
     * To keep track of cancels
     */
    protected final IProgressMonitor monitor;

    /**
     * Document we're working on.
     */
    protected final IDocument document;

    /**
     * Helper so that we can keep a cache among the many requests to the code-completion engine.
     */
    public final ICompletionCache completionCache;

    private final LocalScope currentLocalScope;

    private final Set<String> builtinTokens = new HashSet<String>();

    public AbstractScopeAnalyzerVisitor(IPythonNature nature, String moduleName, IModule current, IDocument document,
            IProgressMonitor monitor) {
        this.monitor = monitor;
        this.current = current;
        this.nature = nature;
        currentLocalScope = new LocalScope(nature);
        this.moduleName = moduleName;
        this.document = document;
        this.scope = new Scope(this, nature, moduleName);
        if (current instanceof SourceModule) {
            this.currentLocalScope.getScopeStack().push(((SourceModule) current).getAst());
        }

        startScope(Scope.SCOPE_TYPE_GLOBAL, null); //initial scope - there is only one 'global'
        ICompletionState completionState = CompletionStateFactory
                .getEmptyCompletionState(nature, new CompletionCache());
        this.completionCache = completionState;

        TokensList builtinCompletions = nature.getAstManager().getBuiltinCompletions(completionState,
                new TokensList());

        if (moduleName != null && moduleName.endsWith("__init__")) {
            //__path__ should be added to modules that have __init__
            builtinCompletions
                    .addAll(new TokensList(new SourceToken(new Name("__path__", Name.Load, false), "__path__", "", "",
                            moduleName, nature)));
        }

        for (IterTokenEntry entry : builtinCompletions) {
            IToken t = entry.getToken();
            Found found = makeFound(t);
            org.python.pydev.shared_core.structure.Tuple<IToken, Found> tup = new org.python.pydev.shared_core.structure.Tuple<IToken, Found>(
                    t, found);
            addToNamesToIgnore(t, scope.getCurrScopeItems(), tup);
            builtinTokens.add(t.getRepresentation());
        }
    }

    protected void checkStop() {
        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    /**
     * nothing is additionally handled here (but all functions even the ones that treat it forward the call
     * to this method, so, it might be useful in subclasses).
     *
     * @see org.python.pydev.parser.jython.ast.VisitorBase#unhandled_node(org.python.pydev.parser.jython.SimpleNode)
     */
    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        checkStop();
        return null;
    }

    /**
     * transverse the node
     * @see org.python.pydev.parser.jython.ast.VisitorBase#traverse(org.python.pydev.parser.jython.SimpleNode)
     */
    @Override
    public void traverse(SimpleNode node) throws Exception {
        checkStop();
        node.traverse(this);
    }

    @Override
    public Object visitCall(final Call callNode) throws Exception {
        if (callNode.func != null) {
            onVisitCallFunc(callNode);
        }

        if (callNode.args != null) {
            for (int i = 0; i < callNode.args.length; i++) {
                if (callNode.args[i] != null) {
                    callNode.args[i].accept(this);
                }
            }
        }
        if (callNode.keywords != null) {
            for (int i = 0; i < callNode.keywords.length; i++) {
                if (callNode.keywords[i] != null) {
                    callNode.keywords[i].accept(this);
                }
            }
        }
        if (callNode.starargs != null) {
            callNode.starargs.accept(this);
        }
        if (callNode.kwargs != null) {
            callNode.kwargs.accept(this);
        }

        return null;
    }

    protected void onVisitCallFunc(Call callNode) throws Exception {
        callNode.func.accept(this);
    }

    /**
     * we are starting a new scope when visiting a class
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitClassDef(org.python.pydev.parser.jython.ast.ClassDef)
     */
    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        unhandled_node(node);

        AbstractScopeAnalyzerVisitor visitor = this;

        handleDecorators(node.decs);

        //we want to visit the bases before actually starting the class scope (as it's as if they're attribute
        //accesses).
        if (node.bases != null) {
            for (int i = 0; i < node.bases.length; i++) {
                if (node.bases[i] != null) {
                    node.bases[i].accept(visitor);
                }
            }
        }

        this.currentLocalScope.getScopeStack().push(node);
        startScope(Scope.SCOPE_TYPE_CLASS, node);

        if (node.name != null) {
            node.name.accept(visitor);
        }

        if (node.body != null) {
            for (int i = 0; i < node.body.length; i++) {
                if (node.body[i] != null) {
                    node.body[i].accept(visitor);
                }
            }
        }

        if (node.keywords != null) {
            for (int i = 0; i < node.keywords.length; i++) {
                if (node.keywords[i] != null) {
                    node.keywords[i].accept(visitor);
                }
            }
        }

        endScope(node);
        this.currentLocalScope.getScopeStack().pop();

        //the class is only added to the names to ignore when it's scope is resolved!
        addToNamesToIgnore(node, true, true);

        return null;
    }

    /**
     * used so that the token is added to the names to ignore...
     */
    protected void addToNamesToIgnore(SimpleNode node, boolean finishClassScope, boolean checkBuiltins) {
        SourceToken token = AbstractVisitor.makeToken(node, "", nature);

        if (checkBuiltins) {
            if (checkCurrentScopeForAssignmentsToBuiltins()) {
                String rep = token.getRepresentation();
                if (builtinTokens.contains(rep)) {
                    // Overriding builtin...
                    onAddAssignmentToBuiltinMessage(token, rep);
                }
            }
        }

        ScopeItems currScopeItems = scope.getCurrScopeItems();

        Found found = new Found(token, token, scope.getCurrScopeId(), scope.getCurrScopeItems());
        org.python.pydev.shared_core.structure.Tuple<IToken, Found> tup = new org.python.pydev.shared_core.structure.Tuple<IToken, Found>(
                token, found);
        addToNamesToIgnore(token, currScopeItems, tup);

        //after adding it to the names to ignore, let's see if there is someone waiting for this declaration
        //in the 'probably not defined' stack.
        for (Iterator<Found> it = probablyNotDefined.iterator(); it.hasNext();) {
            Found n = it.next();

            GenAndTok single = n.getSingle();
            int foundScopeType = single.scopeFound.getScopeType();
            //ok, if we are in a scope method, we may not get things that were defined in a class scope.
            if (((foundScopeType & Scope.ACCEPTED_METHOD_AND_LAMBDA) != 0)
                    && scope.getCurrScopeItems().getScopeType() == Scope.SCOPE_TYPE_CLASS) {
                continue;
            }
            IToken tok = single.tok;
            String rep = tok.getRepresentation();
            if (rep.equals(token.getRepresentation())) {
                //found match in names to ignore...

                if (finishClassScope && foundScopeType == Scope.SCOPE_TYPE_CLASS
                        && scope.getCurrScopeId() < single.scopeFound.getScopeId()) {
                    it.remove();
                    onAddUndefinedMessage(tok, found);
                } else {
                    it.remove();
                    onNotDefinedFoundLater(n, found);
                }
            }
        }
    }

    /**
     * We do not want to check for assignments to builtins when in the class-level, as those aren't going
     * to be accessed as globals later on.
     *
     * I.e.:
     * class A:
     *   id = 10
     *
     * must be accessed either as A.id or self.id, so, we don't need to warn about that.
     */
    private boolean checkCurrentScopeForAssignmentsToBuiltins() {
        return this.scope.getCurrScopeItems().getScopeType() != Scope.SCOPE_TYPE_CLASS;
    }

    protected void addToNamesToIgnore(IToken token, ScopeItems currScopeItems,
            org.python.pydev.shared_core.structure.Tuple<IToken, Found> tup) {
        currScopeItems.namesToIgnore.put(token.getRepresentation(), tup);
        onAfterAddToNamesToIgnore(currScopeItems, tup);
    }

    /**
     * we are starting a new scope when visiting a function
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitFunctionDef(org.python.pydev.parser.jython.ast.FunctionDef)
     */
    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        unhandled_node(node);
        addToNamesToIgnore(node, false, true);

        AbstractScopeAnalyzerVisitor visitor = this;
        argumentsType args = node.args;

        //visit the defaults first (before starting the scope, because this is where the load of variables from other scopes happens)
        if (args.defaults != null) {
            for (exprType expr : args.defaults) {
                if (expr != null) {
                    expr.accept(visitor);
                }
            }
        }

        //visit keyword only args with defaults
        if (args.kw_defaults != null) {
            for (exprType expr : args.kw_defaults) {
                if (expr != null) {
                    expr.accept(visitor);
                }
            }
        }

        if (args.kwonlyargannotation != null) {
            for (exprType expr : args.kwonlyargannotation) {
                if (expr != null) {
                    expr.accept(visitor);
                }
            }
        }

        //visit annotation
        if (args.annotation != null) {
            for (exprType expr : args.annotation) {
                if (expr != null) {
                    expr.accept(visitor);
                }
            }
        }

        //visit the return
        if (node.returns != null) {
            node.returns.accept(visitor);
        }

        //then the decorators (no, still not in method scope)
        handleDecorators(node.decs);

        startScope(Scope.SCOPE_TYPE_METHOD, node);
        this.currentLocalScope.getScopeStack().push(node);

        scope.isInMethodDefinition = true;
        //visit regular args
        if (args.args != null) {
            for (exprType expr : args.args) {
                expr.accept(visitor);
            }
        }

        //visit varargs
        if (args.vararg != null) {
            args.vararg.accept(visitor);
        }

        //visit kwargs
        if (args.kwarg != null) {
            args.kwarg.accept(visitor);
        }

        //visit keyword only args
        if (args.kwonlyargs != null) {
            for (exprType expr : args.kwonlyargs) {
                if (expr != null) {
                    expr.accept(visitor);
                }
            }
        }

        scope.isInMethodDefinition = false;

        //visit the body
        if (node.body != null) {
            for (int i = 0; i < node.body.length; i++) {
                if (node.body[i] != null) {
                    node.body[i].accept(visitor);
                }
            }
        }

        endScope(node); //don't report unused variables if the method is virtual
        this.currentLocalScope.getScopeStack().pop();
        return null;
    }

    protected void handleDecorators(decoratorsType[] decs) throws Exception {
        if (decs != null) {
            for (decoratorsType dec : decs) {
                if (dec != null) {
                    handleDecorator(dec);
                }
            }
        }
    }

    /**
     * Traverses the decorator.
     */
    protected void handleDecorator(decoratorsType dec) throws Exception {
        dec.accept(this);
    }

    /**
     * we are starting a new scope when visiting a lambda
     */
    @Override
    public Object visitLambda(org.python.pydev.parser.jython.ast.Lambda node) throws Exception {
        unhandled_node(node);

        AbstractScopeAnalyzerVisitor visitor = this;
        argumentsType args = node.args;

        //visit the defaults first (before starting the scope, because this is where the load of variables from other scopes happens)
        if (args.defaults != null) {
            for (exprType expr : args.defaults) {
                if (expr != null) {
                    expr.accept(visitor);
                }
            }
        }

        startScope(Scope.SCOPE_TYPE_LAMBDA, node);

        scope.isInMethodDefinition = true;
        //visit regular args
        if (args.args != null) {
            for (exprType expr : args.args) {
                expr.accept(visitor);
            }
        }

        //visit varargs
        if (args.vararg != null) {
            args.vararg.accept(visitor);
        }

        //visit kwargs
        if (args.kwarg != null) {
            args.kwarg.accept(visitor);
        }

        //visit keyword only args
        if (args.kwonlyargs != null) {
            for (exprType expr : args.kwonlyargs) {
                expr.accept(visitor);
            }
        }

        scope.isInMethodDefinition = false;

        //visit the body
        if (node.body != null) {
            node.body.accept(visitor);
        }

        endScope(node);
        return null;
    }

    /**
     * We want to make the name tok a regular name for interpreting purposes.
     */
    @Override
    public Object visitNameTok(NameTok nameTok) throws Exception {
        unhandled_node(nameTok);
        if (nameTok.ctx == NameTok.VarArg || nameTok.ctx == NameTok.KwArg) {

            SourceToken token = AbstractVisitor.makeToken(nameTok, moduleName, nature);
            scope.addToken(token, token, (nameTok).id);
            if (checkCurrentScopeForAssignmentsToBuiltins()) {
                if (builtinTokens.contains(token.getRepresentation())) {
                    // Overriding builtin...
                    onAddAssignmentToBuiltinMessage(token, token.getRepresentation());
                }
            }
        }
        return null;
    }

    @Override
    public Object visitAugAssign(AugAssign node) throws Exception {
        return super.visitAugAssign(node);
    }

    /**
     * when visiting an import, just make the token and add it
     *
     * e.g.: if it is an import such as 'os.path', it will return 2 tokens, one for 'os' and one for 'os.path',
     *
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitImport(org.python.pydev.parser.jython.ast.Import)
     */
    @Override
    public Object visitImport(Import node) throws Exception {
        unhandled_node(node);
        List<IToken> list = AbstractVisitor.makeImportToken(node, null, moduleName, true, nature);

        if (checkCurrentScopeForAssignmentsToBuiltins()) {
            for (IToken token : list) {
                if (builtinTokens.contains(token.getRepresentation())) {
                    // Overriding builtin...
                    onAddAssignmentToBuiltinMessage(token, token.getRepresentation());
                }
            }
        }
        scope.addImportTokens(new TokensList(list), null, this.completionCache);
        return null;
    }

    /**
     * visit some import
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitImportFrom(org.python.pydev.parser.jython.ast.ImportFrom)
     */
    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        unhandled_node(node);
        try {

            if (AbstractVisitor.isWildImport(node)) {
                IToken wildImport = AbstractVisitor.makeWildImportToken(node, null, moduleName, nature);

                ICompletionState state = CompletionStateFactory.getEmptyCompletionState(nature, this.completionCache);
                state.setBuiltinsGotten(true); //we don't want any builtins
                TokensList completionsForWildImport = new TokensList();
                if (nature.getAstManager().getCompletionsForWildImport(state, current, completionsForWildImport,
                        wildImport)) {
                    scope.addImportTokens(completionsForWildImport, wildImport, this.completionCache);
                }
            } else {
                List<IToken> list = AbstractVisitor.makeImportToken(node, null, moduleName, true, nature);
                scope.addImportTokens(new TokensList(list), null, this.completionCache);
            }

        } catch (Exception e) {
            Log.log(IStatus.ERROR, ("Error when analyzing module " + moduleName), e);
        }
        return null;
    }

    /**
     * Visiting some name
     *
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitName(org.python.pydev.parser.jython.ast.Name)
     */
    @Override
    public Object visitName(Name node) throws Exception {
        unhandled_node(node);
        //when visiting the global namespace, we don't go into any inner scope.
        SourceToken token = AbstractVisitor.makeToken(node, moduleName, nature);
        boolean found = true;
        //on aug assign, it has to enter both, the load and the read (but first the load, because it can be undefined)
        if (node.ctx == Name.Load || node.ctx == Name.Del || node.ctx == Name.AugStore) {
            found = markRead(token);
        }

        if (node.ctx == Name.Store || node.ctx == Name.Param || node.ctx == Name.KwOnlyParam
                || (node.ctx == Name.AugStore && found)) { //if it was undefined on augstore, we do not go on to creating the token
            String rep = token.getRepresentation();
            if (checkCurrentScopeForAssignmentsToBuiltins()) {
                if (builtinTokens.contains(rep)) {
                    // Overriding builtin...
                    onAddAssignmentToBuiltinMessage(token, rep);
                }
            }
            org.python.pydev.shared_core.structure.Tuple<IToken, Found> foundInNamesToIgnore = findInNamesToIgnore(rep,
                    token);

            if (foundInNamesToIgnore == null) {

                if (!rep.equals("self") && !rep.equals("cls")) {
                    scope.addToken(token, token);
                } else {
                    addToNamesToIgnore(node, false, false); //ignore self
                }
            }
        }

        return token;
    }

    /**
     * @param rep the representation we're looking for
     * @return whether the representation is in the names to ignore
     */
    protected org.python.pydev.shared_core.structure.Tuple<IToken, Found> findInNamesToIgnore(String rep,
            IToken token) {
        org.python.pydev.shared_core.structure.Tuple<IToken, Found> found = scope.findInNamesToIgnore(rep);
        return found;
    }

    @Override
    public Object visitGlobal(Global node) throws Exception {
        unhandled_node(node);
        for (NameTokType name : node.names) {
            Name nameAst = new Name(((NameTok) name).id, Name.Store, false);
            nameAst.beginLine = name.beginLine;
            nameAst.beginColumn = name.beginColumn;

            SourceToken token = AbstractVisitor.makeToken(nameAst, moduleName, nature);
            scope.addTokenToGlobalScope(token);
            addToNamesToIgnore(nameAst, false, true); // it is global, so, ignore it...
        }
        return null;
    }

    private int visitingAttributeStackI = 0;
    private StringToIntCounterSmallSet visitingAttributeStack = null;
    private final StringToIntCounterSmallSet visitingAttributeStackCache = new StringToIntCounterSmallSet();

    /**
     * visiting some attribute, as os.path or math().val or (10,10).__class__
     *
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitAttribute(org.python.pydev.parser.jython.ast.Attribute)
     */
    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        SourceToken token = AbstractVisitor.makeFullNameToken(node, moduleName, nature);
        unhandled_node(node);

        visitingAttributeStackI += 1;
        if (visitingAttributeStackI == 1) {
            //Mostly a cache helper so that we don't incur the cost of creating a new one every time we find
            //an attribute, as only one will be active at any time when we start checking an attribute.
            visitingAttributeStack = visitingAttributeStackCache;
        }
        String representation = token.getRepresentation();
        int curr = visitingAttributeStack.increment(representation);

        try {
            boolean doReturn = visitNeededAttributeParts(node, this);

            if (representation.length() == 0) {
                return null;
            }

            if (doReturn) {
                return null;
            }

            String fullRep = representation;

            if (node.ctx == Attribute.Store || node.ctx == Attribute.Param || node.ctx == Attribute.KwOnlyParam
                    || node.ctx == Attribute.AugStore) {
                //in a store attribute, the first part is always a load
                int i = fullRep.indexOf('.', 0);
                String sub = fullRep;
                if (i > 0) {
                    sub = fullRep.substring(0, i);
                }
                markRead(token, sub, true, false);

            } else if (node.ctx == Attribute.Load) {

                Iterator<String> it = new FullRepIterable(fullRep).iterator();
                boolean found = false;

                while (it.hasNext()) {
                    String sub = it.next();
                    if (it.hasNext()) {
                        if (markRead(token, sub, false, false)) {
                            found = true;
                        }
                    } else {
                        //I.e.: if we're in a structure where an attribute visits another attribute, we'll only
                        //report as not defined if it's the last one (i.e.: the last one in the stack we created).
                        boolean addToNotDefined = !found && visitingAttributeStack.get(representation) == curr;
                        markRead(token, fullRep, addToNotDefined, true); //only set it to add to not defined if it was still not found
                    }
                }
            }
        } finally {
            visitingAttributeStackI -= 1;
            if (visitingAttributeStackI == 0) {
                visitingAttributeStack.clear();
                visitingAttributeStack = null;
            }
        }
        return null;
    }

    /**
     * In this function, the visitor will traverse the value of the attribute as needed,
     * if it is a subscript, call, etc, as those things are not actually a part of the attribute,
     * but are rather 'in' the attribute.
     *
     * @param node the attribute to visit
     * @param base the visitor that should visit the elements inside the attribute
     * @return true if there's no need to keep visiting other stuff in the attribute
     * @throws Exception
     */
    public static boolean visitNeededAttributeParts(final Attribute node, VisitorBase base) throws Exception {
        exprType value = node.value;
        boolean valueVisited = false;
        boolean doReturn = false;
        if (value instanceof Subscript) {
            Subscript subs = (Subscript) value;
            base.traverse(subs.slice);
            if (subs.value instanceof Name) {
                base.visitName((Name) subs.value);
            } else {
                base.traverse(subs.value);
            }
            //No need to keep visiting. Reason:
            //Let's take the example:
            //print function()[0].strip()
            //function()[0] is part 1 of attribute
            //
            //and the .strip will constitute the second part of the attribute
            //and its value (from the subscript) constitutes the 'function' part,
            //so, when we visit it directly, we don't have to visit the first part anymore,
            //because it was just visited... kind of strange to think about it though.
            doReturn = true;

        } else if (value instanceof Call) {
            visitCallAttr((Call) value, base);
            valueVisited = true;

        } else if (value instanceof Tuple) {
            base.visitTuple((Tuple) value);
            valueVisited = true;

        } else if (value instanceof Dict) {
            base.visitDict((Dict) value);
            doReturn = true;
        }
        if (!doReturn && !valueVisited) {
            if (visitNeededValues(value, base)) {
                doReturn = true;
            }
        }
        return doReturn;
    }

    protected static boolean visitNeededValues(exprType value, VisitorBase base) throws Exception {
        if (value instanceof Name) {
            return false;
        } else if (value instanceof Attribute) {
            return visitNeededValues(((Attribute) value).value, base);
        } else {
            value.accept(base);
            return true;
        }
    }

    /**
     * used if we want to visit all in a call but the func itself (that's the call name).
     */
    protected static void visitCallAttr(Call c, VisitorBase base) throws Exception {
        //now, visit all inside it but the func itself
        VisitorBase visitor = base;
        if (c.func instanceof Attribute) {
            base.visitAttribute((Attribute) c.func);
        }
        if (c.args != null) {
            for (int i = 0; i < c.args.length; i++) {
                if (c.args[i] != null) {
                    c.args[i].accept(visitor);
                }
            }
        }
        if (c.keywords != null) {
            for (int i = 0; i < c.keywords.length; i++) {
                if (c.keywords[i] != null) {
                    c.keywords[i].accept(visitor);
                }
            }
        }
        if (c.starargs != null) {
            c.starargs.accept(visitor);
        }
        if (c.kwargs != null) {
            c.kwargs.accept(visitor);
        }
    }

    @Override
    public Object visitFor(For node) throws Exception {
        scope.addIfSubScope();
        Object ret = super.visitFor(node);
        scope.removeIfSubScope();
        return ret;
    }

    /**
     * Overridden because we want the value to be visited before the targets
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitAssign(org.python.pydev.parser.jython.ast.Assign)
     */
    @Override
    public Object visitAssign(Assign node) throws Exception {
        unhandled_node(node);
        //in 'target1 = target2 = value', this is 'value'
        if (node.value != null) {
            node.value.accept(this);
        }

        //in 'target1 = target2 = a', this is 'target1, target2'
        if (node.targets != null) {
            for (int i = 0; i < node.targets.length; i++) {
                if (node.targets[i] != null) {
                    node.targets[i].accept(this);
                }
            }
        }
        onAfterVisitAssign(node);
        return null;
    }

    /**
     * Overridden because we need to know about if scopes
     */
    @Override
    public Object visitIf(If node) throws Exception {
        scope.addIfSubScope();
        Object r = super.visitIf(node);
        scope.removeIfSubScope();
        return r;
    }

    /**
     * Overridden because we need to know about while scopes
     */
    @Override
    public Object visitWhile(While node) throws Exception {
        scope.addIfSubScope();
        Object r = super.visitWhile(node);
        scope.removeIfSubScope();
        return r;
    }

    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        scope.addTryExceptSubScope(node);
        Object r = super.visitTryExcept(node);
        scope.removeTryExceptSubScope();
        return r;
    }

    @Override
    public Object visitTryFinally(TryFinally node) throws Exception {
        scope.addIfSubScope();
        Object r = super.visitTryFinally(node);
        scope.removeIfSubScope();
        return r;
    }

    @Override
    public Object visitDictComp(DictComp node) throws Exception {
        unhandled_node(node);
        if (node.generators != null) {
            for (int i = 0; i < node.generators.length; i++) {
                if (node.generators[i] != null) {
                    node.generators[i].accept(this);
                }
            }
        }
        if (node.key != null) {
            node.key.accept(this);
        }
        if (node.value != null) {
            node.value.accept(this);
        }
        return null;
    }

    @Override
    public Object visitSetComp(SetComp node) throws Exception {
        unhandled_node(node);
        if (node.generators != null) {
            for (int i = 0; i < node.generators.length; i++) {
                if (node.generators[i] != null) {
                    node.generators[i].accept(this);
                }
            }
        }
        if (node.elt != null) {
            node.elt.accept(this);
        }
        return null;
    }

    /**
     * Overridden because we need to visit the generators first
     *
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitListComp(org.python.pydev.parser.jython.ast.ListComp)
     */
    @Override
    public Object visitListComp(final ListComp node) throws Exception {
        unhandled_node(node);
        if (node.ctx == ListComp.TupleCtx) {
            startScope(Scope.SCOPE_TYPE_LIST_COMP, node);
        }
        try {
            Comprehension type = null;
            if (node.generators != null && node.generators.length > 0) {
                type = (Comprehension) node.generators[0];
            }
            List<exprType> eltsToVisit = new ArrayList<exprType>();

            //we need to take care of 'nested list comprehensions'
            if (type != null && type.iter instanceof ListComp) {
                //print dict((day, index) for index, daysRep in (day for day in enumeratedDays))
                final ListComp listComp = (ListComp) type.iter;

                //the "(day for day in enumeratedDays)" is in its own scope
                if (listComp.ctx == ListComp.TupleCtx) {
                    startScope(Scope.SCOPE_TYPE_LIST_COMP, listComp);
                }
                try {
                    visitListCompGenerators(listComp, eltsToVisit);
                    for (exprType type2 : eltsToVisit) {
                        type2.accept(this);
                    }
                } finally {
                    if (listComp.ctx == ListComp.TupleCtx) {
                        endScope(listComp);
                    }
                }
                type.target.accept(this);
                if (node.elt != null) {
                    node.elt.accept(this);
                }

                return null;
            }

            //then the generators...
            if (node.generators != null) {
                for (int i = 0; i < node.generators.length; i++) {
                    if (node.generators[i] != null) {
                        node.generators[i].accept(this);
                    }
                }
            }

            //we need to take care of 'nested list comprehensions'
            if (node.elt instanceof ListComp) {
                //print dict((day, index) for index, daysRep in enumeratedDays for day in daysRep)
                //note that the daysRep is actually generated and used later in the expression
                visitListCompGenerators((ListComp) node.elt, eltsToVisit);
                for (exprType type2 : eltsToVisit) {
                    type2.accept(this);
                }
                return null;
            }

            if (node.elt != null) {
                node.elt.accept(this);
            }

            return null;
        } finally {
            if (node.ctx == ListComp.TupleCtx) {
                endScope(node);
            }
        }
    }

    private void visitListCompGenerators(ListComp node, List<exprType> eltsToVisit) throws Exception {
        for (comprehensionType c : node.generators) {
            Comprehension comp = (Comprehension) c;
            if (node.elt instanceof ListComp) {
                visitListCompGenerators((ListComp) node.elt, eltsToVisit);
                comp.accept(this);
            } else {
                comp.accept(this);
                eltsToVisit.add(node.elt);
            }
        }
    }

    /**
     * initializes a new scope
     * @param node
     */
    protected void startScope(int newScopeType, SimpleNode node) {
        scope.startScope(newScopeType);
        onAfterStartScope(newScopeType, node);
    }

    /**
     * finalizes the current scope
     * @param reportUnused: defines whether we should report unused things found (we may not want to do that
     * when we have an abstract method)
     */
    protected void endScope(SimpleNode node) {
        onBeforeEndScope(node);

        ScopeItems m = scope.endScope(); //clear the last scope
        for (Iterator<Found> it = probablyNotDefined.iterator(); it.hasNext();) {
            Found n = it.next();

            final GenAndTok probablyNotDefinedFirst = n.getSingle();
            IToken tok = probablyNotDefinedFirst.tok;
            String rep = tok.getRepresentation();
            //we also get a last pass to the unused to see if they might have been defined later on the higher scope

            List<Found> foundItems = find(m, rep);
            boolean setUsed = false;
            for (Found found : foundItems) {
                //the scope where it is defined must be an outer scope so that we can say it was defined later...
                final GenAndTok foundItemFirst = found.getSingle();

                //if something was not defined in a method, if we are in the class definition, it won't be found.

                if ((probablyNotDefinedFirst.scopeFound.getScopeType() & Scope.ACCEPTED_METHOD_AND_LAMBDA) != 0
                        && m.getScopeType() != Scope.SCOPE_TYPE_CLASS) {
                    if (foundItemFirst.scopeId < probablyNotDefinedFirst.scopeId) {
                        found.setUsed(true);
                        setUsed = true;
                    }
                }
            }
            if (setUsed) {
                it.remove();
            }
        }

        //ok, this was the last scope, so, the ones probably not defined are really not defined at this
        //point
        if (scope.size() == 0) {
            onLastScope(m);
        }

        onAfterEndScope(node, m);
    }

    /**
     * Finds an item given its full representation (so, os.path can be found as 'os' and 'os.path')
     */
    protected List<Found> find(ScopeItems m, String fullRep) {
        ArrayList<Found> foundItems = new ArrayList<Found>();
        if (m == null) {
            return foundItems;
        }

        int i = fullRep.indexOf('.', 0);

        while (i >= 0) {
            String sub = fullRep.substring(0, i);
            i = fullRep.indexOf('.', i + 1);
            foundItems.addAll(m.getAll(sub));
        }

        foundItems.addAll(m.getAll(fullRep));
        return foundItems;
    }

    /**
     * we just found a token, so let's mark the correspondent tokens read (or undefined)
     * @return true if it was found
     */
    protected boolean markRead(IToken token) {
        String rep = token.getRepresentation();
        return markRead(token, rep, true, false);
    }

    /**
     * marks a token as read given its representation
     *
     * @param token the token to be added
     * @param rep the token representation
     * @param addToNotDefined determines if it should be added to the 'not defined tokens' stack or not
     * @return true if it was found
     */
    protected boolean markRead(IToken token, String rep, boolean addToNotDefined, boolean checkIfIsValidImportToken) {
        boolean found = false;
        Found foundAs = null;
        String foundAsStr = null;

        int acceptedScopes = 0;
        ScopeItems currScopeItems = scope.getCurrScopeItems();

        if ((currScopeItems.getScopeType() & Scope.ACCEPTED_METHOD_AND_LAMBDA) != 0) {
            acceptedScopes = Scope.ACCEPTED_METHOD_SCOPES;
        } else {
            acceptedScopes = Scope.ACCEPTED_ALL_SCOPES;
        }

        if ("locals".equals(rep)) {
            //if locals() is accessed, all the tokens currently found are marked as 'used'
            //use case:
            //
            //def f2():
            //    a = 1
            //    b = 2
            //    c = 3
            //    f1(**locals())
            currScopeItems.setAllUsed();
            return true;
        }

        Iterator<String> it = new FullRepIterable(rep, true).iterator();
        //search for it
        while (found == false && it.hasNext()) {
            String nextTokToSearch = it.next();
            foundAs = scope.findFirst(nextTokToSearch, true, acceptedScopes);
            found = foundAs != null;
            if (found) {
                foundAsStr = nextTokToSearch;
                foundAs.getSingle().references.add(token);
                onFoundTokenAs(token, foundAs);
            }
        }

        if (!found) {
            //this token might not be defined... (still, might be in names to ignore)
            int i;
            if ((i = rep.indexOf('.')) != -1) {
                //if it is an attribute, we have to check the names to ignore just with its first part
                rep = rep.substring(0, i);
            }
            if (addToNotDefined) {
                org.python.pydev.shared_core.structure.Tuple<IToken, Found> foundInNamesToIgnore = findInNamesToIgnore(
                        rep, token);
                if (foundInNamesToIgnore == null) {
                    Found foundForProbablyNotDefined = makeFound(token);
                    if (scope.size() > 1) { //if we're not in the global scope, it might be defined later
                        probablyNotDefined.add(foundForProbablyNotDefined); //we are not in the global scope, so it might be defined later...
                        onAddToProbablyNotDefined(token, foundForProbablyNotDefined);
                    } else {
                        onAddUndefinedMessage(token, foundForProbablyNotDefined); //it is in the global scope, so, it is undefined.
                    }
                } else {
                    IToken tokenInNamesToIgnore = foundInNamesToIgnore.o1;
                    onFoundInNamesToIgnore(token, tokenInNamesToIgnore);
                }
            }
        } else if (checkIfIsValidImportToken) {
            //ok, it was found, but is it an attribute (and if so, are all the parts in the import defined?)
            //if it was an attribute (say xxx and initially it was xxx.foo, we will have to check if the token foo
            //really exists in xxx, if it was found as an import)
            try {
                if (foundAs.isImport() && !rep.equals(foundAsStr) && foundAs.importInfo != null
                        && foundAs.importInfo.wasResolved) {
                    //the foundAsStr equals the module resolved in the Found tok

                    IModule m = foundAs.importInfo.mod;
                    String tokToCheck;
                    if (foundAs.isWildImport()) {
                        tokToCheck = foundAsStr;

                    } else {
                        String tok = foundAs.importInfo.rep;
                        tokToCheck = rep.substring(foundAsStr.length() + 1);
                        if (tok.length() > 0) {
                            tokToCheck = tok + "." + tokToCheck;
                        }
                    }

                    for (String repToCheck : new FullRepIterable(tokToCheck)) {
                        int inGlobalTokens = m.isInGlobalTokens(repToCheck, nature, true, true, this.completionCache);

                        if (inGlobalTokens == IModule.NOT_FOUND) {
                            if (!isDefinitionUnknown(m, repToCheck)) {
                                //Check if there's some hasattr (if there is, we'll consider that the token which
                                //had the hasattr checked will actually have it).
                                TokensList interfaceForLocal = this.currentLocalScope.getInterfaceForLocal(
                                        foundAsStr, false, true);
                                boolean foundInHasAttr = false;
                                for (IterTokenEntry entry : interfaceForLocal) {
                                    IToken iToken = entry.getToken();
                                    if (iToken.getRepresentation().equals(repToCheck)) {
                                        foundInHasAttr = true;
                                        break;
                                    }
                                }

                                if (!foundInHasAttr) {
                                    IToken foundTok = findNameTok(token, repToCheck);
                                    onAddUndefinedVarInImportMessage(foundTok, foundAs);
                                }
                            }
                            break;//no need to keep checking once one is not defined

                        } else if (inGlobalTokens == IModule.FOUND_BECAUSE_OF_GETATTR) {
                            break;
                        }
                    }
                } else if (foundAs.isImport() && (foundAs.importInfo == null || !foundAs.importInfo.wasResolved)) {
                    //import was not resolved
                    onFoundUnresolvedImportPart(token, rep, foundAs);
                }
            } catch (Exception e) {
                Log.log("Error checking for valid tokens (imports) for " + moduleName, e);
            }
        }
        return found;
    }

    protected void onFoundInNamesToIgnore(IToken token, IToken tokenInNamesToIgnore) {

    }

    protected void onFoundTokenAs(IToken token, Found foundAs) {

    }

    /**
     * @return whether we're actually unable to identify that the representation
     * we're looking exists or not, so,
     * True is returned if we're really unable to identify if that token does
     * not exist and
     * False if we're sure it does not exist
     */
    private boolean isDefinitionUnknown(IModule m, String repToCheck) throws Exception {
        String name = m.getName();
        TupleN key = new TupleN("isDefinitionUnknown", name != null ? name : "", repToCheck);
        Boolean isUnknown = (Boolean) this.completionCache.getObj(key);
        if (isUnknown == null) {
            isUnknown = internalGenerateIsDefinitionUnknown(m, repToCheck);
            this.completionCache.add(key, isUnknown);
        }
        return isUnknown;
    }

    /**
     * Actually makes the check to see if a given representation is unknown in a given module (without using caches)
     */
    private boolean internalGenerateIsDefinitionUnknown(IModule m, String repToCheck) throws Exception {
        if (!(m instanceof SourceModule)) {
            return false;
        }
        repToCheck = FullRepIterable.headAndTail(repToCheck, true)[0];
        if (repToCheck.length() == 0) {
            return false;
        }
        IDefinition[] definitions = m.findDefinition(
                CompletionStateFactory.getEmptyCompletionState(repToCheck, nature, this.completionCache), -1, -1,
                nature);
        for (int i = 0; i < definitions.length; i++) {
            IDefinition foundDefinition = definitions[i];
            if (foundDefinition instanceof AssignDefinition) {
                AssignDefinition d = (AssignDefinition) foundDefinition;

                //if the value is currently None, it will be set later on
                if (d.value.equals("None")) {
                    return true;
                }

                //ok, go to the definition of whatever is set
                IDefinition[] definitions2 = d.module.findDefinition(
                        CompletionStateFactory.getEmptyCompletionState(d.value, nature, this.completionCache), d.line,
                        d.col, nature);

                if (definitions2.length == 1) {
                    //and if it is a function, we're actually unable to find
                    //out about its return value
                    if (definitions2[0] instanceof Definition) {
                        Definition definition = (Definition) definitions2[0];
                        if (definition.ast instanceof FunctionDef) {
                            return true;

                        } else if (definition.ast instanceof ClassDef) {
                            ClassDef def = (ClassDef) definition.ast;
                            if (isDynamicClass(def)) {
                                return true;
                            }
                        }
                    }
                }
            } else if (foundDefinition instanceof Definition) { //not Assign definition
                Definition definition = (Definition) foundDefinition;
                if (definition.ast instanceof ClassDef) {
                    //direct class access
                    ClassDef classDef = (ClassDef) definition.ast;
                    if (isDynamicClass(classDef)) {
                        return true;
                    }
                }

            }
        }
        return false;
    }

    /**
     * @return whether the passed class definition has a docstring indicating that it has dynamic
     */
    private boolean isDynamicClass(ClassDef def) {
        String docString = NodeUtils.getNodeDocString(def);
        if (docString != null) {
            if (docString.indexOf("@DynamicAttrs") != -1) {
                //class that has things dynamically defined.
                return true;
            }
        }
        return false;
    }

    protected Found makeFound(IToken token) {
        return new Found(token, token, scope.getCurrScopeId(), scope.getCurrScopeItems());
    }

    protected IToken findNameTok(IToken token, String tokToCheck) {
        if (token instanceof SourceToken) {
            SourceToken s = (SourceToken) token;
            SimpleNode ast = s.getAst();

            String searchFor = FullRepIterable.getLastPart(tokToCheck);
            while (ast instanceof Attribute) {
                Attribute a = (Attribute) ast;

                if (((NameTok) a.attr).id.equals(searchFor)) {
                    return new SourceToken(a.attr, searchFor, "", "", token.getParentPackage(), nature);

                } else if (a.value.toString().equals(searchFor)) {
                    return new SourceToken(a.value, searchFor, "", "", token.getParentPackage(), nature);
                }
                ast = a.value;
            }
        }
        return token;
    }

    //these are the methods that should be overridden. Those are hooks to subclasses do whatever they need to do
    //on those cases
    protected abstract void onAfterVisitAssign(Assign node);

    protected abstract void onAfterStartScope(int newScopeType, SimpleNode node);

    protected abstract void onBeforeEndScope(SimpleNode node);

    protected abstract void onAfterEndScope(SimpleNode node, ScopeItems m);

    protected abstract void onLastScope(ScopeItems m);

    protected abstract void onAddUndefinedMessage(IToken token, Found foundAs);

    /**
     * Called when a token is not found.
     */
    protected void onAddToProbablyNotDefined(IToken token, Found foundForProbablyNotDefined) {
    }

    /**
     * Called when a token that was thought to be not defined is found later on in the visiting process.
     */
    protected void onNotDefinedFoundLater(Found foundInProbablyNotDefined, Found laterFound) {
        foundInProbablyNotDefined.reportDefined(laterFound);
    }

    protected abstract void onAddUndefinedVarInImportMessage(IToken foundTok, Found foundAs);

    public abstract void onAddUnusedMessage(SimpleNode node, Found found);

    public abstract void onAddReimportMessage(Found newFound);

    public abstract void onAddUnresolvedImport(IToken token);

    protected abstract void onAddAssignmentToBuiltinMessage(IToken foundTok, String representation);

    /**
     * This one is not abstract, but is provided as a hook, as the others.
     */
    protected void onAfterAddToNamesToIgnore(ScopeItems currScopeItems,
            org.python.pydev.shared_core.structure.Tuple<IToken, Found> tup) {
    }

    /**
     * This one is not abstract, but is provided as a hook, as the others.
     */
    protected void onFoundUnresolvedImportPart(IToken token, String rep, Found foundAs) {
    }

    /**
     * This one is not abstract, but is provided as a hook, as the others.
     */
    public void onImportInfoSetOnFound(Found found) {
    }

}
