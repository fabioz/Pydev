/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: July 10, 2003
 */

package org.python.pydev.core.partition;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.log.Log;

/**
 * Rule-based partition scanner
 *
 * Simple, fast parsing of the document into partitions.<p>
 * This is like a rough 1st pass at parsing. We only parse
 * out for comments, single-line strings, and multiline strings<p>
 * The results are parsed again inside {@link org.python.pydev.editor.PyEditConfiguration#getPresentationReconciler}
 * and colored there.<p>
 *
 * "An IPartitionTokenScanner can also start in the middle of a partition,
 * if it knows the type of the partition."
 */
public class PyPartitionScanner extends AbstractPyPartitionScanner {

    public PyPartitionScanner() {
        super();
    }

    /**
     * @return all types recognized by this scanner (used by doc partitioner)
     */
    static public String[] getTypes() {
        return IPythonPartitions.types;
    }

    public static IDocumentPartitioner checkPartitionScanner(IDocument document) {
        return checkPartitionScanner(document, null);
    }

    /**
     * Checks if the partitioner is correctly set in the document.
     * @return the partitioner that is set in the document
     */
    public static IDocumentPartitioner checkPartitionScanner(IDocument document,
            IGrammarVersionProvider grammarVersionProvider) {
        if (document == null) {
            return null;
        }

        IDocumentExtension3 docExtension = (IDocumentExtension3) document;
        IDocumentPartitioner partitioner = docExtension.getDocumentPartitioner(IPythonPartitions.PYTHON_PARTITION_TYPE);
        if (partitioner == null) {
            addPartitionScanner(document, grammarVersionProvider);
            //get it again for the next check
            partitioner = docExtension.getDocumentPartitioner(IPythonPartitions.PYTHON_PARTITION_TYPE);
        }
        if (!(partitioner instanceof PyPartitioner)) {
            Log.log("Partitioner should be subclass of PyPartitioner. It is " + partitioner.getClass());
        }

        return partitioner;
    }

    /**
     * @see http://help.eclipse.org/help31/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/editors_documents.htm
     * @see http://jroller.com/page/bobfoster -  Saturday July 16, 2005
     * @param document the document where we want to add the partitioner
     * @return the added document partitioner (or null)
     */
    public static IDocumentPartitioner addPartitionScanner(IDocument document,
            IGrammarVersionProvider grammarVersionProvider) {
        if (document != null) {
            IDocumentExtension3 docExtension = (IDocumentExtension3) document;
            IDocumentPartitioner curr = docExtension.getDocumentPartitioner(IPythonPartitions.PYTHON_PARTITION_TYPE);

            if (curr == null) {
                //set the new one
                PyPartitioner partitioner = createPyPartitioner();
                partitioner.connect(document);
                docExtension.setDocumentPartitioner(IPythonPartitions.PYTHON_PARTITION_TYPE, partitioner);
                return partitioner;
            } else {
                return curr;
            }
        }
        return null;
    }

    public static PyPartitioner createPyPartitioner() {
        return new PyPartitioner(new PyPartitionScanner(), getTypes());
    }

}
