/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.search_index;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editorinput.PySourceLocatorBase;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.StringMatcher;
import org.python.pydev.shared_core.string.StringMatcher.Position;
import org.python.pydev.shared_core.string.StringUtils;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IReferenceSearches;
import com.python.pydev.analysis.search.FileMatch;
import com.python.pydev.analysis.search.LineElement;

/**
 * Searches the internal indexes from PyDev.
 *
 * Still a work in progress (we want to include/exclude by package name).
 */
public class SearchIndexQuery implements ISearchQuery {

    private SearchIndexResult fResult;
    public final String text;

    public SearchIndexQuery(String text) {
        this.text = text;
    }

    @Override
    public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
        SearchIndexResult searchResult = (SearchIndexResult) getSearchResult();

        boolean ignoreWildCards = false;
        boolean ignoreCase = true;
        StringMatcher stringMatcher = new StringMatcher(text, ignoreCase, ignoreWildCards);

        List<IPythonNature> allPythonNatures = PythonNature.getAllPythonNatures();
        for (IPythonNature nature : allPythonNatures) {
            AbstractAdditionalDependencyInfo info;
            try {
                info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);
            } catch (MisconfigurationException e) {
                Log.log(e);
                continue;
            }
            IReferenceSearches referenceSearches = info.getReferenceSearches();
            List<ModulesKey> search = referenceSearches.search(nature.getProject(), text, monitor);

            IFile workspaceFile;
            for (ModulesKey modulesKey : search) {
                File file = modulesKey.file;
                if (file == null || !file.exists()) {
                    Log.logInfo(StringUtils.format("Ignoring: %s. File no longer exists.", file));
                }

                workspaceFile = new PySourceLocatorBase().getWorkspaceFile(file, nature.getProject());
                if (workspaceFile == null) {
                    Log.logInfo(StringUtils
                            .format("Ignoring: %s. Unable to resolve to a file in the Eclipse workspace.", file));
                    continue;
                }

                IDocument doc = FileUtilsFileBuffer.getDocFromResource(workspaceFile);
                String text = doc.get();
                Position find = stringMatcher.find(text, 0, text.length());
                int offset = find.getStart();
                int length = find.getEnd() - offset;

                PySelection ps = new PySelection(doc, offset);
                int lineNumber = ps.getLineOfOffset();
                String lineContents = ps.getLine(lineNumber);
                int lineStartOffset = ps.getLineOffset(lineNumber);

                LineElement element = new LineElement(workspaceFile, lineNumber, lineStartOffset, lineContents);
                searchResult.addMatch(new FileMatch(workspaceFile, offset, length, element));
            }
        }

        return Status.OK_STATUS;
    }

    @Override
    public String getLabel() {
        return "Index Search";
    }

    @Override
    public boolean canRerun() {
        return true;
    }

    @Override
    public boolean canRunInBackground() {
        return true;
    }

    @Override
    public ISearchResult getSearchResult() {
        if (fResult == null) {
            fResult = new SearchIndexResult(this);
            new SearchResultUpdater(fResult);
        }
        return fResult;
    }

}
