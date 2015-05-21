/*******************************************************************************
 * Copyright (c) 2014 Google, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Danny Yoo (Google) - refactored parts of PyRenameEntryPoint into here.
 *******************************************************************************/

package com.python.pydev.refactoring.wizards.rename;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.core.IModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.editor.refactoring.TooManyMatchesException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Location;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.refactoring.wizards.IRefactorRenameProcess;
import com.python.pydev.refactoring.wizards.RefactorProcessFactory;

/**
 * Searches references to identifiers for operations such as {@code MarkOccurrences} and
 * {@link PyRenameEntryPoint}.
 * 
 * <p>Clients are expected to call {@link #prepareSearch} to collect the processes used to search
 * for a given request.  Once this succeeds, clients call {@link search} to search for references.
 * Finally, clients use {@link getLocalReferences} and {@link getWorkspaceReferences} to query for
 * references.
 */
public class PyReferenceSearcher {
    // This code used to be centralized in PyRenameEntryPoint, but the same logic was used
    // in MarkOccurrences too.  The code paths share very similar logic, so are collected here.

    private final Map<RefactoringRequest, List<IRefactorRenameProcess>> requestToProcesses = new HashMap<>();

    private static final String INVALID_DEFINITION = "The definition found is not valid: ";

    /**
     * Reports exceptions during a search.
     */
    @SuppressWarnings("serial")
    public static class SearchException extends Exception {
        /**
         * Constructs an exception to report problems during search.
         *
         * @param message describes details about the exception.
         */
        public SearchException(String message) {
            super(message);
        }
    }

    /**
     * Constructs a searcher for the given requests.
     * 
     * @param requests the search requests.
     */
    public PyReferenceSearcher(RefactoringRequest... requests) {
        for (RefactoringRequest refactoringRequest : requests) {
            requestToProcesses.put(refactoringRequest, new ArrayList<IRefactorRenameProcess>());
        }
    }

    /**
     * Prepares for an upcoming use of {@link #search(RefactoringRequest)}.  This must be called
     * before a search is performed.
     *
     * @param request the search request.
     * @throws SearchException if the AST can not be found or the definition for the
     *     identifier isn't valid or can't otherwise be searched.
     * @throws BadLocationException 
     * @throws TooManyMatchesException 
     */
    public void prepareSearch(RefactoringRequest request)
            throws SearchException, TooManyMatchesException, BadLocationException {
        List<IRefactorRenameProcess> processes = requestToProcesses.get(request);
        processes.clear(); // Clear the existing processes for the request
        ItemPointer[] pointers;
        if (request.isModuleRenameRefactoringRequest()) {
            IModule module = request.getModule();
            pointers = new ItemPointer[] {
                    new ItemPointer(request.file, new Location(0, 0), new Location(0, 0),
                            new Definition(1, 1, "", null, null, module, false), null) };
        } else {
            SimpleNode ast = request.getAST();
            if (ast == null) {
                throw new SearchException("AST not generated (syntax error).");
            }
            IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
            request.communicateWork("Finding definition");
            pointers = pyRefactoring.findDefinition(request);
        }
        if (pointers.length == 0) {
            // no definition found
            IRefactorRenameProcess p = RefactorProcessFactory.getRenameAnyProcess();
            processes.add(p);
        } else {
            for (ItemPointer pointer : pointers) {
                if (pointer.definition == null) {
                    throw new SearchException(INVALID_DEFINITION + pointer);
                }
                IRefactorRenameProcess p = RefactorProcessFactory.getProcess(pointer.definition, request);
                if (p == null) {
                    throw new SearchException(INVALID_DEFINITION + pointer.definition);
                }
                processes.add(p);
            }
        }

        if (processes.isEmpty()) {
            throw new SearchException("The pre-conditions were not satisfied.");
        }
    }

    /**
     * Searches for references to the identifier in the request.
     *
     * <p>{@link #prepareSearch(RefactoringRequest)} must be called before
     * {@link #search(RefactoringRequest)} or else results are undefined.
     *
     * @param request the search request
     * @throws SearchException if an exception occurs in the process of finding references.
     * @throws OperationCanceledException if the request is canceled.
     */
    public void search(RefactoringRequest request)
            throws SearchException, OperationCanceledException {
        for (IRefactorRenameProcess p : requestToProcesses.get(request)) {
            request.checkCancelled();
            p.clear(); // Clear references found from a previous invocation
            RefactoringStatus status = new RefactoringStatus();
            request.pushMonitor(new SubProgressMonitor(request.getMonitor(), 1));
            try {
                p.findReferencesToRename(request, status);
            } finally {
                request.popMonitor().done();
            }
            if (status.hasFatalError()) {
                throw new SearchException(status.getEntryWithHighestSeverity().getMessage());
            }
        }
    }

    /**
     * Returns the individual search processes used for a request.
     *
     * <p>{@link #prepareSearch(RefactoringRequest)} or {@link #search(RefactoringRequest)} must be
     * called before {@link #getProcesses(RefactoringRequest)}, or else results are undefined.
     *
     * @param request the search request
     * @return the list of processes that are used for the request.
     */
    public List<IRefactorRenameProcess> getProcesses(RefactoringRequest request) {
        return requestToProcesses.get(request);
    }

    /**
     * Returns the set of references found locally.
     *
     * <p>{@link #prepareSearch(RefactoringRequest)} and {@link #search(RefactoringRequest)} must be
     * called before {@link #getProcesses(RefactoringRequest)}, or else results are undefined.
     *
     * @param request the search request
     * @return the set of references that are found in the current document.
     *     Does not get the references from other files
     */
    public HashSet<ASTEntry> getLocalReferences(RefactoringRequest request) {
        HashSet<ASTEntry> allReferences = new HashSet<>();
        for (IRefactorRenameProcess p : requestToProcesses.get(request)) {
            HashSet<ASTEntry> references = p.getOccurrences();
            if (references != null) {
                allReferences.addAll(references);
            }
        }
        return allReferences;
    }

    /**
     * Returns the set of references found in the workspace.
     *
     * <p>{@link #prepareSearch(RefactoringRequest)} and {@link #search(RefactoringRequest)} must be
     * called before {@link #getProcesses(RefactoringRequest)}, or else results are undefined.
     *
     * @param request the search request
     * @return a map that points the references found in other files, but excludes those found locally.
     */
    public Map<Tuple<String, File>, HashSet<ASTEntry>> getWorkspaceReferences(
            RefactoringRequest request) {
        HashMap<Tuple<String, File>, HashSet<ASTEntry>> allReferences = new HashMap<>();
        for (IRefactorRenameProcess p : requestToProcesses.get(request)) {
            Map<Tuple<String, File>, HashSet<ASTEntry>> references = p.getOccurrencesInOtherFiles();
            if (references != null) {
                for (Map.Entry<Tuple<String, File>, HashSet<ASTEntry>> reference : references.entrySet()) {
                    Tuple<String, File> key = reference.getKey();
                    HashSet<ASTEntry> existingReferences = allReferences.get(key);
                    if (existingReferences == null) {
                        existingReferences = new HashSet<>();
                        allReferences.put(key, existingReferences);
                    }
                    existingReferences.addAll(reference.getValue());
                }
            }
        }
        return allReferences;
    }
}
