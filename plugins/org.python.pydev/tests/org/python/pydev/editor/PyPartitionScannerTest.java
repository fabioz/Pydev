/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 29, 2006
 */
package org.python.pydev.editor;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.IToken;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.docutils.PyPartitionScanner;
import org.python.pydev.core.docutils.PyPartitioner;
import org.python.pydev.shared_core.testutils.TestUtils;

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

    public void testPartitioning3() throws Exception {
        String txt = ""
                + "'\\\n"
                + "\\\n"
                + "a' b '\\\n"
                + "c\\\n"
                + "e' f'g' ''' h ''' "
                + "";

        IDocument document = new Document(txt);
        PyPartitioner partitioner = PyPartitionScanner.createPyPartitioner();
        String scan = TestUtils.scan(partitioner.getScanner(), document);
        assertEquals(TestUtils.listToExpected("__python_singleline_string1:0:7",
                "null:7:1",
                "null:8:1",
                "null:9:1",
                "__python_singleline_string1:10:8",
                "null:18:1",
                "null:19:1",
                "__python_singleline_string1:20:3",
                "null:23:1",
                "__python_multiline_string1:24:9",
                "null:33:1"), scan);

        partitioner.connect(document);
        document.setDocumentPartitioner(partitioner);
        checkPartitions(document, "__python_singleline_string1:0:7",
                "__dftl_partition_content_type:7:10",
                "__python_singleline_string1:10:18",
                "__dftl_partition_content_type:18:20",
                "__python_singleline_string1:20:23",
                "__dftl_partition_content_type:23:24",
                "__python_multiline_string1:24:33",
                "__dftl_partition_content_type:33:");

        document.replace(txt.length() - " ''' ".length(), 0, "i");
        checkPartitions(document, "__python_singleline_string1:0:7",
                "__dftl_partition_content_type:7:10",
                "__python_singleline_string1:10:18",
                "__dftl_partition_content_type:18:20",
                "__python_singleline_string1:20:23",
                "__dftl_partition_content_type:23:24",
                "__python_multiline_string1:24:34",
                "__dftl_partition_content_type:34:");

        document.replace(txt.length() - " ''' ".length() + 1, 0, "j");
        checkPartitions(document, "__python_singleline_string1:0:7",
                "__dftl_partition_content_type:7:10",
                "__python_singleline_string1:10:18",
                "__dftl_partition_content_type:18:20",
                "__python_singleline_string1:20:23",
                "__dftl_partition_content_type:23:24",
                "__python_multiline_string1:24:35",
                "__dftl_partition_content_type:35:");

    }

    private void checkPartitions(IDocument document, String... expected) throws Exception {
        String found = TestUtils.getContentTypesAsStr(document);
        assertEquals(TestUtils.listToExpected(expected), found);
    }

}
