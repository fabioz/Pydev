/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import java.util.Iterator;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Iterator through imports that yields tuples with the import itself, the initial line of the import
 * and the final line of the import.
 *
 * @author Fabio
 */
public class PyImportsIterator implements Iterator<ImportHandle> {

    /**
     * Helper to iterate in the document
     */
    private final PyDocIterator docIterator;

    /**
     * Variable holding whether hasNext should return true or not
     */
    private boolean hasNext = true;

    /**
     * Variable holding the next return value
     */
    private ImportHandle nextImport;

    /**
     * Delimiter to be used to add new lines in the imports found.
     */
    private final String delimiter;

    /**
     * Document used in the iteration
     */
    private final IDocument doc;

    private final boolean addOnlyGlobalImports;

    private final boolean allowBadInput;

    public PyImportsIterator(IDocument doc) {
        this(doc, true);
    }

    /**
     * Constructor
     * 
     * @param doc the document from where the import should be gathered.
     * @param allowBadInput 
     */
    public PyImportsIterator(IDocument doc, boolean addOnlyGlobalImports, boolean allowBadInput) {
        this.doc = doc;
        this.addOnlyGlobalImports = addOnlyGlobalImports;
        this.allowBadInput = allowBadInput;
        delimiter = PySelection.getDelimiter(doc);
        this.docIterator = new PyDocIterator(doc, false, false, false, true);
        //gather the 1st import
        calcNext();
    }

    public PyImportsIterator(IDocument doc, boolean addOnlyGlobalImports) {
        this(doc, addOnlyGlobalImports, false);
    }

    /**
     * Pre-calculates the next return value and whether there is a next value to be returned.
     */
    private void calcNext() {
        if (!hasNext) {
            //only pre-calculate if there's something to pre-calculate.
            return;
        }
        int startFoundLine = -1;

        hasNext = false;
        nextImport = null;
        while (docIterator.hasNext()) {
            String str = docIterator.next();

            boolean match;
            if (addOnlyGlobalImports) {
                match = str.startsWith("import ") || str.startsWith("from ") || str.trim().equals("import");
            } else {
                str = StringUtils.leftTrim(str);
                match = str.startsWith("import ") || str.startsWith("from ") || str.trim().equals("import");
            }

            if (match) {
                startFoundLine = docIterator.getLastReturnedLine();

                if (str.indexOf('(') != -1) { //we have something like from os import (pipe,\nfoo)
                    while (docIterator.hasNext() && str.indexOf(')') == -1) {
                        str += delimiter + docIterator.next();
                    }
                }
                if (StringUtils.endsWith(str, '\\')) {
                    while (docIterator.hasNext() && StringUtils.endsWith(str, '\\')) {
                        str += delimiter + docIterator.next();
                    }
                }
                try {
                    nextImport = new ImportHandle(doc, str, startFoundLine, -1, allowBadInput); //-1 == endFoundLine (which will be properly set later on).
                } catch (ImportNotRecognizedException e) {
                    continue;
                }
                hasNext = true;
                nextImport.endFoundLine = docIterator.getLastReturnedLine();
                break; //ok, import found
            }
        }

    }

    /**
     * From the iterator interface
     */
    public boolean hasNext() {
        return this.hasNext;
    }

    /**
     * From the iterator interface
     */
    public ImportHandle next() {
        ImportHandle ret = this.nextImport;
        calcNext(); //pre-compute the next step
        return ret;
    }

    /**
     * From the iterator interface (not implemented)
     */
    public void remove() {
        throw new RuntimeException("Not implemented");
    }

}
