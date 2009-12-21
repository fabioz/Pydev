package org.python.pydev.core.docutils;

import junit.framework.TestCase;

public class StringEscapeUtilsTest extends TestCase {

    
    public void testEscapeUnescapeXML() throws Exception {
        assertEquals("&lt;tett&quot;", StringEscapeUtils.escapeXml("<tett\""));
        assertEquals("<tett\"", StringEscapeUtils.unescapeXml("&lt;tett&quot;"));
        
    }
}
