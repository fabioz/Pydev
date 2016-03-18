/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 19, 2006
 * @author Fabio
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;

import junit.framework.TestCase;

public class PyAddSingleBlockCommentTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testBlock() throws Exception {
        String s = "cc";

        Document doc = new Document(s);
        new PyAddSingleBlockComment(10, true).perform(new PySelection(doc, 0, 0, 0));
        assertEquals("#------ cc", doc.get());

        s = "    cc";

        doc = new Document(s);
        new PyAddSingleBlockComment(10, true).perform(new PySelection(doc, 0, 0, 0));
        assertEquals("    #-- cc", doc.get());

        doc = new Document("cc");
        new PyAddSingleBlockComment(10, false).perform(new PySelection(doc, 0, 0, 0));
        assertEquals("# cc -----", doc.get());

        doc = new Document("\tcc");
        new PyAddSingleBlockComment(12, false).perform(new PySelection(doc, 0, 0, 0));
        assertEquals("\t# cc ---", doc.get());

        doc = new Document("\tcc");
        new PyAddSingleBlockComment(12, true).perform(new PySelection(doc, 0, 0, 0));
        assertEquals("\t#---- cc", doc.get());

    }

}
