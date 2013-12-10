/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 1, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

/**
 * This process takes care of renaming instances of some method either:
 * 
 * - on a class
 * - on a global scope
 * - in an inner scope (inside of another method)
 */
public class PyRenameFunctionProcess extends AbstractRenameWorkspaceRefactorProcess {

    /**
     * This is a cache to improve the lookup if it is requested more times 
     */
    private ASTEntry functionDefEntryCache;

    /**
     * To be used by subclasses
     */
    protected PyRenameFunctionProcess() {

    }

    public PyRenameFunctionProcess(Definition definition) {
        super(definition);
        Assert.isTrue(this.definition.ast instanceof FunctionDef);
    }

    @Override
    protected boolean getRecheckWhereDefinitionWasFound() {
        return true;
    }

    /**
     * This method is the method that should be used to get the occurrences in the same
     * module where the function is defined.
     * 
     * @param occurencesFor the name of the function we're looking for
     * @param simpleNode the root of the module
     * @param status if we're unable to find the reference for the function definition in this module,
     * an error is added to this status.
     * 
     * @return a list with the entries with the references (and definition) to the function searched.
     */
    private List<ASTEntry> getLocalOccurrences(String occurencesFor, SimpleNode simpleNode, RefactoringStatus status) {
        List<ASTEntry> ret = new ArrayList<ASTEntry>();

        //get the entry for the function itself
        ASTEntry functionDefEntry = getOriginalFunctionInAst(simpleNode);

        if (functionDefEntry == null) {
            status.addFatalError("Unable to find the original definition for the function definition.");
            return ret;
        }

        if (functionDefEntry.parent != null) {
            //it has some parent

            final SimpleNode parentNode = functionDefEntry.parent.node;
            if (parentNode instanceof ClassDef) {

                //ok, we're in a class, the first thing is to add the reference to the function just gotten
                ret.add(new ASTEntry(functionDefEntry, ((FunctionDef) functionDefEntry.node).name));

                //get the entry for the self.xxx that access that attribute in the class
                SequencialASTIteratorVisitor classVisitor = SequencialASTIteratorVisitor.create(parentNode);
                Iterator<ASTEntry> it = classVisitor.getIterator(Attribute.class);
                while (it.hasNext()) {
                    ASTEntry entry = it.next();
                    List<SimpleNode> parts = NodeUtils.getAttributeParts((Attribute) entry.node);
                    if (!(parts.get(1) instanceof Attribute)) {
                        final String rep0 = NodeUtils.getRepresentationString(parts.get(0));
                        final String rep1 = NodeUtils.getRepresentationString(parts.get(1));
                        if (rep0 != null && rep1 != null && rep0.equals("self") && rep1.equals(occurencesFor)) {
                            ret.add(entry);
                        }
                    }
                }

                final List<ASTEntry> attributeReferences = ScopeAnalysis.getAttributeReferences(occurencesFor,
                        simpleNode);
                NameTokType funcName = ((FunctionDef) functionDefEntry.node).name;
                for (ASTEntry entry : attributeReferences) {
                    if (entry.node != funcName) {
                        if (entry.node instanceof NameTok) {
                            NameTok nameTok = (NameTok) entry.node;
                            if (nameTok.ctx == NameTok.ClassName) {
                                continue;
                            }
                        }
                        ret.add(entry);
                    }
                }

            } else if (parentNode instanceof FunctionDef) {
                //get the references inside of the parent (this will include the function itself)
                ret.addAll(ScopeAnalysis.getLocalOccurrences(occurencesFor, parentNode));
            }

        } else {
            ret.addAll(ScopeAnalysis.getLocalOccurrences(occurencesFor, simpleNode));
        }

        if (ret.size() > 0) {
            //only add comments and strings if there's at least some other occurrence
            ret.addAll(ScopeAnalysis.getCommentOccurrences(occurencesFor, simpleNode));
            ret.addAll(ScopeAnalysis.getStringOccurrences(occurencesFor, simpleNode));
        }
        //get the references to Names that access that method in the same scope
        return ret;
    }

    /**
     * @param simpleNode this is the module with the AST that has the function definition
     * @return the function definition that matches the original definition as an ASTEntry
     */
    private ASTEntry getOriginalFunctionInAst(SimpleNode simpleNode) {
        if (functionDefEntryCache == null) {
            SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(simpleNode);
            Iterator<ASTEntry> it = visitor.getIterator(FunctionDef.class);
            ASTEntry functionDefEntry = null;
            while (it.hasNext()) {
                functionDefEntry = it.next();

                if (functionDefEntry.node.beginLine == this.definition.ast.beginLine
                        && functionDefEntry.node.beginColumn == this.definition.ast.beginColumn) {
                    functionDefEntryCache = functionDefEntry;
                    break;
                }
            }
        }
        return functionDefEntryCache;
    }

    /**
     * Checks the local scope for references.
     */
    @Override
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        SimpleNode root = request.getAST();

        if (!definition.module.getName().equals(request.moduleName)) {
            //it was found in another module
            docOccurrences.addAll(getEntryOccurrencesInOtherModule(request.initialName, root));

        } else {
            docOccurrences.addAll(getEntryOccurrencesInSameModule(status, request.initialName, root));
        }

    }

    /**
     * Will return the occurrences if we're in the same module for the method definition
     */
    protected List<ASTEntry> getEntryOccurrencesInSameModule(RefactoringStatus status, String initialName,
            SimpleNode root) {
        return getLocalOccurrences(initialName, root, status);
    }

    /**
     * Will return the occurrences if we're NOT in the same module as the method definition
     */
    protected List<ASTEntry> getEntryOccurrencesInOtherModule(String initialName, SimpleNode root) {
        List<ASTEntry> ret = ScopeAnalysis.getLocalOccurrences(initialName, root, false);
        if (ret.size() > 0) {
            //only add comments and strings if there's at least some other occurrence
            ret.addAll(ScopeAnalysis.getCommentOccurrences(initialName, root));
            ret.addAll(ScopeAnalysis.getStringOccurrences(initialName, root));
        }
        return ret;
    }

    /**
     * This method is called for each module that may have some reference to the definition
     * we're looking for.
     * (Abstract in superclass) 
     */
    @Override
    protected List<ASTEntry> findReferencesOnOtherModule(RefactoringStatus status, RefactoringRequest request,
            String initialName, SourceModule module) {
        SimpleNode root = module.getAst();

        //note that the definition may be found in a module that is not actually the 'current' module
        if (!definition.module.getName().equals(module.getName())) {
            return getEntryOccurrencesInOtherModule(initialName, root);
        } else {
            return getLocalOccurrences(initialName, root, status);
        }
    }

}
