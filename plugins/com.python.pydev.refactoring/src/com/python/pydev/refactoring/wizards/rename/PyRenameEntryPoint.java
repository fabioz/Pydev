/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.actions.PyFindAllOccurrences;
import com.python.pydev.refactoring.wizards.IRefactorRenameProcess;
import com.python.pydev.refactoring.wizards.RefactorProcessFactory;

/**
 * Rename to a local variable...
 * 
 * Straightforward 'way': - find the definition and assert it is not a global - rename all occurences within that scope
 * 
 * 'Blurred things': - if we have something as:
 * 
 * case 1: 
 * 
 * def m1(): 
 *     a = 1 
 *     def m2(): 
 *         a = 3 
 *         print a 
 *     print a
 * 
 * case 2: 
 * 
 * def m1(): 
 *     a = 1 
 *     def m2(): 
 *         print a 
 *         a = 3 
 *         print a 
 *     print a
 * 
 * case 3: 
 * 
 * def m1(): 
 *     a = 1 
 *     def m2(): 
 *         if foo: 
 *             a = 3 
 *         print a 
 *     print a
 * 
 * if we rename it inside of m2, do we have to rename it in scope m1 too? what about renaming it in m1?
 * 
 * The solution that will be implemented will be:
 * 
 *  - if we rename it inside of m2, it will only rename inside of its scope in any case 
 *  (the problem is when the rename request commes from an 'upper' scope).
 *  
 *  - if we rename it inside of m1, it will rename it in m1 and m2 only if it is used inside 
 *  that scope before an assign this means that it will rename in m2 in case 2 and 3, but not in case 1.
 */
public class PyRenameEntryPoint extends RenameProcessor {

    public static final Set<String> WORDS_THAT_CANNOT_BE_RENAMED = new HashSet<String>();
    static {
        String[] wordsThatCannotbeRenamed = { "and", "assert", "break", "class", "continue", "def", "del", "elif", "else", "except",
                "exec", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "not", "or", "pass", "print", "raise",
                "return", "try", "while", "with", "yield", "as" };
        for (String string : wordsThatCannotbeRenamed) {
            WORDS_THAT_CANNOT_BE_RENAMED.add(string);
        }

    }

    /**
     * This is the request that triggered this processor
     */
    private RefactoringRequest request;

    /**
     * The change object as required by the Eclipse Language Toolkit
     */
    private CompositeChange fChange;

    /**
     * A list of processes that were activated for doing the rename
     */
    public List<IRefactorRenameProcess> process;

    public PyRenameEntryPoint(RefactoringRequest request) {
        this.request = request;
    }

    @Override
    public Object[] getElements() {
        return new Object[] { this.request };
    }

    public static final String IDENTIFIER = "org.python.pydev.pyRename";

    public static final boolean DEBUG = false || PyFindAllOccurrences.DEBUG_FIND_REFERENCES;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getProcessorName() {
        return "PyDev PyRenameProcessor";
    }

    @Override
    public boolean isApplicable() throws CoreException {
        return true;
    }

