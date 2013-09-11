/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.autoedit.TestIndentPrefs;

public class PyShiftLeftTest extends TestCase {

    public void testShiftLeft1() throws Exception {
        Document doc = new Document("    def a(aa):\n" +
                "        pass\n" +
                "    \n");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        new PyShiftLeft().perform(ps, new TestIndentPrefs(true, 4));

        String expected = "def a(aa):\n" +
                "    pass\n" +
                "\n";
        assertEquals(expected, doc.get());
    }

    public void testShiftLeft2() throws Exception {
        Document doc = new Document("   def a(aa):\n" +
                "        pass\n" +
                "    \n");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        new PyShiftLeft().perform(ps, new TestIndentPrefs(true, 4));

        String expected = "def a(aa):\n" +
                "     pass\n" +
                " \n";
        assertEquals(expected, doc.get());
    }

    public void testShiftLeft3() throws Exception {
        Document doc = new Document("   def a(aa):\n" +
                "        pass\n" +
                "    bb\n");
        PySelection ps = new PySelection(doc, 0, 3, doc.getLength() - 2 - 3);
        new PyShiftLeft().perform(ps, new TestIndentPrefs(true, 4));

        String expected = "def a(aa):\n" +
                "     pass\n" +
                " bb\n";
        assertEquals(expected, doc.get());
    }

}
