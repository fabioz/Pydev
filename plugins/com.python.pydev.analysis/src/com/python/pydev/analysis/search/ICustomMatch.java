package com.python.pydev.analysis.search;

import org.eclipse.core.resources.IFile;

public interface ICustomMatch {

    int getOriginalOffset();

    int getOriginalLength();

    ICustomLineElement getLineElement();

    IFile getFile();

}
