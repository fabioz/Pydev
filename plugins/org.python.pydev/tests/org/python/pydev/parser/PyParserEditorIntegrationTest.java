/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.codecompletion.revisited.PyEditStub;
import org.python.pydev.ast.codecompletion.revisited.PydevFileEditorInputStub;
import org.python.pydev.shared_core.preferences.InMemoryEclipsePreferences;

import junit.framework.TestCase;

public class PyParserEditorIntegrationTest extends TestCase {

    public static void main(String[] args) {
        try {
            PyParserEditorIntegrationTest test = new PyParserEditorIntegrationTest();
            test.setUp();
            test.testChangeInput();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParserEditorIntegrationTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        PyParserManager.setPyParserManager(null);
    }

    @Override
    protected void tearDown() throws Exception {
        PyParserManager.setPyParserManager(null);
    }

    public void testIntegration() throws Exception {
        IEclipsePreferences preferences = new InMemoryEclipsePreferences();
        PyParserManager pyParserManager = PyParserManager.getPyParserManager(preferences);

        Document doc = new Document();
        PyEditStub pyEdit = new PyEditStub(doc, new PydevFileEditorInputStub());
        pyParserManager.attachParserTo(pyEdit);
        checkParserChanged(pyEdit, 1);

        doc.replace(0, 0, "\r\ntest");
        checkParserChanged(pyEdit, 2);

        pyParserManager.attachParserTo(pyEdit);
        checkParserChanged(pyEdit, 3);

        doc.replace(0, 0, "\r\ntest"); //after this change, only 1 reparse should be asked, as the editor and doc is the same
        checkParserChanged(pyEdit, 4);

        pyParserManager.notifyEditorDisposed(pyEdit);
        doc.replace(0, 0, "\r\ntest"); //after this change, only 1 reparse should be asked, as the editor and doc is the same
        waitABit();
        assertEquals(4, pyEdit.parserChanged);

        assertEquals(0, pyParserManager.getParsers().size());
    }

    public void testDifferentEditorsSameInput() throws Exception {
        IEclipsePreferences preferences = new InMemoryEclipsePreferences();
        PyParserManager pyParserManager = PyParserManager.getPyParserManager(preferences);

        Document doc = new Document();
        PydevFileEditorInputStub input = new PydevFileEditorInputStub();
        //create them with the same input
        PyEditStub pyEdit1 = new PyEditStub(doc, input);
        PyEditStub pyEdit2 = new PyEditStub(doc, input);
        pyParserManager.attachParserTo(pyEdit1);
        checkParserChanged(pyEdit1, 1);

        doc.replace(0, 0, "\r\ntest");
        checkParserChanged(pyEdit1, 2);

        pyParserManager.attachParserTo(pyEdit2);
        checkParserChanged(pyEdit1, 3);
        checkParserChanged(pyEdit2, 1);

        IDocument doc2 = new Document();
        pyEdit2.setDocument(doc2);
        pyParserManager.notifyEditorDisposed(pyEdit1);

        checkParserChanged(pyEdit1, 3);
        checkParserChanged(pyEdit2, 2);

        assertNull(pyParserManager.getParser(pyEdit1));
        doc2.replace(0, 0, "\r\ntest");
        checkParserChanged(pyEdit1, 3);
        checkParserChanged(pyEdit2, 3);

        doc.replace(0, 0, "\r\ntest"); //no one's listening this one anymore
        waitABit();
        checkParserChanged(pyEdit1, 3);
        checkParserChanged(pyEdit2, 3);

        pyParserManager.notifyEditorDisposed(pyEdit2);
        assertNull(pyParserManager.getParser(pyEdit2));
        doc2.replace(0, 0, "\r\ntest"); //no one's listening this one anymore
        doc.replace(0, 0, "\r\ntest"); //no one's listening this one anymore
        waitABit();
        checkParserChanged(pyEdit1, 3);
        checkParserChanged(pyEdit2, 3);
        assertEquals(0, pyParserManager.getParsers().size());

    }

    public void testChangeInput() throws Exception {
        IEclipsePreferences preferences = new InMemoryEclipsePreferences();
        PyParserManager pyParserManager = PyParserManager.getPyParserManager(preferences);

        Document doc = new Document();
        PydevFileEditorInputStub input1 = new PydevFileEditorInputStub();
        PydevFileEditorInputStub input2 = new PydevFileEditorInputStub();
        //create them with the same input
        PyEditStub pyEdit1 = new PyEditStub(doc, input1);
        PyEditStub pyEdit2 = new PyEditStub(doc, input2);

        pyParserManager.attachParserTo(pyEdit1);
        pyParserManager.attachParserTo(pyEdit2);
        assertEquals(2, pyParserManager.getParsers().size());

        pyEdit2.setInput(input1);
        pyParserManager.attachParserTo(pyEdit2);
        assertEquals(1, pyParserManager.getParsers().size());

        pyEdit2.setInput(input2); //different input
        pyParserManager.attachParserTo(pyEdit2);
        assertEquals(2, pyParserManager.getParsers().size());

        pyParserManager.notifyEditorDisposed(pyEdit1);
        pyParserManager.notifyEditorDisposed(pyEdit2);
        assertEquals(0, pyParserManager.getParsers().size());
    }

    private void waitABit() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            synchronized (this) {
                this.wait(25);
            }
        }
    }

    Object lock = new Object();

    private void checkParserChanged(PyEditStub pyEdit, int expected) throws InterruptedException {
        for (int i = 0; i < 20 && pyEdit.parserChanged < expected; i++) {
            synchronized (lock) {
                lock.wait(250);
            }
        }
        assertEquals(expected, pyEdit.parserChanged);
    }
}
