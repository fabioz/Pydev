/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.wizards.rename;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.editor.codecompletion.revisited.modules.ASTEntryWithSourceModule;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.editorinput.PySourceLocatorBase;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.refactoring.core.base.PyDocumentChange;
import org.python.pydev.refactoring.core.base.PyTextFileChange;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.scopeanalysis.AstEntryScopeAnalysisConstants;
import com.python.pydev.refactoring.changes.PyRenameResourceChange;
import com.python.pydev.refactoring.wizards.IRefactorRenameProcess;

/**
 * Note that this class should only be used once and then should be thrown away.
 * 
 * It should be used to get AstEntries and transform them into TextEdits (filled into a Change object
 * as required by the refactoring structure).
 * 
 * @author Fabio
 */
public class TextEditCreation {

    /**
     * New name for the variable renamed
     */
    private String inputName;

    /**
     * Initial name of renamed variable
     */
    private String initialName;

    /**
     * Name of the module where the rename was requested
     */
    private String moduleName;

    /**
     * Document where the rename was requested
     */
    private IDocument currentDoc;

    /**
     * List of processes that will be a part of the refactoring
     */
    private List<IRefactorRenameProcess> processes;

    /**
     * Change object with all the changes that will be done in the rename
     */
    private CompositeChange fChange;

    /**
     * Status of the refactoring. Should be updated to contain errors.
     */
    private RefactoringStatus status;

    /**
     * Dictionary with a tuple (name of renamed module / file of the renamed module) --> 
     * occurrences to be renamed
     */
    private Map<Tuple<String, File>, HashSet<ASTEntry>> fileOccurrences;

    /**
     * Occurrences to be renamed in the current module.
     */
    private HashSet<ASTEntry> docOccurrences;

    private IFile currentFile;

    public TextEditCreation(String initialName, String inputName, String moduleName, IDocument currentDoc,
            List<IRefactorRenameProcess> processes, RefactoringStatus status, CompositeChange fChange, IFile currentFile) {
        this.initialName = initialName;
        this.inputName = inputName;
        this.moduleName = moduleName;
        this.currentDoc = currentDoc;
        this.processes = processes;
        this.fChange = fChange;
        this.status = status;
        this.currentFile = currentFile;
    }

    /**
     * In this method, changes from the occurrences found in the current document and 
     * other files are transformed to the objects required by the Eclipse Language Toolkit
     */
    public void fillRefactoringChangeObject(RefactoringRequest request, CheckConditionsContext context) {

        for (IRefactorRenameProcess p : processes) {
            if (status.hasFatalError() || request.getMonitor().isCanceled()) {
                break;
            }
            HashSet<ASTEntry> occurrences = p.getOccurrences();
            if (docOccurrences == null) {
                docOccurrences = occurrences;
            } else {
                docOccurrences.addAll(occurrences);
            }

            Map<Tuple<String, File>, HashSet<ASTEntry>> occurrencesInOtherFiles = p.getOccurrencesInOtherFiles();
            if (fileOccurrences == null) {
                fileOccurrences = occurrencesInOtherFiles;
            } else {
                //iterate in a copy
                for (Map.Entry<Tuple<String, File>, HashSet<ASTEntry>> entry : new HashMap<Tuple<String, File>, HashSet<ASTEntry>>(
                        occurrencesInOtherFiles).entrySet()) {
                    HashSet<ASTEntry> set = occurrencesInOtherFiles.get(entry.getKey());
                    if (set == null) {
                        occurrencesInOtherFiles.put(entry.getKey(), entry.getValue());
                    } else {
                        set.addAll(entry.getValue());
                    }
                }
            }
        }

        createCurrModuleChange();
        createOtherFileChanges();
    }

