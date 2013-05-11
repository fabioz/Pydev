package org.python.pydev.dltk.console.codegen;

import java.io.File;

import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.shared_interactive_console.console.codegen.PythonSnippetUtils;

import junit.framework.TestCase;

public class PythonSnippetUtilsTest extends TestCase {

    public void testGetExecfileCommand() {
        assertEquals("execfile('')\n", PythonSnippetUtils.getExecfileCommand(new File("")));

        assertEquals(platformDependentStr("execfile('/path/with/single\\'quote')\n"),
                PythonSnippetUtils.getExecfileCommand(new File("/path/with/single'quote")));
        assertEquals(platformDependentStr("execfile('/path/with/double\"quote')\n"),
                PythonSnippetUtils.getExecfileCommand(new File("/path/with/double\"quote")));
        assertEquals(platformDependentStr("execfile('/path/with/space in name')\n"),
                PythonSnippetUtils.getExecfileCommand(new File("/path/with/space in name")));
        assertEquals(platformDependentStr("execfile('c:\\\\win\\\\style\\\\path')\n"),
                PythonSnippetUtils.getExecfileCommand(new File("c:\\win\\style\\path")));
    }

    protected String platformDependentStr(String s) {
        return org.python.pydev.shared_core.string.StringUtils.replaceAll(s, "/", (File.separator + File.separator));
    }

    public void testGetSingleQuotedString() {
        assertEquals("''", PythonSnippetUtils.getSingleQuotedString(""));
        assertEquals("' '", PythonSnippetUtils.getSingleQuotedString(" "));
        assertEquals("'\\''", PythonSnippetUtils.getSingleQuotedString("'"));
        assertEquals("'\\\\'", PythonSnippetUtils.getSingleQuotedString("\\"));
        assertEquals("'\\\\\\''", PythonSnippetUtils.getSingleQuotedString("\\'"));
    }

    public void testGetDoubleQuotedString() {
        assertEquals("\"\"", PythonSnippetUtils.getDoubleQuotedString(""));
        assertEquals("\" \"", PythonSnippetUtils.getDoubleQuotedString(" "));
        assertEquals("\"\\\"\"", PythonSnippetUtils.getDoubleQuotedString("\""));
        assertEquals("\"\\\\\"", PythonSnippetUtils.getDoubleQuotedString("\\"));
        assertEquals("\"\\\\\\\"\"", PythonSnippetUtils.getDoubleQuotedString("\\\""));
    }

}
