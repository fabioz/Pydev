package com.python.pydev.analysis.search_index;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

import com.python.pydev.analysis.search.ICustomLineElement;

/**
 * Element representing a line in a file
 *
 */
public class ModuleLineElement implements ICustomLineElement {

    private final IResource fParent;

    private final int fLineNumber;
    private final int fLineStartOffset;
    private final String fLineContents;

    public ModuleLineElement(IResource parent, int lineNumber, int lineStartOffset, String lineContents) {
        fParent = parent;
        fLineNumber = lineNumber;
        fLineStartOffset = lineStartOffset;
        fLineContents = lineContents;
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

    public ModuleMatch[] getMatches(AbstractTextSearchResult result) {
        ArrayList<ModuleMatch> res = new ArrayList<ModuleMatch>();
        Match[] matches = result.getMatches(fParent);
        for (int i = 0; i < matches.length; i++) {
            ModuleMatch curr = (ModuleMatch) matches[i];
            if (curr.getLineElement() == this) {
                res.add(curr);
            }
        }
        return res.toArray(new ModuleMatch[res.size()]);
    }

    public int getNumberOfMatches(AbstractTextSearchResult result) {
        int count = 0;
        Match[] matches = result.getMatches(fParent);
        for (int i = 0; i < matches.length; i++) {
            ModuleMatch curr = (ModuleMatch) matches[i];
            if (curr.getLineElement() == this) {
                count++;
            }
        }
        return count;
    }

}
