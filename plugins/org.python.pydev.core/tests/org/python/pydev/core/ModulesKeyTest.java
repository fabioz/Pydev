/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.io.File;

import junit.framework.TestCase;

public class ModulesKeyTest extends TestCase {

    public void testEquals() throws Exception {
        assertEquals(new ModulesKey("mod1", new File("test")), new ModulesKey("mod1", new File("test")));
        assertEquals(new ModulesKey("mod1", null), new ModulesKey("mod1", null));
        assertNotEquals(new ModulesKey("m", null), new ModulesKey("m2", null));

        //consider only the name
        assertEquals(new ModulesKey("m", null), new ModulesKey("m", new File("test")));
    }

    private void assertNotEquals(ModulesKey modulesKey, ModulesKey modulesKey2) {
        assertFalse(modulesKey.equals(modulesKey2));
    }
}
