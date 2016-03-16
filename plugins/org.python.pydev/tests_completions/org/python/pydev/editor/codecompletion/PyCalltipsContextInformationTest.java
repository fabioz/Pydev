/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.Document;

import junit.framework.TestCase;

public class PyCalltipsContextInformationTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCalltips() throws Exception {
        PyCalltipsContextInformation info = new PyCalltipsContextInformation("(a, b)", 7);
        PyContextInformationValidator validator = new PyContextInformationValidator();

        Document doc = new Document("callIt(a, b)");
        validator.install(info, doc, -1);
        assertTrue(!validator.isContextInformationValid(0));
        assertTrue(!validator.isContextInformationValid(6));
        assertTrue(validator.isContextInformationValid(7));
        assertTrue(validator.isContextInformationValid(11));
        assertTrue(!validator.isContextInformationValid(12));

        doc = new Document("callIt(param1, param2");
        validator.install(info, doc, -1);
        assertTrue(validator.isContextInformationValid(12));

    }
}
