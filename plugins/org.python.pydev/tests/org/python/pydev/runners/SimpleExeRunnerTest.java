/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jun 25, 2006
 * @author Fabio
 */
package org.python.pydev.runners;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * Extends CodeCompletionTestsBase so that we have the bundle set for getting the environment.
 */
public class SimpleExeRunnerTest extends CodeCompletionTestsBase {

    public void testIt() throws Exception {
        if (TestDependent.CYGWIN_CYGPATH_LOCATION != null) {
            SimpleExeRunner runner = new SimpleExeRunner();
            Tuple<String, String> tup = runner.runAndGetOutput(new String[] { TestDependent.CYGWIN_CYGPATH_LOCATION,
                    TestDependent.CYGWIN_CYGPATH_LOCATION }, null, null, null, "utf-8");
            assertEquals(TestDependent.CYGWIN_UNIX_CYGPATH_LOCATION, tup.o1.trim());
            assertEquals("", tup.o2);
        }
    }

    public void testIt2() throws Exception {
        if (TestDependent.CYGWIN_CYGPATH_LOCATION != null) {
            SimpleExeRunner runner = new SimpleExeRunner();
            List<String> ret = runner.convertToCygwinPath(TestDependent.CYGWIN_CYGPATH_LOCATION,
                    TestDependent.CYGWIN_CYGPATH_LOCATION, "c:\\foo");
            assertEquals(2, ret.size());
            ArrayList<String> expected = new ArrayList<String>();
            expected.add(TestDependent.CYGWIN_UNIX_CYGPATH_LOCATION);
            expected.add("/cygdrive/c/foo");
            assertEquals(expected, ret);
        }
    }

}
