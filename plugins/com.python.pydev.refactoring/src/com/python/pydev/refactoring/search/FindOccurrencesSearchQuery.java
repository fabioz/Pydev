/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.search;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.search.ui.ISearchResult;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.editorinput.PySourceLocatorBase;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.IPyRefactoring2;
import com.python.pydev.refactoring.actions.PyFindAllOccurrences;
import com.python.pydev.refactoring.refactorer.search.AbstractPythonSearchQuery;
import com.python.pydev.refactoring.wizards.rename.AbstractRenameRefactorProcess;
import com.python.pydev.ui.search.FileMatch;
import com.python.pydev.ui.search.LineElement;

public class FindOccurrencesSearchQuery extends AbstractPythonSearchQuery {

    private final IPyRefactoring2 pyRefactoring;
    private final RefactoringRequest req;
    private FindOccurrencesSearchResult findOccurrencesSearchResult;

    public FindOccurrencesSearchQuery(IPyRefactoring2 r, RefactoringRequest req) {
        super(req.initialName);
        this.pyRefactoring = r;
        this.req = req;
    }

    public ISearchResult getSearchResult() {
        if (findOccurrencesSearchResult == null) {
            findOccurrencesSearchResult = new FindOccurrencesSearchResult(this);
        }
        return findOccurrencesSearchResult;
    }

    public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
        try {
            monitor.beginTask("Searching...", 100);
            req.pushMonitor(monitor);

            Map<Tuple<String, File>, HashSet<ASTEntry>> occurrences;
            try {
                req.pushMonitor(new SubProgressMonitor(monitor, 80));
                occurrences = pyRefactoring.findAllOccurrences(req);
            } finally {
                req.popMonitor().done();
            }

            if (occurrences == null) {
                return Status.OK_STATUS;
            }
            int length = req.initialName.length();

            HashSet<Integer> foundOffsets = new HashSet<Integer>();
            try {
                req.pushMonitor(new SubProgressMonitor(monitor, 20));
                Set<Entry<Tuple<String, File>, HashSet<ASTEntry>>> entrySet = occurrences.entrySet();
                req.getMonitor().beginTask("Resolving occurrences...", entrySet.size());
                for (Map.Entry<Tuple<String, File>, HashSet<ASTEntry>> o : entrySet) {

                    foundOffsets.clear();

                    IFile workspaceFile = null;
                    try {
                        workspaceFile = new PySourceLocatorBase().getWorkspaceFile(o.getKey().o2);
                        if (workspaceFile == null) {
                            Log.logInfo(StringUtils.format("Ignoring: %s. "
                                    + "Unable to resolve to a file in the Eclipse workspace.", o.getKey().o2));
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

                    IDocument doc = REF.getDocFromResource(workspaceFile);
                    req.getMonitor().setTaskName("Resolving occurrences... " + workspaceFile);

                    for (ASTEntry entry : o.getValue()) {
                        int offset = AbstractRenameRefactorProcess.getOffset(doc, entry);
                        if (!foundOffsets.contains(offset)) {
                            foundOffsets.add(offset);
                            if (PyFindAllOccurrences.DEBUG_FIND_REFERENCES) {
                                System.out.println("Adding match:" + workspaceFile);
                            }
                            PySelection ps = new PySelection(doc, offset);
                            int lineNumber = ps.getLineOfOffset();
                            String lineContents = ps.getLine(lineNumber);
                            int lineStartOffset = ps.getLineOffset(lineNumber);

                            LineElement element = new LineElement(workspaceFile, lineNumber, lineStartOffset,
                                    lineContents);
                            findOccurrencesSearchResult.addMatch(new FileMatch(workspaceFile, offset, length, element));
                        }
                    }
                }
            } finally {
                req.popMonitor().done();
            }
        } catch (CoreException e) {
            Log.log(e);
        } finally {
            req.popMonitor().done();
        }
        return Status.OK_STATUS;
    }

    public String getResultLabel(int nMatches) {
        String searchString = getSearchString();
        if (searchString.length() > 0) {
            // text search
            if (isScopeAllFileTypes()) {
                // search all file extensions
                if (nMatches == 1) {
                    return StringUtils.format("%s - 1 match in %s", searchString, getDescription());
                }
                return StringUtils.format("%s - %s matches in %s", searchString, new Integer(nMatches),
                        getDescription());
            }
            // search selected file extensions
            if (nMatches == 1) {
                return StringUtils.format("%s - 1 match in %s", searchString, getDescription());
            }
            return StringUtils.format("%s - %s matches in %s", searchString, new Integer(nMatches), getDescription());
        }
        throw new RuntimeException("Unexpected condition when finding: " + searchString);
    }

    private String getDescription() {
        return "'" + req.pyEdit.getProject().getName() + "' and related projects";
    }

}