    /**
     * In this method we have to check the conditions for doing the refactorings
     * and finding the definition / references that will be affected in the
     * refactoring.
     * 
     * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        request.pushMonitor(pm);
        request.getMonitor().beginTask("Checking refactoring pre-conditions...", 100);
        
        RefactoringStatus status = new RefactoringStatus();
        try {
            if (!StringUtils.isWord(request.initialName)) {
                status.addFatalError("The initial name is not valid:" + request.initialName);
                return status;
            }
            
            if (WORDS_THAT_CANNOT_BE_RENAMED.contains(request.initialName)) {
                status.addFatalError("The token: " + request.initialName+ " cannot be renamed.");
                return status;
            }

            if (request.inputName != null && !StringUtils.isWord(request.inputName)) {
                status.addFatalError("The new name is not valid:" + request.inputName);
                return status;
            }

            SimpleNode ast = request.getAST();
            if (ast == null) {
                status.addFatalError("AST not generated (syntax error).");
                return status;
            }
            IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
            request.communicateWork("Finding definition");
            ItemPointer[] pointers = pyRefactoring.findDefinition(request);
            
            process = new ArrayList<IRefactorRenameProcess>();

            if (pointers.length == 0) {
                // no definition found
                IRefactorRenameProcess p = RefactorProcessFactory.getRenameAnyProcess();
                process.add(p);

            } else {
                for (ItemPointer pointer : pointers) {
                    if (pointer.definition == null) {
                        status.addFatalError("The definition found is not valid. " + pointer);
                    }
                    if (DEBUG) {
                        System.out.println("Found definition:" + pointer.definition);
                    }

                    IRefactorRenameProcess p = RefactorProcessFactory.getProcess(pointer.definition, request);
                    if (p == null) {
                        status.addFatalError("Refactoring Process not defined: the definition found is not valid:" + pointer.definition);
                        return status;
                    }
                    process.add(p);
                }
            }

            if (process == null || process.size() == 0) {
                status.addFatalError("Refactoring Process not defined: the pre-conditions were not satisfied.");
                return status;
            }

        } catch (OperationCanceledException e) {
            // OK
        } finally {
            request.popMonitor().done();
        }
        return status;
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException,
            OperationCanceledException {
        return checkFinalConditions(pm, context, true);
    }

    /**
     * Find the references and create the change object
     * 
     * @param fillChangeObject
     *            determines if we should fill the change object (we'll not do
     *            it on tests)
     */
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, boolean fillChangeObject)
            throws CoreException, OperationCanceledException {
        request.pushMonitor(pm);
        RefactoringStatus status = new RefactoringStatus();
        try {
            if (process == null || process.size() == 0) {
                request.getMonitor().beginTask("Finding references", 1);
                status.addFatalError("Refactoring Process not defined: the refactoring cycle did not complet correctly.");
                return status;
            }
            request.getMonitor().beginTask("Finding references", process.size());
            
            fChange = new CompositeChange("RenameChange: '" + request.initialName+ "' to '"+request.inputName+"'");

            //Finding references and creating change object...
            //now, check the initial and final conditions
            for (IRefactorRenameProcess p : process) {
                request.checkCancelled();
                
                request.pushMonitor(new SubProgressMonitor(request.getMonitor(), 1));
                try {
                    p.findReferencesToRename(request, status);
                } finally {
                    request.popMonitor().done();
                }
                
                if (status.hasFatalError() || request.getMonitor().isCanceled()) {
                    return status;
                }
            }
            if (fillChangeObject) {
                TextEditCreation textEditCreation = 
                    new TextEditCreation(request.initialName, request.inputName, request.getModule().getName(), 
                            request.getDoc(), process, status, fChange, request.getIFile());
                
                textEditCreation.fillRefactoringChangeObject(request, context);
                 if (status.hasFatalError() || request.getMonitor().isCanceled()) {
                     return status;
                 }
                
            }
        } catch (OperationCanceledException e) {
            // OK
        } finally {
            request.popMonitor().done();
        }
        return status;
    }

    /**
     * Change is actually already created in this stage.
     */
    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        return fChange;
    }

    static RefactoringParticipant[] EMPTY_REFACTORING_PARTICIPANTS = new RefactoringParticipant[0];

    @Override
    public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants)
            throws CoreException {
        return EMPTY_REFACTORING_PARTICIPANTS; // no participants are loaded
    }

    /**
     * @return the list of occurrences that are found in the current document.
     *         Does not get the occurrences if they are in other files
     */
    public HashSet<ASTEntry> getOccurrences() {
        if (process == null || process.size() == 0) {
            return null;
        }
        HashSet<ASTEntry> occurrences = new HashSet<ASTEntry>();
        for (IRefactorRenameProcess p : process) {
            HashSet<ASTEntry> o = p.getOccurrences();
            if (o != null) {
                occurrences.addAll(o);
            }
        }
        return occurrences;
    }

    /**
     * @return a map that points the references found in other files Note that
     *         this will exclude the references found in this buffer.
     */
    public Map<Tuple<String, File>, HashSet<ASTEntry>> getOccurrencesInOtherFiles() {
        HashMap<Tuple<String, File>, HashSet<ASTEntry>> m = new HashMap<Tuple<String, File>, HashSet<ASTEntry>>();
        if (process == null || process.size() == 0) {
            return null;
        }

        for (IRefactorRenameProcess p : process) {
            Map<Tuple<String, File>, HashSet<ASTEntry>> o = p.getOccurrencesInOtherFiles();
            if (o != null) {

                for (Map.Entry<Tuple<String, File>, HashSet<ASTEntry>> entry : o.entrySet()) {
                    Tuple<String, File> key = entry.getKey();

                    HashSet<ASTEntry> existingOccurrences = m.get(key);
                    if (existingOccurrences == null) {
                        existingOccurrences = new HashSet<ASTEntry>();
                        m.put(key, existingOccurrences);
                    }

                    existingOccurrences.addAll(entry.getValue());
                }
            }
        }
        return m;
    }

}
