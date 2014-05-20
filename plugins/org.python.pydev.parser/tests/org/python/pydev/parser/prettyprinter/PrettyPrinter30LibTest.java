/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 27, 2006
 */
package org.python.pydev.parser.prettyprinter;

import java.io.File;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.TestDependent;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;

public class PrettyPrinter30LibTest extends AbstractPrettyPrinterTestBase {

    private static boolean MAKE_COMPLETE_PARSE = true;

    public static void main(String[] args) {
        try {
            //            DEBUG = true;
            junit.textui.TestRunner.run(PrettyPrinter30LibTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        prefs = new PrettyPrinterPrefsV2("\n", "    ", versionProvider);
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
    }

    public void testConstruct1() throws Exception {
        String contents = ""
                + "def func():\n"
                + "    encoded += (aaa[10] + #comment\n"
                + "                bbb[20]\n"
                + "               )\n"
                + "";
        parseAndPrettyPrintFile(new File("temp.py"), contents);
    }

    public void testConstruct2() throws Exception {
        String contents = ""
                + "del threading, local        # Don't contaminate the namespace\n"
                + "";
        parseAndPrettyPrintFile(new File("temp.py"), contents);
    }

    public void testOnCompleteLib() throws Exception {
        File file = new File(TestDependent.PYTHON_30_LIB);
        if (MAKE_COMPLETE_PARSE) {
            parseAndReparsePrettyPrintedFilesInDir(file);
        } else {
            System.out.println("COMPLETE LIB NOT PARSED!");
        }
    }

}
