/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.revisited.jython;

import java.io.IOException;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.codecompletion.shell.JythonShell;

public class JythonCompletionWithBuiltinsTestWorkbench extends AbstractJythonWorkbenchTests {

    private static JythonShell shell;

    public static void main(String[] args) {
        try {
            JythonCompletionWithBuiltinsTestWorkbench test = new JythonCompletionWithBuiltinsTestWorkbench();
            test.setUp();
            test.testPropertiesAccess();
            test.tearDown();

            junit.textui.TestRunner.run(JythonCompletionWithBuiltinsTestWorkbench.class);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

        //we don't want to start it more than once
        if (shell == null) {
            shell = new JythonShell();
        }
        AbstractShell.putServerShell(nature, AbstractShell.getShellId(), shell);

    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        super.tearDown();
        AbstractShell.putServerShell(nature, AbstractShell.getShellId(), null);
    }

    public void testCompleteImportBuiltin2() throws BadLocationException, IOException, Exception {

        String s;
        s = "from java.lang import Class\n" +
                "Class.";
        requestCompl(s, s.length(), -1, new String[] { "forName(string)" });
    }

    public void testCompleteImportBuiltin() throws BadLocationException, IOException, Exception {
        String s;
        s = "from java import ";
        requestCompl(s, s.length(), -1, new String[] { "lang", "math", "util" });
    }

    /**
     * Test related to: http://sourceforge.net/tracker/index.php?func=detail&aid=1560823&group_id=85796&atid=577329
     */
    public void testStaticAccess() throws BadLocationException, IOException, Exception {
        String s;
        s = "" +
                "from javax import swing \n" +
                "print swing.JFrame.";
        requestCompl(s, s.length(), -1, new String[] { "EXIT_ON_CLOSE" });
    }

    /**
     * Test related to https://sourceforge.net/tracker/?func=detail&atid=577329&aid=2723131&group_id=85796
     */
    public void testPropertiesAccess() throws Exception {
        String s;
        s = "" +
                "from java.lang.Boolean import TYPE\n" +
                "TYPE.";
        requestCompl(s, s.length(), -1, new String[] { "fields" });
    }

}
