/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.io.File;

import org.python.pydev.shared_core.string.FastStringBuffer;

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

    public void testToIo() throws Exception {
        ModulesKey key = new ModulesKey("bar.a", null);
        FastStringBuffer buf = new FastStringBuffer();
        key.toIO(buf);
        ModulesKey newKey = ModulesKey.fromIO(buf.toString());
        assertTrue(newKey.getClass() == ModulesKey.class);
    }

    public void testToIo2() throws Exception {
        ModulesKey key = new ModulesKey("bar.a", new File("f.py"));
        FastStringBuffer buf = new FastStringBuffer();
        key.toIO(buf);
        ModulesKey newKey = ModulesKey.fromIO(buf.toString());
        assertTrue(newKey.getClass() == ModulesKey.class);

        assertEquals(key.file, newKey.file);
    }

    public void testToIo3() throws Exception {
        ModulesKeyForZip key = new ModulesKeyForZip("bar.a", new File("f.py"), "ra", true);

        FastStringBuffer buf = new FastStringBuffer();
        key.toIO(buf);
        ModulesKeyForZip newKey = (ModulesKeyForZip) ModulesKey.fromIO(buf.toString());
        assertTrue(newKey.getClass() == ModulesKeyForZip.class);

        assertEquals(key.file, newKey.file);
        assertEquals(key.zipModulePath, "ra");
    }

    public void testToIo4() throws Exception {
        ModulesKeyForZip key = new ModulesKeyForZip("bar.a", new File("f.py"), "", true);

        FastStringBuffer buf = new FastStringBuffer();
        key.toIO(buf);
        ModulesKeyForZip newKey = (ModulesKeyForZip) ModulesKey.fromIO(buf.toString());
        assertTrue(newKey.getClass() == ModulesKeyForZip.class);

        assertEquals(key.file, newKey.file);
        assertEquals(key.zipModulePath, "");
    }

}
