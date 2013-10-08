/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.model;

import org.eclipse.core.runtime.Path;
import org.python.pydev.shared_core.structure.Location;

import junit.framework.TestCase;

public class ItemPointerTest extends TestCase {

    public void testAsPortableString() throws Exception {
        ItemPointer pointer = new ItemPointer(Path.fromPortableString("c:/temp/a.py"), new Location(1, 2),
                new Location(3, 4));
        String asPortableString = pointer.asPortableString();
        assertEquals(pointer, ItemPointer.fromPortableString(asPortableString));

        pointer = new ItemPointer(Path.fromPortableString("c:/temp/a.py"), new Location(1, 2), new Location(3, 4),
                null, "zipLocation");
        asPortableString = pointer.asPortableString();
        assertEquals(pointer, ItemPointer.fromPortableString(asPortableString));
    }
}
