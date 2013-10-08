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

package org.python.pydev.refactoring.tests.coderefactoring.extractlocal;

import junit.framework.Test;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

public class ExtractLocalTestSuite extends AbstractIOTestSuite {

    public ExtractLocalTestSuite(String name) {
        super(name);
    }

    public static Test suite() {
        String testdir = "tests" + I + "python" + I + "coderefactoring" + I + "extractlocal" + I;

        ExtractLocalTestSuite tests = new ExtractLocalTestSuite("Extract Local");
        tests.createTests(testdir);

        return tests;
    }

    @Override
    protected IInputOutputTestCase createTestCase(String testCaseName) {
        return new ExtractLocalTestCase(testCaseName);
    }
}
