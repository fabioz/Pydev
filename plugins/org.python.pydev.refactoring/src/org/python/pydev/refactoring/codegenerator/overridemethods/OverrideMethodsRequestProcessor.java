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
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.overridemethods;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.codegenerator.overridemethods.request.OverrideMethodsRequest;
import org.python.pydev.refactoring.core.model.overridemethods.ClassTreeNode;
import org.python.pydev.refactoring.core.model.overridemethods.FunctionTreeNode;
import org.python.pydev.refactoring.core.request.IRequestProcessor;

public class OverrideMethodsRequestProcessor implements IRequestProcessor<OverrideMethodsRequest> {

    private Object[] checked;

    private int insertionPoint;

    private boolean generateMethodComments;

    private IClassDefAdapter origin;

    private AdapterPrefs adapterPrefs;

    public OverrideMethodsRequestProcessor(IClassDefAdapter origin, AdapterPrefs adapterPrefs) {
        checked = new Object[0];
        insertionPoint = IOffsetStrategy.AFTERINIT;
        this.origin = origin;
        this.adapterPrefs = adapterPrefs;
    }

    public void setCheckedElements(Object[] checked) {
        this.checked = checked;
    }

    public void setInsertionPoint(int strategy) {
        this.insertionPoint = strategy;
    }

    public void setGenerateMethodComments(boolean value) {
        this.generateMethodComments = value;
    }

    @Override
    public List<OverrideMethodsRequest> getRefactoringRequests() {
        List<OverrideMethodsRequest> requests = new ArrayList<OverrideMethodsRequest>();

        for (ClassTreeNode clazz : getClasses()) {
            for (FunctionDefAdapter method : getMethods(clazz)) {
                requests.add(new OverrideMethodsRequest(origin, insertionPoint, method, generateMethodComments, clazz
                        .getAdapter().getName(), adapterPrefs));
            }
        }

        return requests;
    }

    private List<FunctionDefAdapter> getMethods(ClassTreeNode parent) {
        List<FunctionDefAdapter> methods = new ArrayList<FunctionDefAdapter>();

        for (Object obj : checked) {
            if (obj instanceof FunctionTreeNode) {
                FunctionTreeNode method = (FunctionTreeNode) obj;
                if (method.getParent() == parent) {
                    methods.add(method.getAdapter());
                }
            }
        }

        return methods;
    }

    private List<ClassTreeNode> getClasses() {
        List<ClassTreeNode> classes = new ArrayList<ClassTreeNode>();

        for (Object obj : checked) {
            if (obj instanceof ClassTreeNode) {
                classes.add((ClassTreeNode) obj);
            }
        }

        return classes;
    }

}
