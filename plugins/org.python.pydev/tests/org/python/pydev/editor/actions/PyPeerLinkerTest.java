/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.autoedit.TestIndentPrefs;

import junit.framework.TestCase;

/**
 * @author fabioz
 *
 */
public class PyPeerLinkerTest extends TestCase {

    public static void main(String[] args) {
        try {
            PyPeerLinkerTest test = new PyPeerLinkerTest();
            test.setUp();
            test.testParens();
            test.tearDown();
            junit.textui.TestRunner.run(PyPeerLinkerTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private PyPeerLinker peerLinker;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.peerLinker = new PyPeerLinker();
        this.peerLinker.setIndentPrefs(new TestIndentPrefs(true, 4));
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testLiteral() throws Exception {
        Document doc = new Document("");
        PySelection ps = new PySelection(doc, 0, 0);

        peerLinker.perform(ps, '\'', null);
        assertEquals("''", doc.get());
        assertEquals(1, peerLinker.getLinkOffset());
        assertEquals(0, peerLinker.getLinkLen());
        assertEquals(2, peerLinker.getLinkExitPos());
    }

    public void testLiteral2() throws Exception {
        Document doc = new Document("'");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        peerLinker.perform(ps, '\'', null);
        assertEquals("'", doc.get()); //Don't do anything (leave it to the auto indent)
        assertEquals(-1, peerLinker.getLinkOffset());
        assertEquals(-1, peerLinker.getLinkExitPos());
    }

    public void testLiteral3() throws Exception {
        Document doc = new Document("a");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());

        peerLinker.perform(ps, '\'', null);
        assertEquals("'a'", doc.get());
        assertEquals(1, peerLinker.getLinkOffset());
        assertEquals(1, peerLinker.getLinkLen());
        assertEquals(3, peerLinker.getLinkExitPos());
    }

    public void testLiteral4() throws Exception {
        Document doc = new Document("')'  ");
        PySelection ps = new PySelection(doc, 0, doc.getLength());

        peerLinker.perform(ps, '\'', null);
        assertEquals("')'  ''", doc.get());
        assertEquals(6, peerLinker.getLinkOffset());
        assertEquals(0, peerLinker.getLinkLen());
        assertEquals(7, peerLinker.getLinkExitPos());
    }

    public void testBrackets() throws Exception {
        Document doc = new Document("");
        PySelection ps = new PySelection(doc, 0, 0);

        peerLinker.perform(ps, '[', null);
        assertEquals("[]", doc.get());
        assertEquals(1, peerLinker.getLinkOffset());
        assertEquals(0, peerLinker.getLinkLen());
        assertEquals(2, peerLinker.getLinkExitPos());
    }

    public void testParens() throws Exception {
        Document doc = new Document("");
        PySelection ps = new PySelection(doc, 0, 0);

        peerLinker.perform(ps, '(', null);
        assertEquals("()", doc.get());
        assertEquals(1, peerLinker.getLinkOffset());
        assertEquals(0, peerLinker.getLinkLen());
        assertEquals(2, peerLinker.getLinkExitPos());
    }

    public void testParens2() throws Exception {
        Document doc = new Document(")");
        PySelection ps = new PySelection(doc, 0, 0);

        peerLinker.perform(ps, '(', null);
        assertEquals("()", doc.get());
        assertEquals(-1, peerLinker.getLinkOffset());
        assertEquals(-1, peerLinker.getLinkExitPos());
    }

    public void testParens3() throws Exception {
        String initial = "class Foo:\n" +
                "    def m1";
        Document doc = new Document(initial);
        PySelection ps = new PySelection(doc, 1, 10);

        peerLinker.perform(ps, '(', null);
        assertEquals(initial +
                "(self):", doc.get());
        assertEquals(26, peerLinker.getLinkOffset());
        assertEquals(28, peerLinker.getLinkExitPos());
    }

}
