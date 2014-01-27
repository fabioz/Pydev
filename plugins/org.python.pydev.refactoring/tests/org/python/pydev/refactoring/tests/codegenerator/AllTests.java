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

package org.python.pydev.refactoring.tests.codegenerator;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.python.pydev.refactoring.tests.codegenerator.constructorfield.ConstructorFieldTestSuite;
import org.python.pydev.refactoring.tests.codegenerator.generateproperties.GeneratePropertiesTestSuite;
import org.python.pydev.refactoring.tests.codegenerator.overridemethods.OverrideMethodsTestSuite;

public final class AllTests {
    private AllTests() {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Codegenerator Tests");
        // $JUnit-BEGIN$
        suite.addTest(ConstructorFieldTestSuite.suite());
        suite.addTest(OverrideMethodsTestSuite.suite());
        suite.addTest(GeneratePropertiesTestSuite.suite());
        // $JUnit-END$
        return suite;
    }

}
