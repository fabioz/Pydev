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
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.core.partition.PyPartitioner;
import org.python.pydev.shared_core.testutils.TestUtils;

public class PyPartitionScannerTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyPartitionScannerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPartitioning() throws Exception {
        Document doc = new Document("class Foo: #comment\n" +
                "    pass\n");
        IDocumentPartitioner partitioner = PyPartitionScanner.addPartitionScanner(doc, null);
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
        assertEquals(TestUtils.listToExpected("__python_singleline_bytes_or_unicode1:0:7",
                "null:7:1",
                "null:8:1",
                "null:9:1",
                "__python_singleline_bytes_or_unicode1:10:8",
                "null:18:1",
                "null:19:1",
                "__python_singleline_bytes_or_unicode1:20:3",
                "null:23:1",
                "__python_multiline_bytes_or_unicode1:24:9",
                "null:33:1"), scan);

        partitioner.connect(document);
        document.setDocumentPartitioner(partitioner);
        checkPartitions(document, "__python_singleline_bytes_or_unicode1:0:7",
                "__dftl_partition_content_type:7:10",
                "__python_singleline_bytes_or_unicode1:10:18",
                "__dftl_partition_content_type:18:20",
                "__python_singleline_bytes_or_unicode1:20:23",
                "__dftl_partition_content_type:23:24",
                "__python_multiline_bytes_or_unicode1:24:33",
                "__dftl_partition_content_type:33:");

        document.replace(txt.length() - " ''' ".length(), 0, "i");
        checkPartitions(document, "__python_singleline_bytes_or_unicode1:0:7",
                "__dftl_partition_content_type:7:10",
                "__python_singleline_bytes_or_unicode1:10:18",
                "__dftl_partition_content_type:18:20",
                "__python_singleline_bytes_or_unicode1:20:23",
                "__dftl_partition_content_type:23:24",
                "__python_multiline_bytes_or_unicode1:24:34",
                "__dftl_partition_content_type:34:");

        document.replace(txt.length() - " ''' ".length() + 1, 0, "j");
        checkPartitions(document, "__python_singleline_bytes_or_unicode1:0:7",
                "__dftl_partition_content_type:7:10",
                "__python_singleline_bytes_or_unicode1:10:18",
                "__dftl_partition_content_type:18:20",
                "__python_singleline_bytes_or_unicode1:20:23",
                "__dftl_partition_content_type:23:24",
                "__python_multiline_bytes_or_unicode1:24:35",
                "__dftl_partition_content_type:35:");

    }

    public void testPartitioning4() throws Exception {
        String txt = ""
                + "class F:\n"
                + "    '''test'''\n"
                + "	\"\"\"test\"\"\"\n"
                + "	'test'\n"
                + "	\"test\"\n"
                + "	`test`\n"
                + "	#test\n"
                + "	test = 10.\n"
                + "";

        IDocument document = new Document(txt);
        PyPartitioner partitioner = PyPartitionScanner.createPyPartitioner();
        String scan = TestUtils.scan(partitioner.getScanner(), document);
        assertEquals(TestUtils.listToExpected("null:0:1",
                "null:1:1",
                "null:2:1",
                "null:3:1",
                "null:4:1",
                "null:5:1",
                "null:6:1",
                "null:7:1",
                "null:8:1",
                "null:9:1",
                "null:10:1",
                "null:11:1",
                "null:12:1",
                "__python_multiline_bytes_or_unicode1:13:10",
                "null:23:1",
                "null:24:1",
                "__python_multiline_bytes_or_unicode2:25:10",
                "null:35:1",
                "null:36:1",
                "__python_singleline_bytes_or_unicode1:37:6",
                "null:43:1",
                "null:44:1",
                "__python_singleline_bytes_or_unicode2:45:6",
                "null:51:1",
                "null:52:1",
                "__python_backquotes:53:6",
                "null:59:1",
                "null:60:1",
                "__python_comment:61:6",
                "null:67:1",
                "null:68:1",
                "null:69:1",
                "null:70:1",
                "null:71:1",
                "null:72:1",
                "null:73:1",
                "null:74:1",
                "null:75:1",
                "null:76:1",
                "null:77:1",
                "null:78:1"), scan);

    }

    public void testPartitioning5() throws Exception {
        String txt = ""
                + "'''test'''"
                + "";

        IDocument document = new Document(txt);
        PyPartitioner partitioner = PyPartitionScanner.createPyPartitioner();
        String scan = TestUtils.scan(partitioner.getScanner(), document);
        assertEquals(TestUtils.listToExpected("__python_multiline_bytes_or_unicode1:0:10"), scan);

    }

    private void checkPartitions(IDocument document, String... expected) throws Exception {
        String found = TestUtils.getContentTypesAsStr(document);
        assertEquals(TestUtils.listToExpected(expected), found);
    }

}
