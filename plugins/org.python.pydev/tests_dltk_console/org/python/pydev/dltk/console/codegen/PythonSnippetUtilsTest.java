/*******************************************************************************
 * Copyright (C) 2011, 2013  Jonah Graham and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
 *     Fabio Zadrozny <fabiofz@gmail.com>    - ongoing maintenance
 *******************************************************************************/
package org.python.pydev.dltk.console.codegen;

import java.io.File;

import junit.framework.TestCase;

import org.python.pydev.shared_interactive_console.console.codegen.PythonSnippetUtils;

public class PythonSnippetUtilsTest extends TestCase {

    public void testGetRunfileCommand() {
        assertEquals("runfile('')\n", PythonSnippetUtils.getRunfileCommand(new File("")));

        assertEquals(platformDependentStr("runfile('/path/with/single\\'quote')\n"),
                PythonSnippetUtils.getRunfileCommand(new File("/path/with/single'quote")));
        assertEquals(platformDependentStr("runfile('/path/with/double\"quote')\n"),
                PythonSnippetUtils.getRunfileCommand(new File("/path/with/double\"quote")));
        assertEquals(platformDependentStr("runfile('/path/with/space in name')\n"),
                PythonSnippetUtils.getRunfileCommand(new File("/path/with/space in name")));
        assertEquals(platformDependentStr("runfile('c:\\\\win\\\\style\\\\path')\n"),
                PythonSnippetUtils.getRunfileCommand(new File("c:\\win\\style\\path")));
    }

    protected String platformDependentStr(String s) {
        if (File.separatorChar != '/') {
            return org.python.pydev.shared_core.string.StringUtils
                    .replaceAll(s, "/", (File.separator + File.separator));
        } else {
            return s;
        }
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
