/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.parser.IPyParser;
import org.python.pydev.shared_core.editor.IBaseEditor;
import org.python.pydev.shared_core.model.IModelListener;
import org.python.pydev.shared_core.model.ISimpleNode;

public class PyParserEditorIntegrationTest extends TestCase {

    static class PyEditStub implements IPyEdit {
        public IDocument doc;
        public int parserChanged;
        private Map<String, Object> cache = new HashMap<String, Object>();
        private PydevFileEditorInputStub pydevFileEditorInputStub;

        public PyEditStub(IDocument doc, PydevFileEditorInputStub pydevFileEditorInputStub) {
            this.doc = doc;
            this.pydevFileEditorInputStub = pydevFileEditorInputStub;
        }

        public IEditorInput getEditorInput() {
            return pydevFileEditorInputStub;
        }

        public IPythonNature getPythonNature() {
            return new PythonNatureStub();
        }

        public void setParser(IPyParser parser) {
            throw new RuntimeException("Not implemented");
        }

        public void parserChanged(ISimpleNode root, IAdaptable file, IDocument doc, long docModificationStamp) {
            this.parserChanged += 1;
        }

        public void parserError(Throwable error, IAdaptable file, IDocument doc) {
            throw new RuntimeException("Not implemented");
        }

        public Map<String, Object> getCache() {
            return this.cache;
        }

        @Override
        public Object getAdapter(Class adapter) {
            return null;
        }

        public boolean hasSameInput(IBaseEditor edit) {
            if (this == edit) {
                throw new RuntimeException(
                        "Cannot compare when it's the same... it should change the document in this case");
            }
            if (edit.getEditorInput() == getEditorInput()) {
                return true;
            }
            return false;
        }

        public IDocument getDocument() {
            return doc;
        }

        public void setDocument(IDocument doc) {
            this.doc = doc;
        }

        public void setInput(PydevFileEditorInputStub input) {
            this.pydevFileEditorInputStub = input;
        }

        public void setStatusLineErrorMessage(String msg) {
            throw new RuntimeException("Not implemented");
        }

        public IGrammarVersionProvider getGrammarVersionProvider() {
            return this.getPythonNature();
        }

        public IIndentPrefs getIndentPrefs() {
            throw new RuntimeException("Not implemented");
        }

        public Object getFormatStd() {
            throw new RuntimeException("Not implemented");
        }

        public void addModelListener(IModelListener modelListener) {
            throw new RuntimeException("Not implemented");
        }

        public void removeModelListener(IModelListener modelListener) {
            throw new RuntimeException("Not implemented");
        }
    }

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
        PreferenceStore preferences = new PreferenceStore();
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
        PreferenceStore preferences = new PreferenceStore();
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
        PreferenceStore preferences = new PreferenceStore();
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
