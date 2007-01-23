package com.python.pydev.refactoring.refactorer.search;


import org.eclipse.core.resources.IFile;
import org.eclipse.search.ui.text.Match;

public class FileMatch extends Match {
    private long fCreationTimeStamp;
    
    public FileMatch(IFile element, int offset, int length) {
        super(element, offset, length);
        fCreationTimeStamp= element.getModificationStamp();
    }
    
    public IFile getFile() {
        return (IFile) getElement();
    }

    public long getCreationTimeStamp() {
        return fCreationTimeStamp;
    }
}
