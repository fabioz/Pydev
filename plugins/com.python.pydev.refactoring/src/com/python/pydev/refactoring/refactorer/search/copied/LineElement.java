/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.search.copied;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.python.pydev.shared_ui.search.ICustomLineElement;

/**
 * Element representing a line in a file
 *
 */
public class LineElement implements ICustomLineElement {

    private final IResource fParent;

    private final int fLineNumber;
    private final int fLineStartOffset;
    private final String fLineContents;

    public LineElement(IResource parent, int lineNumber, int lineStartOffset, String lineContents) {
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

    public FileMatch[] getMatches(AbstractTextSearchResult result) {
        ArrayList<FileMatch> res = new ArrayList<FileMatch>();
        Match[] matches = result.getMatches(fParent);
        for (int i = 0; i < matches.length; i++) {
            FileMatch curr = (FileMatch) matches[i];
            if (curr.getLineElement() == this) {
                res.add(curr);
            }
        }
        return res.toArray(new FileMatch[res.size()]);
    }

    public int getNumberOfMatches(AbstractTextSearchResult result) {
        int count = 0;
        Match[] matches = result.getMatches(fParent);
        for (int i = 0; i < matches.length; i++) {
            FileMatch curr = (FileMatch) matches[i];
            if (curr.getLineElement() == this) {
                count++;
            }
        }
        return count;
    }

}
