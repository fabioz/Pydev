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
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editorinput.PySourceLocatorBase;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.StringUtils;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IReferenceSearches;

/**
 * Searches the internal indexes from PyDev.
 *
 * Still a work in progress (we want to include/exclude by package name).
 */
public class SearchIndexQuery implements ISearchQuery {

    private SearchIndexResult fResult;

    private boolean caseInsensitive = true;

    public final String text;

    public SearchIndexQuery(String text) {
        this.text = text;
    }

    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    public boolean getCaseInsensitive() {
        return this.caseInsensitive;
    }

    @Override
    public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
        SearchIndexResult searchResult = (SearchIndexResult) getSearchResult();
        //Remove all so that we don't get duplicates on a search refresh.
        searchResult.removeAll();

        StringMatcherWithIndexSemantics stringMatcher = createStringMatcher();

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
                createMatches(doc, text, stringMatcher, workspaceFile, searchResult, modulesKey);
            }
        }

        return Status.OK_STATUS;
    }

    public void createMatches(IDocument doc, String text, StringMatcherWithIndexSemantics stringMatcher,
            IFile workspaceFile,
            AbstractTextSearchResult searchResult, ModulesKey modulesKey) {

        StringMatcherWithIndexSemantics.Position find = stringMatcher.find(text, 0);
        while (find != null) {
            int offset = find.getStart();
            int end = find.getEnd();
            int length = end - offset;

            PySelection ps = new PySelection(doc, offset);
            int lineNumber = ps.getLineOfOffset();
            String lineContents = ps.getLine(lineNumber);
            int lineStartOffset = ps.getLineOffset(lineNumber);

            ModuleLineElement element = new ModuleLineElement(workspaceFile, lineNumber, lineStartOffset, lineContents);
            searchResult.addMatch(new ModuleMatch(workspaceFile, offset, length, element, modulesKey));
            find = stringMatcher.find(text, end);
        }
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

    public StringMatcherWithIndexSemantics createStringMatcher() {
        boolean ignoreCase = getCaseInsensitive();
        StringMatcherWithIndexSemantics stringMatcher = new StringMatcherWithIndexSemantics(text, ignoreCase);
        return stringMatcher;
    }

    public String getResultLabel(int nMatches) {
        String searchString = text;
        if (nMatches == 1) {
            return StringUtils.format("%s - 1 match", searchString);
        }
        return StringUtils.format("%s - %s matches", searchString, nMatches);
    }

}
