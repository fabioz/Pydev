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

package org.python.pydev.refactoring.tests.codegenerator.generateproperties;

import java.util.ArrayList;

import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;

public class MockupGeneratePropertiesConfig {

    private int classSelection;

    private ArrayList<Integer> attributeSelection;

    private int methodOffsetStrategy;

    private int propertyOffsetStrategy;

    private ArrayList<Integer> methodSelection;

    private int accessModifier;

    public MockupGeneratePropertiesConfig() {
        this.classSelection = 0;
        this.methodOffsetStrategy = IOffsetStrategy.AFTERINIT;
        this.propertyOffsetStrategy = IOffsetStrategy.AFTERINIT;
        this.attributeSelection = new ArrayList<Integer>();
        this.methodSelection = new ArrayList<Integer>();
        attributeSelection.add(0);
        methodSelection.add(0);
        this.accessModifier = 0;
    }

    public ArrayList<Integer> getAttributeSelection() {
        return attributeSelection;
    }

    public int getClassSelection() {
        return classSelection;
    }

    public int getMethodOffsetStrategy() {
        return methodOffsetStrategy;
    }

    public ArrayList<Integer> getMethodSelection() {
        return methodSelection;
    }

    public int getPropertyOffsetStrategy() {
        return propertyOffsetStrategy;
    }

    public int getAccessModifier() {
        return accessModifier;
    }

}
