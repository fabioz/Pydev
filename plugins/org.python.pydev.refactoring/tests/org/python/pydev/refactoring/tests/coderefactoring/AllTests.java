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
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.coderefactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.python.pydev.refactoring.tests.coderefactoring.extractlocal.ExtractLocalTestSuite;
import org.python.pydev.refactoring.tests.coderefactoring.extractmethod.ExtractMethodTestSuite;
import org.python.pydev.refactoring.tests.coderefactoring.inlinelocal.InlineLocalTestSuite;

public final class AllTests {
    /* Hide Constructor */
    private AllTests() {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Coderefactoring Tests");
        // $JUnit-BEGIN$
        suite.addTest(ExtractLocalTestSuite.suite());
        suite.addTest(ExtractMethodTestSuite.suite());
        suite.addTest(InlineLocalTestSuite.suite());
        // $JUnit-END$
        return suite;
    }

}
