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
import java.util.List;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.codegenerator.constructorfield.request.ConstructorFieldRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;

public class MockupConstructorFieldRequestProcessor implements IRequestProcessor<ConstructorFieldRequest> {

    private ModuleAdapter module;

    private int classSelection;

    private int offsetStrategy;

    private List<Integer> attributeSelection;

    private MockupConstructorFieldRequestProcessor(ModuleAdapter module, int classSelection,
            List<Integer> attributeSelection, int offsetStrategy) {
        this.module = module;
        this.attributeSelection = attributeSelection;
        this.classSelection = classSelection;
        this.offsetStrategy = offsetStrategy;
    }

    public MockupConstructorFieldRequestProcessor(ModuleAdapter module, MockupConstructorFieldConfig config) {
        this(module, config.getClassSelection(), config.getAttributeSelection(), config.getOffsetStrategy());
    }

    public List<ConstructorFieldRequest> getRefactoringRequests() {
        IClassDefAdapter clazz = module.getClasses().get(classSelection);
        List<INodeAdapter> attributes = new ArrayList<INodeAdapter>();

        for (int index : attributeSelection) {
            attributes.add(clazz.getAttributes().get(index));
        }
        ConstructorFieldRequest req = new ConstructorFieldRequest(clazz, attributes, this.offsetStrategy,
                new AdapterPrefs("\n", new IGrammarVersionProvider() {

                    public int getGrammarVersion() throws MisconfigurationException {
                        return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
                    }
                }));

        List<ConstructorFieldRequest> requests = new ArrayList<ConstructorFieldRequest>();
        requests.add(req);

        return requests;

    }
}
