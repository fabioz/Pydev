package org.python.pydev.debug.model;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.Tuple;

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
        
        try{
            AbstractDebugTarget.getThreadIdAndReason("pid333_seq23\n108");
            fail("Expected errro");
        }catch(CoreException e){
            assertEquals("Unexpected threadRun payload pid333_seq23\n108(unable to match)", e.getMessage());
        }
    }
}
