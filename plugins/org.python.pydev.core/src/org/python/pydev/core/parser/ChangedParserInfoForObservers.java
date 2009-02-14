package org.python.pydev.core.parser;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;

public class ChangedParserInfoForObservers {

    public final ISimpleNode root;
    public final IAdaptable file;
    public final IDocument doc;
    public final Object[] argsToReparse;
    public final long documentTime;

    public ChangedParserInfoForObservers(ISimpleNode root, IAdaptable file, IDocument doc, long documentTime, Object ... argsToReparse){
        this.root = root;
        this.file = file;
        this.doc = doc;
        this.argsToReparse = argsToReparse;
        this.documentTime = documentTime;
    }
}
