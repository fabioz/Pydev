package org.python.pydev.dltk.console.codegen;

import java.io.File;

import junit.framework.TestCase;

public class PythonSnippetUtilsTest extends TestCase {

	public void testGetExecfileCommand() {
		assertEquals("execfile('')\n", PythonSnippetUtils.getExecfileCommand(new File("")));
		assertEquals("execfile('/path/with/single\\'quote')\n",
				PythonSnippetUtils.getExecfileCommand(new File("/path/with/single'quote")));
		assertEquals("execfile('/path/with/double\"quote')\n",
				PythonSnippetUtils.getExecfileCommand(new File("/path/with/double\"quote")));
		assertEquals("execfile('/path/with/space in name')\n",
				PythonSnippetUtils.getExecfileCommand(new File("/path/with/space in name")));
		assertEquals("execfile('c:\\\\win\\\\style\\\\path')\n",
				PythonSnippetUtils.getExecfileCommand(new File("c:\\win\\style\\path")));
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
