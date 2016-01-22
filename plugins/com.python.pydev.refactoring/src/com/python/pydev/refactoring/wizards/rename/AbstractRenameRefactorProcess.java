/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.scopeanalysis.AstEntryScopeAnalysisConstants;
import com.python.pydev.analysis.scopeanalysis.ScopeAnalyzerVisitor;
import com.python.pydev.refactoring.refactorer.RefactorerFindReferences;
import com.python.pydev.refactoring.wizards.IRefactorRenameProcess;

/**
 * This class presents the basic functionality for doing a rename.
 *
 * @author Fabio
 */
public abstract class AbstractRenameRefactorProcess implements IRefactorRenameProcess {

    /**
     * The request for the refactor
     */
    protected RefactoringRequest request;

    /**
     * For a rename, we always need a definition
     */
    protected Definition definition;

    /**
     * This is the list that contains only the occurrences for the current document,
     * passed by the request.
     */
    protected HashSet<ASTEntry> docOccurrences = new HashSet<ASTEntry>();

    /**
     * This map contains:
     * key: tuple with module name and the IFile representing that module
     * value: list of ast entries to be replaced in a given file
     */
    protected Map<Tuple<String, File>, HashSet<ASTEntry>> fileOccurrences = new HashMap<Tuple<String, File>, HashSet<ASTEntry>>();

    @Override
    public void clear() {
        fileOccurrences.clear();
        docOccurrences.clear();
    }

    /**
     * May be used by subclasses
     */
    public AbstractRenameRefactorProcess() {

    }

    /**
     * @param definition the definition on where this rename should be applied (we will find the references based
     * on this definition).
     */
    public AbstractRenameRefactorProcess(Definition definition) {
        this.definition = definition;
    }

    /**
     * Adds the occurences to be renamed given the request. If the rename is a local rename, and there is no need
     * of handling multiple files, this should be the preferred way of adding the occurrences.
     *
     * @param request will be used to fill the module name and the document
     * @param oc the occurrences to add
     */
    protected void addOccurrences(RefactoringRequest request, List<ASTEntry> oc) {
        docOccurrences.addAll(oc);
    }

    /**
     * Adds the occurrences found to some module.
     *
     * @param oc the occurrences found
     * @param file the file where the occurrences were found
     * @param modName the name of the module that is bounded to the given file.
     */
    protected void addOccurrences(List<ASTEntry> oc, File file, String modName) {
        Tuple<String, File> key = new Tuple<String, File>(modName, file);
        Set<ASTEntry> existent = fileOccurrences.get(key);
        if (existent == null) {
            fileOccurrences.put(key, new HashSet<ASTEntry>(oc));
        } else {
            existent.addAll(oc);
        }
    }

    public static int getOffset(IDocument doc, ASTEntry entry) {
        int beginLine;
        int beginCol;
        SimpleNode node = entry.node;
        if (node instanceof ClassDef) {
            ClassDef def = (ClassDef) node;
            node = def.name;
        }
        if (node instanceof FunctionDef) {
            FunctionDef def = (FunctionDef) node;
            node = def.name;
        }
        if (node instanceof Attribute) {
            exprType value = ((Attribute) node).value;
            if (value instanceof Call) {
                Call c = (Call) value;
                node = c.func;
            }
        }
        beginLine = node.beginLine;
        beginCol = node.beginColumn;
        int offset = PySelection.getAbsoluteCursorOffset(doc, beginLine - 1, beginCol - 1);
        return offset;
    }

