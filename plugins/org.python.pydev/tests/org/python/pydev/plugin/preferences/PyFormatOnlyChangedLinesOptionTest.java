/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.preferences;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.core.formatter.FormatStd.FormatterEnum;
import org.python.pydev.core.formatter.PyFormatterPreferences;
import org.python.pydev.core.preferences.PydevPrefs;

import junit.framework.TestCase;

/**
 * The "only changed lines" option is available for all the formatters but autopep8.
 * Uses the real preferences and preference page: needs OSGi and a display (the Tycho UI harness has both).
 */
public class PyFormatOnlyChangedLinesOptionTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PydevPrefs.getEclipsePreferences().putBoolean(PyFormatterPreferences.FORMAT_ONLY_CHANGED_LINES, true);
    }

    @Override
    protected void tearDown() throws Exception {
        PydevPrefs.getEclipsePreferences().remove(PyFormatterPreferences.FORMAT_ONLY_CHANGED_LINES);
        PydevPrefs.getEclipsePreferences().remove(PyFormatterPreferences.FORMATTER_STYLE);
        super.tearDown();
    }

    public void testOptionIsHonouredOnSave() {
        assertEquals(true, isHonoured(FormatterEnum.PYDEVF));
        assertEquals(true, isHonoured(FormatterEnum.BLACK));
        assertEquals(true, isHonoured(FormatterEnum.RUFF));
        assertEquals(false, isHonoured(FormatterEnum.AUTOPEP8));
    }

    public void testOptionIsShownInThePreferencePage() {
        assertEquals(true, isShown(FormatterEnum.PYDEVF));
        assertEquals(true, isShown(FormatterEnum.BLACK));
        assertEquals(true, isShown(FormatterEnum.RUFF));
        assertEquals(false, isShown(FormatterEnum.AUTOPEP8));
    }

    private static boolean isHonoured(FormatterEnum style) {
        PydevPrefs.getEclipsePreferences().put(PyFormatterPreferences.FORMATTER_STYLE, style.toString());
        return PyFormatterPreferences.getFormatOnlyChangedLines(null);
    }

    private static boolean isShown(FormatterEnum style) {
        PydevPrefs.getEclipsePreferences().put(PyFormatterPreferences.FORMATTER_STYLE, style.toString());
        Shell shell = new Shell();
        PyCodeFormatterPage page = new PyCodeFormatterPage();
        try {
            page.createControl(shell);
            Label option = findOption(shell);
            assertNotNull("Option not found in the page", option);
            return option.getVisible();
        } finally {
            page.dispose();
            shell.dispose();
        }
    }

    private static Label findOption(Control control) {
        if (control instanceof Label && ((Label) control).getText().contains("in changed lines")) {
            return (Label) control;
        }
        if (control instanceof Composite) {
            for (Control child : ((Composite) control).getChildren()) {
                Label found = findOption(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
