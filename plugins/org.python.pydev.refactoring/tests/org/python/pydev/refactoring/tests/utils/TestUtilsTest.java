/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.tests.utils;

import java.util.List;

import org.python.pydev.refactoring.utils.TestUtils;
import org.python.pydev.refactoring.utils.TestUtils.Cursors;
import org.python.pydev.refactoring.utils.TestUtils.Marker;

import junit.framework.TestCase;

public class TestUtilsTest extends TestCase {

    public void testGetMarkers() {
        List<Marker> markers = TestUtils.getMarkers("x = 10 ## type int\ny = 'hello' ## type str");

        assertEquals(2, markers.size());

        Marker intMarker = markers.get(0);

        assertEquals("x = 10", intMarker.expr);
        assertEquals("int", intMarker.type);
        assertEquals(1, intMarker.beginLine);

        Marker strMarker = markers.get(1);

        assertEquals("y = 'hello'", strMarker.expr);
        assertEquals("str", strMarker.type);
        assertEquals(2, strMarker.beginLine);
    }

    public void testFindCursors() {
        Cursors findCursors = TestUtils.findCursors("##|x##|x##|x##|x##|");

        assertEquals("xxxx", findCursors.text);
        assertEquals(5, findCursors.positions.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((Integer) i, findCursors.positions.get(i));
        }
    }

}
