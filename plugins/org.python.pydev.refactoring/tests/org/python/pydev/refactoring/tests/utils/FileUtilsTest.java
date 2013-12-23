/******************************************************************************
* Copyright (C) 2007-2013  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.tests.utils;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.python.pydev.core.TestDependent;
import org.python.pydev.refactoring.utils.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;

public class FileUtilsTest extends TestCase {

    public void testRead() throws IOException {
        File file = new File(TestDependent.TEST_PYDEV_REFACTORING_PLUGIN_LOC + "/tests/python/utils/smallfile.txt");
        String contents = FileUtils.read(file);
        contents = StringUtils.replaceNewLines(contents, "\n");
        assertEquals("This\nis\na\nsmall\ntext\nfile.", contents);
    }

    public void testReadNonexistent() {
        File file = new File("thisfiledoesnotexistsowetrytoreadittoseeifanioexceptionisthrown");
        try {
            FileUtils.read(file);
            fail("IOException expected (tongue twister)");
        } catch (IOException e) {
        }
    }

    public void testStripExtension() {
        assertEquals("foo", FileUtils.stripExtension("foo.bar"));
        assertEquals("foo.bar", FileUtils.stripExtension("foo.bar.baz"));
        assertEquals("/path/to/file", FileUtils.stripExtension("/path/to/file.txt"));
        assertEquals("", FileUtils.stripExtension(".foo"));
        assertEquals(".foo", FileUtils.stripExtension(".foo.foo"));
        assertEquals("", FileUtils.stripExtension("."));
        assertEquals(".", FileUtils.stripExtension(".."));
    }

}
