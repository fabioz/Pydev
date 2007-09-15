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