    /**
     * Create the changes for references in other modules.
     * 
     * @param fChange the 'root' change.
     * @param status the status of the change
     * @param editsAlreadyCreated 
     */
    private void createOtherFileChanges() {

        for (Map.Entry<Tuple<String, File>, HashSet<ASTEntry>> entry : fileOccurrences.entrySet()) {
            //key = module name, IFile for the module (__init__ file may be found if it is a package)
            Tuple<String, File> tup = entry.getKey();

            //now, let's make the mapping from the filesystem to the Eclipse workspace
            IFile workspaceFile = null;
            try {
                workspaceFile = new PySourceLocatorBase().getWorkspaceFile(tup.o2);
                if (workspaceFile == null) {
                    status.addWarning(org.python.pydev.shared_core.string.StringUtils.format("Error. Unable to resolve the file:\n" + "%s\n"
                            + "to a file in the Eclipse workspace.", tup.o2));
                    continue;
                }
            } catch (IllegalStateException e) {
                //this can happen on tests (but if not on tests, we want to re-throw it
                String message = e.getMessage();
                if (message == null || !message.equals("Workspace is closed.")) {
                    throw e;
                }
                //otherwise, let's just keep going in the test...
                continue;
            }

            //check the text changes
            HashSet<ASTEntry> astEntries = filterAstEntries(entry.getValue(), AST_ENTRIES_FILTER_TEXT);
            if (astEntries.size() > 0) {
                IDocument docFromResource = FileUtilsFileBuffer.getDocFromResource(workspaceFile);
                TextFileChange fileChange = new PyTextFileChange("RenameChange: " + inputName, workspaceFile);

                MultiTextEdit rootEdit = new MultiTextEdit();
                fileChange.setEdit(rootEdit);
                fileChange.setKeepPreviewEdits(true);

                List<Tuple<TextEdit, String>> renameEdits = getAllRenameEdits(docFromResource, astEntries);
                fillEditsInDocChange(fileChange, rootEdit, renameEdits);
            }

            //now, check for file changes
            astEntries = filterAstEntries(entry.getValue(), AST_ENTRIES_FILTER_FILE);
            if (astEntries.size() > 0) {
                IResource resourceToRename = workspaceFile;
                String newName = inputName + ".py";

                //if we have an __init__ file but the initial token is not an __init__ file, it means
                //that we have to rename the folder that contains the __init__ file
                if (tup.o1.endsWith(".__init__") && !initialName.equals("__init__")) {
                    resourceToRename = resourceToRename.getParent();
                    newName = inputName;

                    if (!resourceToRename.getName().equals(initialName)) {
                        status.addFatalError(org.python.pydev.shared_core.string.StringUtils
                                .format("Error. The package that was found (%s) for renaming does not match the initial token found (%s)",
                                        resourceToRename.getName(), initialName));
                        return;
                    }
                }

                fChange.add(new PyRenameResourceChange(resourceToRename, newName, org.python.pydev.shared_core.string.StringUtils.format(
                        "Renaming %s to %s", resourceToRename.getName(), inputName)));
            }
        }
    }

    private final static int AST_ENTRIES_FILTER_TEXT = 1;

    private final static int AST_ENTRIES_FILTER_FILE = 2;

    private HashSet<ASTEntry> filterAstEntries(HashSet<ASTEntry> value, int astEntryFilter) {
        HashSet<ASTEntry> ret = new HashSet<ASTEntry>();

        for (ASTEntry entry : value) {
            if (entry instanceof ASTEntryWithSourceModule) {
                if ((astEntryFilter & AST_ENTRIES_FILTER_FILE) != 0) {
                    ret.add(entry);
                }
            } else {
                if ((astEntryFilter & AST_ENTRIES_FILTER_TEXT) != 0) {
                    ret.add(entry);
                }
            }
        }

        return ret;
    }

