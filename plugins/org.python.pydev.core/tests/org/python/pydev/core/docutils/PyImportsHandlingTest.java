/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import java.util.Iterator;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.shared_core.SharedCorePlugin;

public class PyImportsHandlingTest extends TestCase {

    public static void main(String[] args) {
        try {
            PyImportsHandlingTest test = new PyImportsHandlingTest();
            test.setUp();
            test.testPyImportHandling5();
            test.tearDown();
            junit.textui.TestRunner.run(PyImportsHandlingTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPyImportHandling() throws Exception {
        Document doc = new Document("from xxx import yyy");
        PyImportsHandling importsHandling = new PyImportsHandling(doc);
        Iterator<ImportHandle> it = importsHandling.iterator();
        assertTrue(it.hasNext());
        ImportHandle next = it.next();
        assertEquals("from xxx import yyy", next.importFound);
        assertEquals(0, next.startFoundLine);
        assertEquals(0, next.endFoundLine);
        assertFalse(it.hasNext());
    }

    public void testPyImportHandling2() throws Exception {

        Document doc = new Document("from xxx import yyy\nfrom y import (a, \nb,\nc)");
        PyImportsHandling importsHandling = new PyImportsHandling(doc);
        Iterator<ImportHandle> it = importsHandling.iterator();
        assertTrue(it.hasNext());
        ImportHandle next = it.next();
        assertEquals("from xxx import yyy", next.importFound);

        assertEquals(0, next.startFoundLine);
        assertEquals(0, next.endFoundLine);
        assertTrue(it.hasNext());
        next = it.next();

        assertEquals("from y import (a, \nb,\nc)", next.importFound);
        assertEquals(1, next.startFoundLine);
        assertEquals(3, next.endFoundLine);

        assertTrue(!it.hasNext());
    }

    public void testPyImportHandling3() throws Exception {

        Document doc = new Document("from ...a.b import b\nfrom xxx.bbb \\\n    import yyy\n");
        PyImportsHandling importsHandling = new PyImportsHandling(doc);
        Iterator<ImportHandle> it = importsHandling.iterator();
        assertTrue(it.hasNext());
        ImportHandle next = it.next();

        assertEquals("from ...a.b import b", next.importFound);
        assertEquals(0, next.startFoundLine);
        assertEquals(0, next.endFoundLine);

        next = it.next();
        assertEquals("from xxx.bbb \\\n    import yyy", next.importFound);
        assertEquals(1, next.startFoundLine);
        assertEquals(2, next.endFoundLine);

        assertTrue(!it.hasNext());

    }

    public void testPyImportHandling4() throws Exception {

        Document doc = new Document("class Foo:\n    from ...a.b import b\n    from xxx.bbb \\\n    import yyy\n");
        PyImportsHandling importsHandling = new PyImportsHandling(doc, false);
        Iterator<ImportHandle> it = importsHandling.iterator();
        assertTrue(it.hasNext());
        ImportHandle next = it.next();

        assertEquals("from ...a.b import b", next.importFound);
        assertEquals(1, next.startFoundLine);
        assertEquals(1, next.endFoundLine);

        next = it.next();
        assertEquals("from xxx.bbb \\\n    import yyy", next.importFound);
        assertEquals(2, next.startFoundLine);
        assertEquals(3, next.endFoundLine);

        assertTrue(!it.hasNext());

    }

    public void testPyImportHandling5() throws Exception {
        // This test fails when the line ends with ";", if the line
        // has no semi-colon the test passes (if you update the
        // both locations)
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        Document doc = new Document("import threading ;\n");
        PyImportsHandling importsHandling = new PyImportsHandling(doc, false);
        Iterator<ImportHandle> it = importsHandling.iterator();
        assertTrue(it.hasNext());
        ImportHandle next = it.next();

        assertEquals("import threading;", next.importFound);
        assertEquals(0, next.startFoundLine);
        assertEquals(0, next.endFoundLine);

        assertTrue(!it.hasNext());

    }

    public void testPyImportHandling6() throws Exception {
        Document doc = new Document("from a import\n");
        PyImportsHandling importsHandling = new PyImportsHandling(doc, false, true);
        Iterator<ImportHandle> it = importsHandling.iterator();
        assertTrue(it.hasNext());
        ImportHandle next = it.next();

        assertEquals("from a import", next.importFound);
        assertEquals(1, next.getImportInfo().size());
        assertEquals(0, next.getImportInfo().get(0).getImportedStr().size());
        assertEquals(0, next.startFoundLine);
        assertEquals(0, next.endFoundLine);

        assertTrue(!it.hasNext());

    }

    public void testPyImportHandling7() throws Exception {
        Document doc = new Document("import\n");
        PyImportsHandling importsHandling = new PyImportsHandling(doc, false, true);
        Iterator<ImportHandle> it = importsHandling.iterator();
        assertTrue(it.hasNext());
        ImportHandle next = it.next();

        assertEquals("import", next.importFound);
        assertEquals(1, next.getImportInfo().size());
        assertEquals(0, next.getImportInfo().get(0).getImportedStr().size());
        assertEquals(0, next.startFoundLine);
        assertEquals(0, next.endFoundLine);

        assertTrue(!it.hasNext());

    }

}
