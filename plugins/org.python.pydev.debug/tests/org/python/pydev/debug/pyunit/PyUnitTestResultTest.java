/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import org.python.pydev.editor.model.ItemPointer;

import junit.framework.TestCase;

public class PyUnitTestResultTest extends TestCase {

    public void testOpen() throws Exception {
        String contents = getContents();
        ItemPointer itemPointer = PyUnitTestResult.getItemPointer(null, contents, "Test.test_Create");
        assertEquals(7, itemPointer.start.line);
        itemPointer = PyUnitTestResult.getItemPointer(null, contents, "Test.test_Rename");
        assertEquals(44, itemPointer.start.line);
    }

    private String getContents() {
        return ""
                + "class Test(unittest.TestCase):\n"
                + //0
                "\n"
                + "    def setUp(self):\n"
                + "        import data_file\n"
                + "        self.CreateDataDir(data_file, True)\n"
                + "\n"
                + "\n"
                + "    def test_Create(self):\n"
                + //7
                "        import os.path\n" +
                "\n" +
                "        toolbox = coilib50.toolbox.ToolBox()\n" +
                "\n"
                + "        # Create: normal use\n"
                + "        self.assert_(not os.path.isfile(self.GetDataFilename('alpha.txt')))\n"
                + "        toolbox.CreateFile(self.GetDataFilename('alpha.txt'))\n"
                + "        self.assert_(os.path.isfile(self.GetDataFilename('alpha.txt')))\n" +
                "\n"
                + "        # Create: FailException if the file already exists\n" +
                "        self.assertRaises(\n"
                + "            coilib50.toolbox.FailException,\n" +
                "            toolbox.CreateFile,\n"
                + "            self.GetDataFilename('alpha.txt'))\n" +
                "\n"
                + "        # Create: FailException if the path does not exists\n" +
                "        self.assertRaises(\n"
                + "            coilib50.toolbox.FailException,\n" +
                "            toolbox.CreateFile,\n"
                + "            self.GetDataFilename('foo/bar/alpha.txt'))\n" +
                "\n"
                + "        # Create: on_fail=SKIP\n" +
                "        self.assertEqual(\n"
                + "            toolbox.CreateFile(\n" +
                "                self.GetDataFilename('alpha.txt'),\n"
                + "                on_fail=coilib50.toolbox.SKIP),\n" +
                "            coilib50.toolbox.SKIP)\n" +
                "\n"
                + "        # Create: on_fail=IGNORE\n" +
                "        self.assertEqual(\n"
                + "            toolbox.CreateFile(\n" +
                "                self.GetDataFilename('alpha.txt'),\n"
                + "                on_fail=coilib50.toolbox.IGNORE),\n" +
                "            coilib50.toolbox.IGNORE)\n"
                + "\n"
                + "\n"
                + "    def test_Rename(self):\n"
                + //44
                "        import os.path\n" +
                "\n" +
                "        toolbox = coilib50.toolbox.ToolBox()\n" +
                "\n"
                + "        # Rename: normal use\n" +
                "        self.assert_(os.path.isfile('data_file/beta.txt'))\n"
                + "        self.assert_(not os.path.isfile('data_file/delta.txt'))\n" +
                "\n"
                + "        toolbox.RenameFile('data_file/beta.txt', 'data_file/delta.txt')\n" +
                "\n";
    }
}
