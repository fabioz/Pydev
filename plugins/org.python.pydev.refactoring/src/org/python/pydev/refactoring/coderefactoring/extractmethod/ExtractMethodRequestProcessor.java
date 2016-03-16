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

package org.python.pydev.refactoring.coderefactoring.extractmethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ParameterReturnDeduce;
import org.python.pydev.refactoring.coderefactoring.extractmethod.request.ExtractMethodRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;

public class ExtractMethodRequestProcessor implements IRequestProcessor<ExtractMethodRequest> {

    private int offsetStrategy;

    private String methodName;

    private AbstractScopeNode<?> scopeAdapter;

    private ModuleAdapter parsedSelection;

    private ParameterReturnDeduce deducer;

    private Map<String, String> renameMap;

    private List<String> parameterOrder;

    private ITextSelection selection;

    private AdapterPrefs adapterPrefs;

    public ExtractMethodRequestProcessor(AbstractScopeNode<?> scopeAdapter, ModuleAdapter parsedSelection,
            ModuleAdapter module, ITextSelection selection) {
        initProcessor(scopeAdapter, parsedSelection, module, selection);
    }

    public void initProcessor(AbstractScopeNode<?> scopeAdapter, ModuleAdapter parsedSelection, ModuleAdapter module,
            ITextSelection selection) {
        this.methodName = "pepticMethod";
        this.scopeAdapter = scopeAdapter;
        this.selection = selection;
        this.parsedSelection = parsedSelection;
        this.deducer = new ParameterReturnDeduce(this.scopeAdapter, selection, module);
        this.parameterOrder = new ArrayList<String>();
        parameterOrder.addAll(deducer.getParameters());
        this.renameMap = new TreeMap<String, String>();
        initRenamedMap();
        this.adapterPrefs = module.getAdapterPrefs();

        offsetStrategy = IOffsetStrategy.BEFORECURRENT;
    }

    private void initRenamedMap() {
        for (String variable : deducer.getParameters()) {
            this.renameMap.put(variable, variable);
        }

    }

    public AbstractScopeNode<?> getScopeAdapter() {
        return scopeAdapter;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getOffsetStrategy() {
        return offsetStrategy;
    }

    public void setOffsetStrategy(int offsetStrategy) {
        this.offsetStrategy = offsetStrategy;
    }

    @Override
    public List<ExtractMethodRequest> getRefactoringRequests() {
        List<ExtractMethodRequest> requests = new ArrayList<ExtractMethodRequest>();
        requests.add(new ExtractMethodRequest(this.methodName, this.selection, this.scopeAdapter, this.parsedSelection,
                parameterOrder, deducer.getReturns(), this.renameMap, this.offsetStrategy, this.adapterPrefs));
        return requests;
    }

    public ParameterReturnDeduce getDeducer() {
        return deducer;
    }

    public void setParameterMap(Map<String, String> renameMap) {
        this.renameMap = renameMap;
    }

    public void setParameterOrder(List<String> parameterOrder) {
        this.parameterOrder = parameterOrder;
    }

}
