/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.string.SelectionKeeper;

public class SelectionKeeperTest extends TestCase {

    private static final class SelectionProvider implements ISelectionProvider {
        private TextSelection sel;

        private SelectionProvider() {
        }

        @Override
        public void setSelection(ISelection selection) {
            sel = (TextSelection) selection;
        }

        @Override
        public void removeSelectionChangedListener(ISelectionChangedListener listener) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public ISelection getSelection() {
            return sel;
        }

        @Override
        public void addSelectionChangedListener(ISelectionChangedListener listener) {
            throw new RuntimeException("Not implemented");
        }
    }

    public void testSelectionKeeper2() throws Exception {
        Document doc = new Document();
        final String initial = "" +
                "class Bar(object ): \n" +
                "    pass" +
                "";

        doc.set(initial);

        PySelection ps = new PySelection(doc, 0, 20);
        SelectionKeeper keeper = new SelectionKeeper(ps);

        final String finalStr = "" +
                "class Bar(object): \n" +
                "    pass" +
                "";

        doc.set(finalStr);
        final SelectionProvider selectionProvider = new SelectionProvider();
        keeper.restoreSelection(selectionProvider, doc);

        assertEquals(19, selectionProvider.sel.getOffset());
        assertEquals(0, selectionProvider.sel.getLength());
    }

    public void testSelectionKeeper() throws Exception {
        Document doc = new Document();
        final String initial = "" +
                "aaa\n" +
                "bbb\n" +
                "ccc\n" +
                "ddd\n" +
                "";

        doc.set(initial);

        PySelection ps = new PySelection(doc, 1, 2);
        final int initialOffset = ps.getAbsoluteCursorOffset();
        assertEquals(6, initialOffset);
        SelectionKeeper keeper = new SelectionKeeper(ps);

        doc.set("");

        final SelectionProvider selectionProvider = new SelectionProvider();
        keeper.restoreSelection(selectionProvider, doc);

        assertEquals(0, selectionProvider.sel.getOffset());
        assertEquals(0, selectionProvider.sel.getLength());

        doc.set("aaa\n");
        keeper.restoreSelection(selectionProvider, doc);
        assertEquals(4, selectionProvider.sel.getOffset());
        assertEquals(0, selectionProvider.sel.getLength());

        doc.set("aaa");
        keeper.restoreSelection(selectionProvider, doc);
        assertEquals(2, selectionProvider.sel.getOffset());
        assertEquals(0, selectionProvider.sel.getLength());

        doc.set(initial);
        keeper.restoreSelection(selectionProvider, doc);
        assertEquals(initialOffset, selectionProvider.sel.getOffset());
        assertEquals(0, selectionProvider.sel.getLength());

    }
}
