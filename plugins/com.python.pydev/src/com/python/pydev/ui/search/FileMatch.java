/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.ui.search;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.Region;

import org.eclipse.search.ui.text.Match;

public class FileMatch extends Match {
    private LineElement fLineElement;
    private Region fOriginalLocation;
    private long fCreationTimeStamp;

    public FileMatch(IFile element) {
        super(element, -1, -1);
        fLineElement = null;
        fOriginalLocation = null;
    }

    public FileMatch(IFile element, int offset, int length, LineElement lineEntry) {
        super(element, offset, length);
        Assert.isLegal(lineEntry != null);
        fLineElement = lineEntry;
        fCreationTimeStamp = element.getModificationStamp();
    }

    public void setOffset(int offset) {
        if (fOriginalLocation == null) {
            // remember the original location before changing it
            fOriginalLocation = new Region(getOffset(), getLength());
        }
        super.setOffset(offset);
    }

    public void setLength(int length) {
        if (fOriginalLocation == null) {
            // remember the original location before changing it
            fOriginalLocation = new Region(getOffset(), getLength());
        }
        super.setLength(length);
    }

    public int getOriginalOffset() {
        if (fOriginalLocation != null) {
            return fOriginalLocation.getOffset();
        }
        return getOffset();
    }

    public int getOriginalLength() {
        if (fOriginalLocation != null) {
            return fOriginalLocation.getLength();
        }
        return getLength();
    }

    public LineElement getLineElement() {
        return fLineElement;
    }

    public IFile getFile() {
        return (IFile) getElement();
    }

    public boolean isFileSearch() {
        return fLineElement == null;
    }

    public long getCreationTimeStamp() {
        return fCreationTimeStamp;
    }
}