    /**
     * Create the change for the current module
     * 
     * @param status the status for the change.
     * @param fChange tho 'root' change.
     * @param editsAlreadyCreated 
     */
    private void createCurrModuleChange() {
        TextChange docChange;
        if (this.currentFile != null) {
            docChange = new PyTextFileChange("Current module: " + moduleName, this.currentFile);
        } else {
            //used for tests
            docChange = PyDocumentChange.create("Current module: " + moduleName, this.currentDoc);
        }
        if (docOccurrences.size() == 0) {
            status.addFatalError("No occurrences found.");
            return;
        }

        MultiTextEdit rootEdit = new MultiTextEdit();
        docChange.setEdit(rootEdit);
        docChange.setKeepPreviewEdits(true);

        List<Tuple<TextEdit, String>> renameEdits = getAllRenameEdits(currentDoc, docOccurrences);
        fillEditsInDocChange(docChange, rootEdit, renameEdits);
    }

    /**
     * Puts the edits found in a doc change, tak
     * @param fChange
     * @param editsAlreadyCreatedLst
     * @param docChange
     * @param rootEdit
     * @param renameEdits
     */
    private void fillEditsInDocChange(TextChange docChange, MultiTextEdit rootEdit,
            List<Tuple<TextEdit, String>> renameEdits) {
        try {
            boolean addedEdit = false;
            for (Tuple<TextEdit, String> t : renameEdits) {
                addedEdit = true;
                rootEdit.addChild(t.o1);
                docChange.addTextEditGroup(new TextEditGroup(t.o2, t.o1));
            }
            if (addedEdit) {
                fChange.add(docChange);
            }
        } catch (RuntimeException e) {
            //StringBuffer buf = new StringBuffer("Found occurrences:");
            //for (Tuple<TextEdit, String> t : renameEdits) {
            //  buf.append("Offset: ");
            //  buf.append(t.o1.getOffset());
            //  buf.append("Len: ");
            //  buf.append(t.o1.getLength());
            //  buf.append("Str: ");
            //  buf.append(t.o2);
            //  buf.append("\n");
            //}
            //
            //don't bother reporting this to the user (usually happens if we have it the file changes during the analysis).
            //PydevPlugin.log(buf.toString(), e);
            throw e;
        }
    }

    /**
     * Create a text edit on the given offset.
     * 
     * It uses the information in the request to obtain the length of the replace and
     * the new name to be set in the replace
     * 
     * @param offset the offset marking the place where the replace should happen.
     * @return a TextEdit correponding to a rename.
     */
    protected TextEdit createRenameEdit(int offset) {
        return new ReplaceEdit(offset, initialName.length(), inputName);
    }

    /**
     * Gets the occurrences in a document and converts it to a TextEdit as required
     * by the Eclipse language toolkit.
     * 
     * @param occurrences the occurrences found
     * @param doc the doc where the occurrences were found
     * @param occurrences 
     * @return a list of tuples with the TextEdit and the description for that edit.
     */
    protected List<Tuple<TextEdit, String>> getAllRenameEdits(IDocument doc, HashSet<ASTEntry> occurrences) {
        Set<Integer> s = new HashSet<Integer>();

        List<Tuple<TextEdit, String>> ret = new ArrayList<Tuple<TextEdit, String>>();
        //occurrences = sortOccurrences(occurrences);

        FastStringBuffer entryBuf = new FastStringBuffer();
        for (ASTEntry entry : occurrences) {
            entryBuf.clear();

            Integer loc = (Integer) entry.getAdditionalInfo(AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_LOCATION, 0);

            if (loc == AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_IN_COMMENT) {
                entryBuf.append("Change (comment): ");

            } else if (loc == AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_IN_STRING) {
                entryBuf.append("Change (string): ");

            } else {
                entryBuf.append("Change: ");
            }
            entryBuf.append(initialName);
            entryBuf.append(" >> ");
            entryBuf.append(inputName);
            entryBuf.append(" (line:");
            entryBuf.append(entry.node.beginLine);
            entryBuf.append(")");

            int offset = AbstractRenameRefactorProcess.getOffset(doc, entry);
            if (!s.contains(offset)) {
                s.add(offset);
                ret.add(new Tuple<TextEdit, String>(createRenameEdit(offset), entryBuf.toString()));
            }
        }
        return ret;
    }

}