    /**
     * This method is used to sort the occurrences given the place where they were found
     */
    public static List<ASTEntry> sortOccurrences(List<ASTEntry> occurrences) {
        occurrences = new ArrayList<ASTEntry>(occurrences);

        Collections.sort(occurrences, new Comparator<ASTEntry>() {

            public int compare(ASTEntry o1, ASTEntry o2) {
                int o1Found = (Integer) o1
                        .getAdditionalInfo(AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_LOCATION, 0);
                int o2Found = (Integer) o2
                        .getAdditionalInfo(AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_LOCATION, 0);
                if (o1Found == o2Found) {
                    return 0;
                }
                if (o1Found < o2Found) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        return occurrences;
    }

    /**
     * This function is used to redirect where the initial should should target
     * (in the local or workspace scope).
     */
    public void findReferencesToRename(RefactoringRequest request, RefactoringStatus status) {
        this.request = request;

        if ((Boolean) request.getAdditionalInfo(RefactoringRequest.FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE,
                false)) {
            findReferencesToRenameOnLocalScope(request, status);

        } else {
            findReferencesToRenameOnWorkspace(request, status);
        }

        if (!occurrencesValid(status)) {
            return;
        }

    }

    /**
     * This function should be overridden to find the occurrences in the local scope
     * (and check if they are correct).
     *
     * @param status object where the status can be set (to add errors/warnings)
     * @param request the request used for this check
     */
    protected abstract void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status);

    /**
     * This function should be overridden to find the occurrences in the workspace scope
     * (and check if they are correct).
     *
     * @param status object where the status can be set (to add errors/warnings)
     * @param request the request used for this check
     */
    protected abstract void findReferencesToRenameOnWorkspace(RefactoringRequest request, RefactoringStatus status);

    /**
     * Checks if the occurrences gotten are valid or not.
     *
     * @param status the errors will be added to the passed status.
     * @return true if all is ok and false otherwise
     */
    protected boolean occurrencesValid(RefactoringStatus status) {
        if (docOccurrences.size() == 0 && !(request.isModuleRenameRefactoringRequest())) {
            status.addFatalError("No occurrences found for:" + request.initialName);
            return false;
        }
        return true;
    }

    /**
     * Implemented from the super interface. Should return the occurrences from the current document
     *
     * @see com.python.pydev.refactoring.wizards.IRefactorRenameProcess#getOccurrences()
     */
    public HashSet<ASTEntry> getOccurrences() {
        return docOccurrences;
    }

    /**
     * Implemented from the super interface. Should return the occurrences found in other documents
     * (but should not return the ones found in the current document)
     *
     * @see com.python.pydev.refactoring.wizards.IRefactorRenameProcess#getOccurrencesInOtherFiles()
     */
    public Map<Tuple<String, File>, HashSet<ASTEntry>> getOccurrencesInOtherFiles() {
        return this.fileOccurrences;
    }

    /**
     * Searches for a list of entries that are found within a scope.
     *
     * It is always based on a single scope and bases itself on a refactoring request.
     */
    protected List<ASTEntry> getOccurrencesWithScopeAnalyzer(RefactoringRequest request, SourceModule module) {
        List<ASTEntry> entryOccurrences = new ArrayList<ASTEntry>();

        try {
            ScopeAnalyzerVisitor visitor;
            if (!request.ps.getCurrToken().o1.equals(request.initialName)) {
                //i.e.: it seems it wasn't started from the editor, so, we need to search using the
                //initial name and not the current selection
                PySelection ps = request.ps;
                visitor = new ScopeAnalyzerVisitor(request.nature, module.getName(), module,
                        ps.getDoc(),
                        new NullProgressMonitor(),
                        request.initialName,
                        -1,
                        ActivationTokenAndQual.splitActAndQualifier(request.initialName));
            } else {

                visitor = new ScopeAnalyzerVisitor(request.nature, module.getName(), module,
                        new NullProgressMonitor(), request.ps);
            }

            module.getAst().accept(visitor);
            entryOccurrences = visitor.getEntryOccurrences();
        } catch (BadLocationException e) {
            //don't log
        } catch (Exception e) {
            Log.log(e);
        }
        return entryOccurrences;
    }

    /**
     * This functions tries to find the modules that may have matches for a given request.
     *
     * Note that it may return files that don't actually contain what we're looking for.
     *
     * @param request the request for a rename.
     * @return a list with the files that may contain matches for the refactoring.
     */
    protected List<Tuple<List<ModulesKey>, IPythonNature>> findFilesWithPossibleReferences(
            RefactoringRequest request) throws OperationCanceledException {
        return new RefactorerFindReferences().findPossibleReferences(request);
    }

}
