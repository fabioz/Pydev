/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.shared_core.structure.Tuple;

public class AbstractDebugTargetTest extends TestCase {

    public void testMatcher() throws Exception {
        Tuple<String, String> idAndReason = AbstractDebugTarget.getThreadIdAndReason("pid333_seq23\t108");
        assertTrue(idAndReason != null);
        assertEquals("pid333_seq23", idAndReason.o1);
        assertEquals("108", idAndReason.o2);

        idAndReason = AbstractDebugTarget.getThreadIdAndReason("pid3720_zad_seq1\t108");
        assertTrue(idAndReason != null);
        assertEquals("pid3720_zad_seq1", idAndReason.o1);
        assertEquals("108", idAndReason.o2);

        try {
            AbstractDebugTarget.getThreadIdAndReason("pid333_seq23\n108");
            fail("Expected errro");
        } catch (CoreException e) {
            assertEquals("Unexpected threadRun payload pid333_seq23\n108(unable to match)", e.getMessage());
        }
    }
}
