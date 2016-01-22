package com.python.pydev.analysis.search_index;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.shared_ui.search.ICustomLineElement;

/**
 * Element representing a line in a file
 *
 */
public class PyModuleLineElement implements ICustomLineElement, IAdaptable {

    private final IResource fParent;

    private final int fLineNumber;
    private final int fLineStartOffset;
    private final String fLineContents;
    public final ModulesKey modulesKey;

    public PyModuleLineElement(IResource parent, int lineNumber, int lineStartOffset, String lineContents,
            ModulesKey modulesKey) {
        fParent = parent;
        fLineNumber = lineNumber;
        fLineStartOffset = lineStartOffset;
        fLineContents = lineContents;
        this.modulesKey = modulesKey;
    }

    public IProject getProject() {
        return fParent.getProject();
    }

    public IResource getParent() {
        return fParent;
    }

    public int getLine() {
        return fLineNumber;
    }

    public String getContents() {
        return fLineContents;
    }

    public int getOffset() {
        return fLineStartOffset;
    }

    public boolean contains(int offset) {
        return fLineStartOffset <= offset && offset < fLineStartOffset + fLineContents.length();
    }

    public int getLength() {
        return fLineContents.length();
    }

    public PyModuleMatch[] getMatches(AbstractTextSearchResult result) {
        ArrayList<PyModuleMatch> res = new ArrayList<PyModuleMatch>();
        Match[] matches = result.getMatches(fParent);
        for (int i = 0; i < matches.length; i++) {
            PyModuleMatch curr = (PyModuleMatch) matches[i];
            if (curr.getLineElement() == this) {
                res.add(curr);
            }
        }
        return res.toArray(new PyModuleMatch[res.size()]);
    }

    public int getNumberOfMatches(AbstractTextSearchResult result) {
        int count = 0;
        Match[] matches = result.getMatches(fParent);
        for (int i = 0; i < matches.length; i++) {
            PyModuleMatch curr = (PyModuleMatch) matches[i];
            if (curr.getLineElement() == this) {
                count++;
            }
        }
        return count;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return this.fParent.getAdapter(adapter);
    }

}
