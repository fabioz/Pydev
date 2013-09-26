/******************************************************************************
* Copyright (C) 2007-2012  IFS Institute for Software and others
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

import java.util.LinkedList;

import junit.framework.TestCase;

import org.python.pydev.refactoring.utils.StringUtils;

public class StringUtilsTest extends TestCase {

    public void testJoin() {
        assertEquals("a.b.c", StringUtils.join('.', l("a", "b", "c")));
        assertEquals("", StringUtils.join('.', l()));
        assertEquals("a", StringUtils.join('.', l("a")));
    }

    public void testCapitalize() {
        assertEquals("Changed", StringUtils.capitalize("changed"));
        assertEquals("Unchanged", StringUtils.capitalize("Unchanged"));
        assertEquals("TwoWords", StringUtils.capitalize("twoWords"));
        assertEquals("A", StringUtils.capitalize("a"));
        assertEquals("", StringUtils.capitalize(""));
    }

    public void testStripParts() {
        String input = "foo/bar/baz/xyz.py";

        assertEquals("foo/bar/baz", StringUtils.stripParts(input, 1));
        assertEquals("foo/bar", StringUtils.stripParts(input, 2));
        assertEquals("foo", StringUtils.stripParts(input, 3));
        assertEquals("", StringUtils.stripParts(input, 4));
    }

    private static LinkedList<String> l(String... array) {
        LinkedList<String> list = new LinkedList<String>();
        for (String item : array) {
            list.add(item);
        }

        return list;
    }

}
