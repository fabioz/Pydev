/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.comprehensionType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.structure.FastStack;

/**
 * @author Fabio Zadrozny
 */
public class FindDefinitionModelVisitor extends AbstractVisitor {

    /**
     * This is the token to find.
     */
    private String tokenToFind;

    /**
     * List of definitions.
     */
    public List<Definition> definitions = new ArrayList<Definition>();

    /**
     * Stack of classes / methods to get to a definition.
     */
    private FastStack<SimpleNode> defsStack = new FastStack<SimpleNode>(20);

    /**
     * This is a stack that will keep the globals for each stack
     */
    private FastStack<Set<String>> globalDeclarationsStack = new FastStack<Set<String>>(20);

    /**
     * This is the module we are visiting: just a weak reference so that we don't create a cycle (let's
     * leave things easy for the garbage collector).
     */
    private WeakReference<IModule> module;

    /**
     * It is only available if the cursor position is upon a NameTok in an import (it represents the complete
     * path for finding the module from the current module -- it can be a regular or relative import).
     */
    public String moduleImported;

    /**
     * Starts at 1
     */
    private int line;

    /**
     * Starts at 1
     */
    private int col;

    private boolean foundAsDefinition = false;

    private Definition definitionFound;

    /**
     * Call is stored for the context for a keyword parameter
     */
    private Stack<Call> call = new Stack<Call>();

    /**
     * Constructor
     * @param line: starts at 1
     * @param col: starts at 1
     */
    public FindDefinitionModelVisitor(String token, int line, int col, IModule module) {
        this.tokenToFind = token;
        this.module = new WeakReference<IModule>(module);
        this.line = line;
        this.col = col;
        this.moduleName = module.getName();
        //we may have a global declared in the global scope
        globalDeclarationsStack.push(new HashSet<String>());
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        String modRep = NodeUtils.getRepresentationString(node.module);
        if (NodeUtils.isWithin(line, col, node.module)) {
            //it is a token in the definition of a module
            int startingCol = node.module.beginColumn;
            int endingCol = startingCol;
            while (endingCol < this.col) {
                endingCol++;
            }
            int lastChar = endingCol - startingCol;
            moduleImported = modRep.substring(0, lastChar);
            int i = lastChar;
            while (i < modRep.length()) {
                if (Character.isJavaIdentifierPart(modRep.charAt(i))) {
                    i++;
                } else {
                    break;
                }
            }
            moduleImported += modRep.substring(lastChar, i);
        } else {
            //it was not the module, so, we have to check for each name alias imported
            for (aliasType alias : node.names) {
                //we do not check the 'as' because if it is some 'as', it will be gotten as a global in the module
                if (NodeUtils.isWithin(line, col, alias.name)) {
                    moduleImported = modRep + "." + NodeUtils.getRepresentationString(alias.name);
                }
            }
        }
        return super.visitImportFrom(node);
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#unhandled_node(org.python.pydev.parser.jython.SimpleNode)
     */
    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#traverse(org.python.pydev.parser.jython.SimpleNode)
     */
    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitClassDef(org.python.pydev.parser.jython.ast.ClassDef)
     */
    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        globalDeclarationsStack.push(new HashSet<String>());
        defsStack.push(node);

        node.traverse(this);

        defsStack.pop();
        globalDeclarationsStack.pop();

        checkDeclaration(node, (NameTok) node.name);
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitFunctionDef(org.python.pydev.parser.jython.ast.FunctionDef)
     */
    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        globalDeclarationsStack.push(new HashSet<String>());
        defsStack.push(node);

        if (node.args != null) {
            if (node.args.args != null) {
                for (exprType arg : node.args.args) {
                    if (arg instanceof Name) {
                        checkParam((Name) arg);
                    }
                }
            }
            if (node.args.kwonlyargs != null) {
                for (exprType arg : node.args.kwonlyargs) {
                    if (arg instanceof Name) {
                        checkParam((Name) arg);
                    }
                }
            }
        }
        node.traverse(this);

        defsStack.pop();
        globalDeclarationsStack.pop();

        checkDeclaration(node, (NameTok) node.name);
        return null;
    }

