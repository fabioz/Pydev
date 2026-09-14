/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.formatter;

import org.python.pydev.core.formatter.FormatStd.FormatterEnum;

import junit.framework.TestCase;

public class PyFormatterPreferencesTest extends TestCase {

    public void testOnlyChangedLinesIsSupportedByFormattersThatFormatRanges() {
        assertTrue(PyFormatterPreferences.supportsFormatOnlyChangedLines(FormatterEnum.PYDEVF));
        assertTrue(PyFormatterPreferences.supportsFormatOnlyChangedLines(FormatterEnum.BLACK));
        assertTrue(PyFormatterPreferences.supportsFormatOnlyChangedLines(FormatterEnum.RUFF));
    }

    public void testOnlyChangedLinesIsNotSupportedByAutopep8() {
        assertFalse(PyFormatterPreferences.supportsFormatOnlyChangedLines(FormatterEnum.AUTOPEP8));
    }
}
