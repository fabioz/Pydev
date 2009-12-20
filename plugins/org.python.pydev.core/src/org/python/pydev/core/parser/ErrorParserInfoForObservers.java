package org.python.pydev.core.parser;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;

public class ErrorParserInfoForObservers {
    
    public final Throwable error;
    public final IAdaptable file;
    public final IDocument doc;
    public final Object[] argsToReparse;

    public ErrorParserInfoForObservers(Throwable error, IAdaptable file, IDocument doc, Object ... argsToReparse){
        this.error = error;
        this.file = file;
        this.doc = doc;
        this.argsToReparse = argsToReparse;
    }

}
