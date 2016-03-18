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

package org.python.pydev.refactoring.codegenerator.overridemethods.request;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class OverrideMethodsRequest implements IRefactoringRequest {

    public final FunctionDefAdapter method;
    public final int offsetStrategy;
    public final boolean generateMethodComments;

    private IClassDefAdapter classAdapter;
    private String baseClassName;
    private AdapterPrefs adapterPrefs;

    public OverrideMethodsRequest(IClassDefAdapter classAdapter, int offsetStrategy, FunctionDefAdapter method,
            boolean generateMethodComments, String baseClassName, AdapterPrefs adapterPrefs) {
        this.baseClassName = baseClassName;
        this.classAdapter = classAdapter;
        this.offsetStrategy = offsetStrategy;
        this.method = method;
        this.generateMethodComments = generateMethodComments;
        this.adapterPrefs = adapterPrefs;
    }

    @Override
    public IASTNodeAdapter<? extends SimpleNode> getOffsetNode() {
        return classAdapter;
    }

    public String getBaseClassName() {
        return getOffsetNode().getModule().getBaseContextName(this.classAdapter, baseClassName);
    }

    @Override
    public AdapterPrefs getAdapterPrefs() {
        return adapterPrefs;
    }
}
