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
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;
import com.python.pydev.refactoring.wizards.RefactorProcessFactory;

/**
 * This is the process that should take place when the definition maps to a class
 * definition (its AST is a ClassDef)
 * 
 * @see RefactorProcessFactory#getProcess(Definition) for details on choosing the 
 * appropriate process.
 * 
 * Note that the definition found may map to some module that is not actually
 * the current module, meaning that we may have a RenameClassProcess even
 * if the class definition is on some other module.
 * 
 * Important: the assumptions that can be made given this are:
 * - The current module has the token that maps to the definition found (so, it
 * doesn't need to be double checked)
 * - The module where the definition was found also does not need double checking
 * 
 * - All other modules need double checking if there is some other token in the 
 * workspace with the same name.
 * 
 * @author Fabio
 */
public class PyRenameClassProcess extends AbstractRenameWorkspaceRefactorProcess {

    /**
     * Do we want to debug?
     */
    public static final boolean DEBUG_CLASS_PROCESS = false;

    /**
     * Creates the rename class process with a definition.
     * 
     * @param definition a definition with a ClassDef.
     */
    public PyRenameClassProcess(Definition definition) {
        super(definition);
        Assert.isTrue(this.definition.ast instanceof ClassDef);
    }

    /**
     * When checking the class on a local scope, we have to cover the class definition
     * itself and any access to it (global)
     */
    @Override
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        SimpleNode root = request.getAST();
        List<ASTEntry> oc = new ArrayList<ASTEntry>();

        //in the local scope for a class, we'll only have at least one reference
        oc.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, root));
        oc.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, root));
        int currLine = request.ps.getCursorLine();
        int currCol = request.ps.getCursorColumn();
        int tokenLen = request.initialName.length();
        boolean foundAsComment = false;
        for (ASTEntry entry : oc) {
            //it may be that we are actually hitting it in a comment and not in the class itself...
            //(for a comment it is ok just to check the line)
            int startLine = entry.node.beginLine - 1;
            int startCol = entry.node.beginColumn - 1;
            int endCol = entry.node.beginColumn + tokenLen - 1;
            if (currLine == startLine && currCol >= startCol && currCol <= endCol) {
                foundAsComment = true;
                break;
            }
        }

        ASTEntry classDefInAst = null;
        if (!foundAsComment && (request.moduleName == null || request.moduleName.equals(definition.module.getName()))) {
            classDefInAst = getOriginalClassDefInAst(root);

            if (classDefInAst == null) {
                status.addFatalError("Unable to find the original definition for the class definition.");
                return;
            }

            while (classDefInAst.parent != null) {
                if (classDefInAst.parent.node instanceof FunctionDef) {
                    request.setAdditionalInfo(RefactoringRequest.FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE,
                            true); //it is in a local scope.
                    oc.addAll(this.getOccurrencesWithScopeAnalyzer(request, (SourceModule) request.getModule()));
                    addOccurrences(request, oc);
                    return;
                }
                classDefInAst = classDefInAst.parent;
            }

            //it is defined in the module we're looking for
            oc.addAll(this.getOccurrencesWithScopeAnalyzer(request, (SourceModule) request.getModule()));
        } else {
            //it is defined in some other module (or as a comment... so, we won't have an exact match in the position)
            oc.addAll(ScopeAnalysis.getLocalOccurrences(request.initialName, root));
        }

        if (classDefInAst == null) {
            //only get attribute references if the class defitinion was not found in this module
            // -- which means that it was found as an import. E.g.:
            // import foo
            // foo.ClassAccess <-- Searching for ClassAccess
            List<ASTEntry> attributeReferences = ScopeAnalysis.getAttributeReferences(request.initialName, root, 0);
            oc.addAll(attributeReferences);
        }

        addOccurrences(request, oc);
    }

    /**
     * @param simpleNode this is the module with the AST that has the function definition
     * @return the function definition that matches the original definition as an ASTEntry
     */
    private ASTEntry getOriginalClassDefInAst(SimpleNode simpleNode) {
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(simpleNode);
        Iterator<ASTEntry> it = visitor.getIterator(ClassDef.class);
        ASTEntry classDefEntry = null;
        while (it.hasNext()) {
            classDefEntry = it.next();

            if (classDefEntry.node.beginLine == this.definition.ast.beginLine
                    && classDefEntry.node.beginColumn == this.definition.ast.beginColumn) {
                return classDefEntry;
            }
        }
        return null;
    }

    /**
     * This method is called for each module that may have some reference to the definition
     * we're looking for. 
     */
    @Override
    protected List<ASTEntry> findReferencesOnOtherModule(RefactoringStatus status, RefactoringRequest request,
            String initialName, SourceModule module) {
        SimpleNode root = module.getAst();

        List<ASTEntry> entryOccurrences = ScopeAnalysis.getLocalOccurrences(initialName, root);
        entryOccurrences.addAll(ScopeAnalysis.getAttributeReferences(initialName, root));

        if (entryOccurrences.size() > 0) {
            //only add comments and strings if there's at least some other occurrence
            entryOccurrences.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, root));
            entryOccurrences.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, root));
        }
        return entryOccurrences;
    }

    @Override
    protected boolean getRecheckWhereDefinitionWasFound() {
        return true;
    }

}
