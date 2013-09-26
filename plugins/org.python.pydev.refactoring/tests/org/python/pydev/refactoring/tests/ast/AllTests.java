/******************************************************************************
* Copyright (C) 2009  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.refactoring.tests.ast;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.python.pydev.refactoring.tests.ast.factory.PyAstFactoryTest;

public class AllTests {

    public static Test suite() {
        return new TestSuite(PyAstFactoryTest.class);
    }

}
