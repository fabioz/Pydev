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

package org.python.pydev.refactoring.tests.coderefactoring.extractmethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ParameterReturnDeduce;
import org.python.pydev.refactoring.coderefactoring.extractmethod.request.ExtractMethodRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;

public class MockupExtractMethodRequestProcessor implements IRequestProcessor<ExtractMethodRequest> {

    private AbstractScopeNode<?> scopeAdapter;

    private int offsetStrategy;

    private ModuleAdapter parsedSelection;

    private ParameterReturnDeduce deducer;

    private Map<String, String> renameMap;

    private ITextSelection selection;

    public MockupExtractMethodRequestProcessor(AbstractScopeNode<?> scopeAdapter, ITextSelection selection,
            ModuleAdapter parsedSelection, ParameterReturnDeduce deducer, Map<String, String> renameMap,
            int offsetStrategy) {

        this.scopeAdapter = scopeAdapter;
        this.selection = selection;
        this.parsedSelection = parsedSelection;
        this.offsetStrategy = offsetStrategy;
        this.deducer = deducer;
        this.renameMap = renameMap;
    }

    public List<ExtractMethodRequest> getRefactoringRequests() {
        List<ExtractMethodRequest> requests = new ArrayList<ExtractMethodRequest>();
        ExtractMethodRequest req = new ExtractMethodRequest("extracted_method", this.selection, this.scopeAdapter,
                this.parsedSelection, deducer.getParameters(), deducer.getReturns(), this.renameMap,
                this.offsetStrategy, new AdapterPrefs("\n", new IGrammarVersionProvider() {

                    public int getGrammarVersion() throws MisconfigurationException {
                        return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
                    }
                }));
        requests.add(req);

        return requests;

    }
}
