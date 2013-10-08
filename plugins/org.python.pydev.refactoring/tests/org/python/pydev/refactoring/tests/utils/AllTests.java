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

import junit.framework.Test;
import junit.framework.TestSuite;

public final class AllTests {
    private AllTests() {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for org.python.pydev.refactoring.utils");
        //$JUnit-BEGIN$
        suite.addTestSuite(TestUtilsTest.class);
        suite.addTestSuite(FileUtilsTest.class);
        //$JUnit-END$
        return suite;
    }

}
