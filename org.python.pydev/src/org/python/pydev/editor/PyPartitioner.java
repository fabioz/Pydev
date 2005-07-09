/*
 * License: Common Public License v1.0
 * Created on Jun 27, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor;

import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;

/**
 * @author Fabio Zadrozny
 */
public class PyPartitioner extends FastPartitioner{

    /**
     * @param scanner
     * @param legalContentTypes
     */
    public PyPartitioner(IPartitionTokenScanner scanner, String[] legalContentTypes) {
        super(scanner, legalContentTypes);
    }


}
