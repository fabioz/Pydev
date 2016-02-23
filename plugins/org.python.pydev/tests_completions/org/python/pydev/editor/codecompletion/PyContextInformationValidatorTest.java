/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.Document;

import junit.framework.TestCase;

public class PyContextInformationValidatorTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIt() throws Exception {
        PyContextInformationValidator validator = new PyContextInformationValidator();
        assertEquals(1, validator.getCurrentParameter(new Document("m1(a,b)\n"), 3, 5, ",", "", true));
        assertEquals(1, validator.getCurrentParameter(new Document("m1('',b)\n"), 3, 6, ",", "", true));
        assertEquals(1, validator.getCurrentParameter(new Document("m1('''(''',b)\n"), 3, 11, ",", "", true));
    }

}
