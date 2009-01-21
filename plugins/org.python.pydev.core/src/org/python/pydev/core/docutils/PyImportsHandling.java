package org.python.pydev.core.docutils;

import java.util.Iterator;

import org.eclipse.jface.text.IDocument;

/**
 * This class is responsible for gathering information about imports and has utilities to help using them later on.
 *
 * @author Fabio
 */
public class PyImportsHandling implements Iterable<ImportHandle>{

    private IDocument doc;

    public PyImportsHandling(IDocument doc) {
        this.doc = doc;
    }

    /**
     * @return an iterator that will yield the imports available
     */
    public Iterator<ImportHandle> iterator() {
        return new PyImportsIterator(this.doc);
    }

}
