package org.python.pydev.shared_ui.search;

import org.eclipse.core.resources.IFile;

public interface ICustomMatch {

    int getOriginalOffset();

    int getOriginalLength();

    ICustomLineElement getLineElement();

    IFile getFile();

}