    /**
     * @param node the declaration node we're interested in (class or function)
     * @param name the token that represents the name of that declaration
     */
    private void checkParam(Name name) {
        String rep = NodeUtils.getRepresentationString(name);
        if (rep.equals(tokenToFind) && line == name.beginLine && col >= name.beginColumn
                && col <= name.beginColumn + rep.length()) {
            foundAsDefinition = true;
            // if it is found as a definition it is an 'exact' match, so, erase all the others.
            ILocalScope scope = new LocalScope(this.defsStack);
            for (Iterator<Definition> it = definitions.iterator(); it.hasNext();) {
                Definition d = it.next();
                if (!d.scope.equals(scope)) {
                    it.remove();
                }
            }

            definitionFound = new Definition(line, name.beginColumn, rep, name, scope, module.get());
            definitions.add(definitionFound);
        }
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        this.call.push(node);
        Object r = super.visitCall(node);
        this.call.pop();
        return r;
    }

    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        if (node.ctx == NameTok.KeywordName) {
            if (this.line == node.beginLine && this.call.size() > 0) {
                String rep = NodeUtils.getRepresentationString(node);

                if (PySelection.isInside(col, node.beginColumn, rep.length())) {
                    foundAsDefinition = true;
                    // if it is found as a definition it is an 'exact' match, so, erase all the others.
                    ILocalScope scope = new LocalScope(this.defsStack);
                    for (Iterator<Definition> it = definitions.iterator(); it.hasNext();) {
                        Definition d = it.next();
                        if (!d.scope.equals(scope)) {
                            it.remove();
                        }
                    }

                    definitions.clear();

                    definitionFound = new KeywordParameterDefinition(line, node.beginColumn, rep, node, scope,
                            module.get(), this.call.peek());
                    definitions.add(definitionFound);
                    throw STOP_VISITING_EXCEPTION;
                }
            }
        }
        return null;
    }

    private static final StopVisitingException STOP_VISITING_EXCEPTION = new StopVisitingException();

    /**
     * @param node the declaration node we're interested in (class or function)
     * @param name the token that represents the name of that declaration
     */
    private void checkDeclaration(SimpleNode node, NameTok name) {
        String rep = NodeUtils.getRepresentationString(node);
        if (rep.equals(tokenToFind)
                && ((line == -1 && col == -1)
                        || (line == name.beginLine && col >= name.beginColumn && col <= name.beginColumn
                                + rep.length()))) {
            foundAsDefinition = true;
            // if it is found as a definition it is an 'exact' match, so, erase all the others.
            ILocalScope scope = new LocalScope(this.defsStack);
            for (Iterator<Definition> it = definitions.iterator(); it.hasNext();) {
                Definition d = it.next();
                if (!d.scope.equals(scope)) {
                    it.remove();
                }
            }

            definitionFound = new Definition(name.beginLine, name.beginColumn, rep, node, scope, module.get());
            definitions.add(definitionFound);
        }
    }

    @Override
    public Object visitGlobal(Global node) throws Exception {
        for (NameTokType n : node.names) {
            globalDeclarationsStack.peek().add(NodeUtils.getFullRepresentationString(n));
        }
        return null;
    }

    @Override
    public Object visitModule(Module node) throws Exception {
        this.defsStack.push(node);
        return super.visitModule(node);
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitAssign(org.python.pydev.parser.jython.ast.Assign)
     */
    @Override
    public Object visitAssign(Assign node) throws Exception {
        return this.visitAssign(node, -1);
    }

    public Object visitAssign(Assign node, int unpackPos) throws Exception {
        ILocalScope scope = new LocalScope(this.defsStack);
        scope.setFoundAtASTNode(node);
        if (foundAsDefinition && !scope.equals(definitionFound.scope)) { //if it is found as a definition it is an 'exact' match, so, we do not keep checking it
            return null;
        }

        for (int i = 0; i < node.targets.length; i++) {
            exprType target = node.targets[i];
            if (target instanceof Subscript) {
                continue; //assigning to an element and not the variable itself. E.g.: mydict[1] = 10 (instead of mydict = 10)
            }

            if (target instanceof Tuple) {
                //if assign is xxx, yyy = 1, 2
                //let's separate those as different assigns and analyze one by one
                Tuple targetTuple = (Tuple) target;
                if (node.value instanceof Tuple) {
                    Tuple valueTuple = (Tuple) node.value;
                    checkTupleAssignTarget(targetTuple, valueTuple.elts, false);

                } else if (node.value instanceof org.python.pydev.parser.jython.ast.List) {
                    org.python.pydev.parser.jython.ast.List valueList = (org.python.pydev.parser.jython.ast.List) node.value;
                    checkTupleAssignTarget(targetTuple, valueList.elts, false);

                } else {
                    checkTupleAssignTarget(targetTuple, new exprType[] { node.value }, true);
                }

            } else {
                String rep = NodeUtils.getFullRepresentationString(target);

                if (tokenToFind.equals(rep)) { //note, order of equals is important (because one side may be null).
                    exprType nodeValue = node.value;
                    String value = NodeUtils.getFullRepresentationString(nodeValue);
                    if (value == null) {
                        value = "";
                    }

                    //get the line and column correspondent to the target
                    int line = NodeUtils.getLineDefinition(target);
                    int col = NodeUtils.getColDefinition(target);

                    AssignDefinition definition = new AssignDefinition(value, rep, i, node, line, col, scope,
                            module.get(), nodeValue, unpackPos);

                    //mark it as global (if it was found as global in some of the previous contexts).
                    for (Set<String> globals : globalDeclarationsStack) {
                        if (globals.contains(rep)) {
                            definition.foundAsGlobal = true;
                        }
                    }

                    definitions.add(definition);
                }
            }
        }

        return super.visitAssign(node);
    }

    @Override
    public Object visitListComp(ListComp node) throws Exception {
        exprType elt = node.elt;
        elt = fixMissingAttribute(elt);
        if (this.line == elt.beginLine) {
            ILocalScope scope = new LocalScope(this.defsStack);
            scope.setFoundAtASTNode(node);
            if (foundAsDefinition && !scope.equals(definitionFound.scope)) { //if it is found as a definition it is an 'exact' match, so, we do not keep checking it
                return super.visitListComp(node);
            }

            if (this.tokenToFind.equals(NodeUtils.getRepresentationString(elt))) {
                // Something as [a for a in [F(), C()]]
                if (node.generators != null && node.generators.length == 1) {
                    comprehensionType comprehensionType = node.generators[0];
                    if (comprehensionType instanceof Comprehension) {
                        Comprehension comprehension = (Comprehension) comprehensionType;
                        if (comprehension.iter != null) {
                            if (this.tokenToFind.equals(NodeUtils.getRepresentationString(comprehension.target))) {
                                exprType[] elts = NodeUtils.getEltsFromCompoundObject(comprehension.iter);
                                String rep = "";
                                if (elts != null && elts.length > 0) {
                                    rep = NodeUtils.getRepresentationString(elts[0]);
                                }
                                ListCompDefinition definition = new ListCompDefinition(rep, this.tokenToFind, node,
                                        line, col, scope, module.get());

                                definitions.add(definition);
                            }
                        }
                    }
                }
            } else if (elt instanceof Tuple || elt instanceof List) {
                // something as [(a, b) for (a, b) in [(F(), G()), ...]]

                exprType[] eltsFromCompoundObject = NodeUtils.getEltsFromCompoundObject(elt);
                if (eltsFromCompoundObject != null) {
                    int length = eltsFromCompoundObject.length;
                    for (int i = 0; i < length; i++) {
                        exprType eltFromCompound = fixMissingAttribute(eltsFromCompoundObject[i]);
                        if (this.tokenToFind.equals(NodeUtils.getRepresentationString(eltFromCompound))) {

                            if (node.generators != null && node.generators.length == 1) {
                                comprehensionType comprehensionType = node.generators[0];

                                if (comprehensionType instanceof Comprehension) {
                                    Comprehension comprehension = (Comprehension) comprehensionType;

                                    if (comprehension.iter != null) {
                                        exprType target = comprehension.target;

                                        if (target != null) {
                                            exprType[] targetElts = NodeUtils.getEltsFromCompoundObject(target);

                                            if (targetElts != null) {

                                                for (int j = 0; j < targetElts.length; j++) {
                                                    exprType targetElt = targetElts[j];

                                                    if (this.tokenToFind
                                                            .equals(NodeUtils.getRepresentationString(targetElt))) {
                                                        exprType[] elts = NodeUtils
                                                                .getEltsFromCompoundObject(comprehension.iter);
                                                        String rep = "";
                                                        if (elts != null && elts.length > j) {
                                                            rep = NodeUtils.getRepresentationString(elts[j]);
                                                        }
                                                        ListCompDefinition definition = new ListCompDefinition(rep,
                                                                this.tokenToFind, node,
                                                                line, col, scope, module.get());

                                                        definitions.add(definition);
                                                    }

                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return super.visitListComp(node);
    }

    private exprType fixMissingAttribute(exprType elt) {
        if (elt instanceof Attribute) {
            // I.e.: in this case we have: Attribute[value=Name[id=i, ctx=Load, reserved=false], attr=NameTok[id=!<MissingName>!, ctx=Attrib], ctx=Load]
            Attribute attribute = (Attribute) elt;
            String rep = NodeUtils.getRepresentationString(attribute.attr);
            if (rep == null || rep.startsWith("!")) {
                elt = attribute.value;
            }
        }
        return elt;
    }

    /**
     * Analyze an assign that has the target as a tuple and the multiple elements in the other side.
     *
     * E.g.: www, yyy = 1, 2
     *
     * @param targetTuple the target in the assign
     * @param valueElts the values that are being assigned
     * @param unpackElements
     */
    private void checkTupleAssignTarget(Tuple targetTuple, exprType[] valueElts, boolean unpackElements)
            throws Exception {
        if (valueElts == null || valueElts.length == 0) {
            return; //nothing to do if we don't have any values
        }

        for (int i = 0; i < targetTuple.elts.length; i++) {
            int j = i;
            //that's if the number of values is less than the number of assigns (actually, that'd
            //probably be an error, but let's go on gracefully, as the user can be in an invalid moment
            //in his code)
            if (j >= valueElts.length) {
                j = valueElts.length - 1;
            }
            Assign assign = new Assign(new exprType[] { targetTuple.elts[i] }, valueElts[j]);
            assign.beginLine = targetTuple.beginLine;
            assign.beginColumn = targetTuple.beginColumn;
            if (unpackElements) {
                visitAssign(assign, i);

            } else {
                visitAssign(assign);

            }
        }
    }
}
