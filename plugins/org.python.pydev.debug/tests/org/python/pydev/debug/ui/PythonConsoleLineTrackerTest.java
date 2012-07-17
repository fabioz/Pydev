/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui;

import java.util.regex.Matcher;

import junit.framework.TestCase;

public class PythonConsoleLineTrackerTest extends TestCase {

    public void testFileMatch() throws Exception {
        Matcher matcher = PythonConsoleLineTracker.linePattern
                .matcher("File \"Y:\\test_python\\src\\mod1\\mod2\\test_it2.py\", line 45, in testAnotherCase");
        assertTrue(matcher.matches());
        String file = matcher.group(1);
        String fileName = matcher.group(2);
        String lineNumber = matcher.group(3);
        assertEquals("File", file);
        assertEquals("Y:\\test_python\\src\\mod1\\mod2\\test_it2.py", fileName);
        assertEquals("45", lineNumber);

        matcher = PythonConsoleLineTracker.linePattern
                .matcher("File \"/home/users/foo/test_it2.py\", line 45, in testAnotherCase");
        assertTrue(matcher.matches());
        fileName = matcher.group(2);
        lineNumber = matcher.group(3);
        assertEquals("/home/users/foo/test_it2.py", fileName);
        assertEquals("45", lineNumber);

    }
}
