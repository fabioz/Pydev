/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.utils.DocUtils;

import junit.framework.TestCase;

public class DocUtilsTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DocUtilsTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPeer() throws Exception {
        assertEquals('(', StringUtils.getPeer(')'));
        assertEquals(')', StringUtils.getPeer('('));

        assertEquals('{', StringUtils.getPeer('}'));
        assertEquals('}', StringUtils.getPeer('{'));

        assertEquals('[', StringUtils.getPeer(']'));
        assertEquals(']', StringUtils.getPeer('['));
    }

    public void testUpdateDocWithContentsNoChange() throws Exception {
        String docContents = "a\nb\nc\n";
        String newDocContents = docContents;
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContentsRemoveFirstLines() throws Exception {
        String docContents = "a\nb\nc\n";
        String newDocContents = "b\nc\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContentsAddFirstLines() throws Exception {
        String docContents = "a\nb\nc\n";
        String newDocContents = "x\na\nb\nc\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContents() throws Exception {
        String docContents = "a\nbc\n";
        String newDocContents = "x\na\nb\nc\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContents2() throws Exception {
        String docContents = "\n";
        String newDocContents = "x\na\nb\nc\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContents4() throws Exception {
        String docContents = "";
        String newDocContents = "x\na\nb\nc\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContents5() throws Exception {
        String docContents = "x\na\nb\nc\n";
        String newDocContents = "";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContents6() throws Exception {
        String docContents = "x\na\nb\nc\n";
        String newDocContents = "\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContents7() throws Exception {
        String docContents = "x\na\nb\nc\n";
        String newDocContents = "\n\n\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContents8() throws Exception {
        String docContents = "a\nb\nc\n";
        String newDocContents = "x";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContents9() throws Exception {
        String docContents = "a\nb\nc\n";
        String newDocContents = "x\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContents10() throws Exception {
        String docContents = "a\nb\nc\n";
        String newDocContents = "\na\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContents11() throws Exception {
        String docContents = "a\nb\nc\n";
        String newDocContents = "\nc\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContentsChangeFirstLine() throws Exception {
        String docContents = "a\nb\nc\n";
        String newDocContents = "x\nb\nc\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContentsChangeFirstLines() throws Exception {
        String docContents = "a\na\n\nb\nc\n";
        String newDocContents = "x\nb\nc\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContentsChangeFirstLines2() throws Exception {
        String docContents = "a\na\n\nb\nc\n";
        String newDocContents = "x\nx\nx\nx\nx\nb\nc\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContents3() throws Exception {
        String docContents = "a\nb\nc\n";
        String newDocContents = "\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateDocWithContentsNewContentInEnd() throws Exception {
        String docContents = "a\nb\n";
        String newDocContents = "a\nb\nc";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateRemoveDocContentsFromEnd() throws Exception {
        String docContents = "a\nb\nc";
        String newDocContents = "a\nb\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testClearDoc() throws Exception {
        String docContents = "a\nb\nc";
        String newDocContents = "";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testWholeNewDoc() throws Exception {
        String docContents = "";
        String newDocContents = "a\nb\n";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateContentsInMiddle() throws Exception {
        String docContents = "from snippet7 import Foo\n" +
                "class TestIt(object):\n" +
                "";
        String newDocContents = "from snippet7 import Foo\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "class TestIt(object):\n" +
                "";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testRemoveContentsInMiddle() throws Exception {
        String docContents = "from snippet7 import Foo\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "class TestIt(object):\n" +
                "";
        String newDocContents = "from snippet7 import Foo\n" +
                "class TestIt(object):\n" +
                "";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateContentsInMiddle2() throws Exception {
        String docContents = "from snippet7 import Foo\n" +
                "class TestIt(object):\n" +
                "";
        String newDocContents = "from snippet7 import Foo\n" +
                "a\n" +
                "a\n" +
                "a\n" +
                "a\n" +
                "class TestIt(object):\n" +
                "";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateContentsInMiddle3() throws Exception {
        String docContents = "from snippet7 import Foo\n" +
                "class TestIt(object):\n" +
                "class TestIt(object):\n" +
                "class TestIt(object):\n" +
                "";
        String newDocContents = "from snippet7 import Foo\n" +
                "class TestIt(object):\n" +
                "a\n" +
                "a\n" +
                "a\n" +
                "a\n" +
                "class TestIt(object):\n" +
                "class TestIt(object):\n" +
                "";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateContentsInMiddle4() throws Exception {
        String docContents = "from snippet7 import Foo\n" +
                "class TestIt(object):\n" +
                "class TestIt(object):\n" +
                "class TestIt(object):\n" +
                "";
        String newDocContents = "from snippet7 import Foo\n" +
                "class TestIt(object):\n" +
                "a\n" +
                "class TestIt(object):\n" +
                "class TestIt(object):\n" +
                "";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }

    public void testUpdateContentsInMiddle5() throws Exception {
        String docContents = "from snippet7 import Foo\n" +
                "class TestIt(object):\n" +
                "class TestIt(object):\n" +
                "a\n" +
                "class TestIt(object):\n" +
                "";
        String newDocContents = "from snippet7 import Foo\n" +
                "class TestIt(object):\n" +
                "a\n" +
                "class TestIt(object):\n" +
                "class TestIt(object):\n" +
                "";
        String endLineDelimiter = "\n";
        IDocument doc = new Document(docContents);
        DocUtils.updateDocRangeWithContents(doc, docContents, newDocContents, endLineDelimiter);
        assertEquals(newDocContents, doc.get());
    }
}
