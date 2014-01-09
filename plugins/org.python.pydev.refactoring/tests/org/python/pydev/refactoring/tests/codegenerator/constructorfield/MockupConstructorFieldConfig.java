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

package org.python.pydev.refactoring.tests.codegenerator.constructorfield;

import java.util.ArrayList;

import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;

public class MockupConstructorFieldConfig {

    private int classSelection;

    private ArrayList<Integer> attributeSelection;

    private int offsetStrategy;

    public MockupConstructorFieldConfig() {
        this.classSelection = 0;
        this.offsetStrategy = IOffsetStrategy.AFTERINIT;
        this.attributeSelection = new ArrayList<Integer>();
        attributeSelection.add(0);
    }

    public ArrayList<Integer> getAttributeSelection() {
        return attributeSelection;
    }

    public int getClassSelection() {
        return classSelection;
    }

    public int getOffsetStrategy() {
        return offsetStrategy;
    }
}
