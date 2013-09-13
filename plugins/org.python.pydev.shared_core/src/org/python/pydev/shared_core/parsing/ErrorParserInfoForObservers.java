/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.parsing;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;

public class ErrorParserInfoForObservers {

    public final Throwable error;
    public final IAdaptable file;
    public final IDocument doc;
    public final Object[] argsToReparse;

    public ErrorParserInfoForObservers(Throwable error, IAdaptable file, IDocument doc, Object... argsToReparse) {
        this.error = error;
        this.file = file;
        this.doc = doc;
        this.argsToReparse = argsToReparse;
    }

}
