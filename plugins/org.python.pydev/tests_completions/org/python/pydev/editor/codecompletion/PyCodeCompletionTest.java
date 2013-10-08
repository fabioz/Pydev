/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.docutils.ImportsSelection;

/**
 * @author Fabio Zadrozny
 */
public class PyCodeCompletionTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyCodeCompletionTest.class);
    }

    IPyCodeCompletion completion;

    public void doTest(String s, String expected) {
        Document doc = new Document(s);
        int length = s.length();
        ImportInfo importsTipperStr = ImportsSelection.getImportsTipperStr(doc, length);
        String tipperStr = importsTipperStr.importsTipperStr;
        assertEquals(expected, tipperStr);
        if (tipperStr.length() > 0) {
            assertEquals(s.indexOf("from") != -1, importsTipperStr.hasFromSubstring);
            assertEquals(s.indexOf("import") != -1, importsTipperStr.hasImportSubstring);
        }
    }

    public void testIt() {
        completion = new PyCodeCompletion();

        doTest("from datetime import foo,\\\nbar\n", ""); //no \ in the prev line
        doTest("from datetime import (foo\nbar)\n", ""); //not actually an import (we're already after it)
        doTest("from datetime import foo\n", ""); //not actually an import (we're already in another line)

        doTest("from datetime import (\n    ", "datetime");
        doTest("from datetime import (\nbar, \n", "datetime");
        doTest("from datetime import foo,\\\nbar,\\\n", "datetime");
        doTest("from datetime import (\n, ", "datetime");

        doTest("from . import unittest , ", ".");

        doTest("from datetime import datetime, date, MINYEAR,", "datetime");
        doTest("from testl", " ");

        doTest("from .", ".");
        doTest("from ..", "..");

        doTest("import unittest.bar.f, os.path, sy", " ");
        doTest("import unittest.bar.f, a.", "a");
        doTest("import unittest.bar.f, ", " ");

        doTest("import unittest.", "unittest");
        doTest("import unittest", " ");
        doTest("import unittest.bar.f", "unittest.bar");

        doTest("from .. import ", "..");
        doTest("from ..bar import ", "..bar");

        doTest("from .. import unittest , ", "..");

        doTest("    from datetime import datetime, date, MINYEAR,", "datetime");
        doTest("no    from datetime import datetime, date, MINYEAR,", "");

        doTest("from datetime.datetime import ", "datetime.datetime");
        doTest("    from datetime.datetime import ", "datetime.datetime");

        doTest("from testlib import unittest , ", "testlib");
        doTest("    from testlib import unittest , ", "testlib");

        doTest("from datetime.datetime import to", "datetime.datetime");
        doTest("    from datetime.datetime import to", "datetime.datetime");

        doTest("from this space", "");
        doTest("from ", " ");

        doTest("nothere", "");
        doTest("from i import y\n\na=10", "");
        doTest("from i import (y)\n\na=10", "");
    }

}
