/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.rewriter;

import org.python.pydev.refactoring.tests.core.AbstractRewriterTestCase;

/**
 * @author Dennis Hunziker, Ueli Kistler
 */
public class RewriterTestCase extends AbstractRewriterTestCase {

    public RewriterTestCase(String name) {
        super(name);
    }

    public RewriterTestCase(String name, boolean ignoreEmptyLines) {
        super(name, ignoreEmptyLines);
    }

    public void runTest() throws Throwable {
        super.runRewriter();
        String expected = getExpected().replace("\r\n", "\n").replace("\r", "\n");
        //		System.out.println(">"+expected.replace(' ', '.').replace('\n', '|').replace('\r', '*')+"<");

        String generated = getGenerated();
        //        System.out.println(">"+generated.replace(' ', '.').replace('\n', '|').replace('\r', '*')+"<");
        assertEquals(expected, generated);
    }
}
