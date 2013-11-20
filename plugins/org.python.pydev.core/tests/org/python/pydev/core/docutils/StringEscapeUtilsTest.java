/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import junit.framework.TestCase;

public class StringEscapeUtilsTest extends TestCase {

    public void testEscapeUnescapeXML() throws Exception {
        assertEquals("&lt;tett&quot;", StringEscapeUtils.escapeXml("<tett\""));
        assertEquals("<tett\"", StringEscapeUtils.unescapeXml("&lt;tett&quot;"));

    }
}
