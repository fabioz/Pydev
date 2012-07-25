/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.parser;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;

public class ChangedParserInfoForObservers {

    public final ISimpleNode root;
    public final IAdaptable file;
    public final IDocument doc;
    public final Object[] argsToReparse;
    public final long documentTime;

    public ChangedParserInfoForObservers(ISimpleNode root, IAdaptable file, IDocument doc, long documentTime,
            Object... argsToReparse) {
        this.root = root;
        this.file = file;
        this.doc = doc;
        this.argsToReparse = argsToReparse;
        this.documentTime = documentTime;
    }
}
