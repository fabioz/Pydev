/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 19, 2006
 * @author Fabio
 */
package org.python.pydev.editor.actions;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;

public class PyUncommentTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testUncomment() throws Exception {
        Document doc = new Document("#a\n" +
                "#b");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 3), new PyUncomment().perform(ps));

        String expected = "a\n" +
                "b";
        assertEquals(expected, doc.get());

    }
}
