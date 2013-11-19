/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole;

import java.util.List;

import org.python.pydev.debug.newconsole.prefs.ColorManager;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_interactive_console.console.ui.IConsoleStyleProvider;
import org.python.pydev.shared_interactive_console.console.ui.ScriptStyleRange;

import junit.framework.TestCase;

/**
 * @author fabioz
 *
 */
public class PydevConsoleTest extends TestCase {

    public void testPydevConsoleColors() throws Exception {
        IConsoleStyleProvider provider = new ConsoleStyleProvider();

        //foo start yellow bar endyellow xxx
        Tuple<List<ScriptStyleRange>, String> tup = provider.createInterpreterErrorStyle(
                "foo\u001B[0;33mbar\u001B[0mxxx", 0);
        List<ScriptStyleRange> ranges = tup.o1;
        assertEquals(3, ranges.size());
        assertEquals(null, ranges.get(0).foreground);
        assertEquals(ColorManager.dimYellow, ranges.get(1).foreground.getRGB());
        assertEquals(null, ranges.get(2).foreground);
    }
}
