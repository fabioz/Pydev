/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jun 27, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.core.partition;

import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.python.pydev.core.IGrammarVersionProvider;

/**
 * @author Fabio Zadrozny
 */
public final class PyPartitioner extends org.eclipse.jface.text.rules.FastPartitioner {

    private PyPartitionScanner scanner;

    /**
     * @param scanner
     * @param legalContentTypes
     */
    public PyPartitioner(PyPartitionScanner scanner, String[] legalContentTypes) {
        super(scanner, legalContentTypes);
        this.scanner = scanner;
    }

    public IPartitionTokenScanner getScanner() {
        return fScanner;
    }

    public void setGrammarVersionProvider(IGrammarVersionProvider grammarVersionProvider) {
        if (grammarVersionProvider != null) {
            PyPartitionScanner s = this.scanner;
            if (s != null) {
                s.setGrammarVersionProvider(grammarVersionProvider);
            }
        }
    }

    /**
     * Returns whether the setting changed.
     */
    public boolean setFromFutureImportUnicode(boolean hasFromFutureImportUnicode) {
        PyPartitionScanner s = this.scanner;
        if (s != null) {
            return s.setFromFutureImportUnicode(hasFromFutureImportUnicode);
        }
        return false;
    }

}
