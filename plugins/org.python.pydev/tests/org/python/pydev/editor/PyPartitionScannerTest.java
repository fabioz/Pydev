/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 29, 2006
 */
package org.python.pydev.editor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.IToken;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.docutils.PyPartitionScanner;

import junit.framework.TestCase;

public class PyPartitionScannerTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyPartitionScannerTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPartitioning() throws Exception {
        Document doc = new Document("class Foo: #comment\n" +
                "    pass\n");
        IDocumentPartitioner partitioner = PyPartitionScanner.addPartitionScanner(doc);
        assertEquals(IPythonPartitions.PY_DEFAULT, partitioner.getContentType(5));
        assertEquals(IPythonPartitions.PY_COMMENT, partitioner.getContentType(15));
    }

    public void testPartitioning2() throws Exception {
        Document doc = new Document("class Foo: #comment\n" +
                "    pass\n");
        PyPartitionScanner pyPartitionScanner = new PyPartitionScanner();
        pyPartitionScanner.setRange(doc, 0, doc.getLength());
        IToken nextPartition = pyPartitionScanner.nextToken();
        while (!nextPartition.isEOF()) {
            String data = (String) nextPartition.getData();
            assertTrue("Found: " + data, data == null || data.equals(IPythonPartitions.PY_COMMENT));
            nextPartition = pyPartitionScanner.nextToken();
        }

    }
}
