package com.python.pydev.analysis.search_index;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.Region;
import org.eclipse.search.ui.text.Match;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.shared_ui.search.ICustomMatch;

public class PyModuleMatch extends Match implements ICustomMatch {
    private PyModuleLineElement fLineElement;
    private Region fOriginalLocation;
    private long fCreationTimeStamp;
    public final ModulesKey modulesKey;

    public PyModuleMatch(IFile element, int offset, int length, PyModuleLineElement lineEntry, ModulesKey modulesKey) {
        super(element, offset, length);
        Assert.isLegal(lineEntry != null);
        this.modulesKey = modulesKey;
        fLineElement = lineEntry;
        fCreationTimeStamp = element.getModificationStamp();
    }

    @Override
    public void setOffset(int offset) {
        if (fOriginalLocation == null) {
            // remember the original location before changing it
            fOriginalLocation = new Region(getOffset(), getLength());
        }
        super.setOffset(offset);
    }

    @Override
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

    public PyModuleLineElement getLineElement() {
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
