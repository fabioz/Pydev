/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import junit.framework.TestCase;

public abstract class TestCaseUtils extends TestCase {

    public static final boolean DEBUG = false;

    public static void assertContentsEqual(String expected, String generated) {
        if (DEBUG) {
            System.out.println(generated);
        }
        assertEquals(org.python.pydev.shared_core.string.StringUtils.replaceNewLines(expected, "\n"), org.python.pydev.shared_core.string.StringUtils.replaceNewLines(generated, "\n"));
    }

}
