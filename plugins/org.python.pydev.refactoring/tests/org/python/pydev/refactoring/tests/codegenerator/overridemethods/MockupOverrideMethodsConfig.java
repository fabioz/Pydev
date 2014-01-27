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

package org.python.pydev.refactoring.tests.codegenerator.overridemethods;

import java.util.ArrayList;

import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;

public class MockupOverrideMethodsConfig {

    private String classSelection;

    private ArrayList<Integer> methodSelection;

    private int offsetStrategy;

    private int editClass;

    public MockupOverrideMethodsConfig() {
        this.classSelection = "";
        this.offsetStrategy = IOffsetStrategy.AFTERINIT;
        this.methodSelection = new ArrayList<Integer>();
        methodSelection.add(0);
        this.editClass = 0;
    }

    public ArrayList<Integer> getMethodSelection() {
        return methodSelection;
    }

    public String getClassSelection() {
        return classSelection;
    }

    public int getOffsetStrategy() {
        return offsetStrategy;
    }

    public int getEditClass() {
        return editClass;
    }
}
