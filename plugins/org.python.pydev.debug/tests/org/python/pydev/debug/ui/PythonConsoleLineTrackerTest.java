package org.python.pydev.debug.ui;

import java.util.regex.Matcher;

import junit.framework.TestCase;

public class PythonConsoleLineTrackerTest extends TestCase{

    public void testFileMatch() throws Exception{
        Matcher matcher = PythonConsoleLineTracker.linePattern.matcher("File \"Y:\\test_python\\src\\mod1\\mod2\\test_it2.py\", line 45, in testAnotherCase");
        assertTrue(matcher.matches());
        String fileName = matcher.group(1);
        String lineNumber = matcher.group(2);
        assertEquals("Y:\\test_python\\src\\mod1\\mod2\\test_it2.py", fileName);
        assertEquals("45", lineNumber);

        matcher = PythonConsoleLineTracker.linePattern.matcher("File \"/home/users/foo/test_it2.py\", line 45, in testAnotherCase");
        assertTrue(matcher.matches());
        fileName = matcher.group(1);
        lineNumber = matcher.group(2);
        assertEquals("/home/users/foo/test_it2.py", fileName);
        assertEquals("45", lineNumber);
        
    }
}
