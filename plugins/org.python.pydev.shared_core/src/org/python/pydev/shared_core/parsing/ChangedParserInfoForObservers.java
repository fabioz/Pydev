/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.parsing;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.model.ISimpleNode;

public class ChangedParserInfoForObservers {

    public final ISimpleNode root;
    public final long docModificationStamp;
    public final IAdaptable file;
    public final IDocument doc;
    public final Object[] argsToReparse;
    public final long documentMillisTime;

    /**
     * This is the error info when generating the AST. May be null.
     */
    public final ErrorParserInfoForObservers errorInfo;

    public ChangedParserInfoForObservers(ISimpleNode root, long docModificationStamp, IAdaptable file, IDocument doc,
            long documentMillisTime, ErrorParserInfoForObservers errorInfo, Object... argsToReparse) {
        this.root = root;
        this.docModificationStamp = docModificationStamp;
        this.file = file;
        this.doc = doc;
        this.argsToReparse = argsToReparse;
        this.documentMillisTime = documentMillisTime;
        this.errorInfo = errorInfo;
    }
}
