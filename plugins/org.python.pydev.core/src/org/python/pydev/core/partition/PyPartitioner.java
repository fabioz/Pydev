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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.python.pydev.core.IPythonPartitions;

/**
 * @author Fabio Zadrozny
 */
public final class PyPartitioner extends org.eclipse.jface.text.rules.FastPartitioner {

    /**
     * @param scanner
     * @param legalContentTypes
     */
    public PyPartitioner(PyPartitionScanner scanner, String[] legalContentTypes) {
        super(scanner, legalContentTypes);
    }

    public IPartitionTokenScanner getScanner() {
        return fScanner;
    }

    @Override
    public ITypedRegion getPartition(int offset, boolean preferOpenPartitions) {
        if (preferOpenPartitions) {
            if (offset <= 0) {
                return new TypedRegion(offset, 0, IDocument.DEFAULT_CONTENT_TYPE);
            }
            if (fDocument != null && offset == fDocument.getLength()) {
                // Fix issue where a wrong partition is being gotten when a comment is being typed
                // as the last thing in the document.
                // Fixes #PyDev-762: Code completion is active in comments.
                try {
                    int lineOffset = fDocument.getLineOffset(fDocument.getLineOfOffset(offset));
                    if (lineOffset != offset) { // A comment must start with a #, so, the char 0 of a line can't be a comment itself.
                        ITypedRegion region = getPartition(offset - 1);
                        if (IPythonPartitions.PY_COMMENT.equals(region.getType())
                                || IDocument.DEFAULT_CONTENT_TYPE.equals(region.getType())) {
                            return region;
                        }
                    }
                } catch (BadLocationException e) {
                    // ignore
                }
            }
        }
        return super.getPartition(offset, preferOpenPartitions);
    }
}
