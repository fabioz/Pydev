package org.python.pydev.editor.model;

import org.eclipse.core.runtime.Path;

import junit.framework.TestCase;

public class ItemPointerTest extends TestCase {

    public void testAsPortableString() throws Exception {
        ItemPointer pointer = new ItemPointer(Path.fromPortableString("c:/temp/a.py"), new Location(1, 2), new Location(3,4));
        String asPortableString = pointer.asPortableString();
        assertEquals(pointer, ItemPointer.fromPortableString(asPortableString));
        
        pointer = new ItemPointer(Path.fromPortableString("c:/temp/a.py"), new Location(1, 2), new Location(3,4), null, "zipLocation");
        asPortableString = pointer.asPortableString();
        assertEquals(pointer, ItemPointer.fromPortableString(asPortableString));
    }
}

