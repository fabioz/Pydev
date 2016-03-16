/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import java.util.Iterator;

import org.eclipse.jface.text.IDocument;

/**
 * This class is responsible for gathering information about imports and has utilities to help using them later on.
 *
 * @author Fabio
 */
public class PyImportsHandling implements Iterable<ImportHandle> {

    private final IDocument doc;
    private final boolean addOnlyGlobalImports;
    private final boolean allowBadInput;

    public PyImportsHandling(IDocument doc) {
        this(doc, true);
    }

    public PyImportsHandling(IDocument doc, boolean addOnlyGlobalImports) {
        this(doc,addOnlyGlobalImports, false);
    }
    public PyImportsHandling(IDocument doc, boolean addOnlyGlobalImports, boolean allowBadInput) {
        this.doc = doc;
        this.addOnlyGlobalImports = addOnlyGlobalImports;
        this.allowBadInput = allowBadInput;
    }

    /**
     * @return an iterator that will yield the imports available
     */
    @Override
    public Iterator<ImportHandle> iterator() {
        return new PyImportsIterator(this.doc, addOnlyGlobalImports, allowBadInput);
    }

}
