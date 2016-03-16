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

package org.python.pydev.refactoring.coderefactoring.extractmethod.request;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.core.request.IExtractMethodRefactoringRequest;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class ExtractMethodRequest implements IRefactoringRequest, IExtractMethodRefactoringRequest {

    public final AbstractScopeNode<?> scopeAdapter;
    public final int offsetStrategy;
    public final String methodName;
    public final ModuleAdapter parsedSelection;
    public final List<String> parameters;
    public final List<String> returnVariables;
    public final Map<String, String> renamedVariables;
    public final ITextSelection selection;

    private final AdapterPrefs adapterPrefs;

    public ExtractMethodRequest(String methodName, ITextSelection selection, AbstractScopeNode<?> scopeAdapter,
            ModuleAdapter parsedSelection, List<String> callParameters, List<String> returnVariables,
            Map<String, String> renamedVariables, int offsetStrategy, AdapterPrefs adapterPrefs) {
        this.methodName = methodName;
        this.selection = selection;
        this.scopeAdapter = scopeAdapter;
        this.parsedSelection = parsedSelection;
        this.offsetStrategy = offsetStrategy;

        this.parameters = callParameters;
        this.returnVariables = returnVariables;
        this.renamedVariables = renamedVariables;
        this.adapterPrefs = adapterPrefs;
    }

    @Override
    public IASTNodeAdapter<? extends SimpleNode> getOffsetNode() {
        IASTNodeAdapter<? extends SimpleNode> offsetNode = scopeAdapter;
        while (offsetNode instanceof FunctionDefAdapter) {
            offsetNode = offsetNode.getParent();
        }

        return offsetNode;
    }

    @Override
    public AdapterPrefs getAdapterPrefs() {
        return adapterPrefs;
    }

    @Override
    public AbstractScopeNode<?> getScopeAdapter() {
        return this.scopeAdapter;
    }

}
