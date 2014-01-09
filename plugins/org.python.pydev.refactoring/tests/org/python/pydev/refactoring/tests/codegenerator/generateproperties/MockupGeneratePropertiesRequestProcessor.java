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
import java.util.List;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.adapters.PropertyTextAdapter;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;

public class MockupGeneratePropertiesRequestProcessor implements IRequestProcessor<GeneratePropertiesRequest> {

    private ModuleAdapter module;

    private int classSelection;

    private List<Integer> attributeSelection;

    private int methodOffsetStrategy;

    private int propertyOffsetStrategy;

    private List<Integer> methodSelection;

    private int accessModifier;

    private MockupGeneratePropertiesRequestProcessor(ModuleAdapter module, int classSelection,
            List<Integer> attributeSelection, int methodOffsetStrategy, int propertyOffsetStrategy,
            List<Integer> methodSelection, int accessModifier) {
        this.module = module;
        this.attributeSelection = attributeSelection;
        this.classSelection = classSelection;
        this.methodSelection = methodSelection;
        this.propertyOffsetStrategy = propertyOffsetStrategy;
        this.methodOffsetStrategy = methodOffsetStrategy;
        this.accessModifier = accessModifier;
    }

    public MockupGeneratePropertiesRequestProcessor(ModuleAdapter module, MockupGeneratePropertiesConfig config) {
        this(module, config.getClassSelection(), config.getAttributeSelection(), config.getMethodOffsetStrategy(),
                config.getPropertyOffsetStrategy(), config.getMethodSelection(), config.getAccessModifier());
    }

    public List<GeneratePropertiesRequest> getRefactoringRequests() {
        IClassDefAdapter clazz = module.getClasses().get(classSelection);

        List<INodeAdapter> attributes = new ArrayList<INodeAdapter>();
        for (int index : attributeSelection) {
            attributes.add(clazz.getAttributes().get(index));
        }

        List<PropertyTextAdapter> properties = new ArrayList<PropertyTextAdapter>();
        for (int elem : methodSelection) {
            properties.add(new PropertyTextAdapter(elem, ""));
        }

        List<GeneratePropertiesRequest> requests = new ArrayList<GeneratePropertiesRequest>();
        GeneratePropertiesRequest req;
        for (INodeAdapter elem : attributes) {
            req = new GeneratePropertiesRequest(clazz, elem, properties, methodOffsetStrategy, propertyOffsetStrategy,
                    accessModifier, new AdapterPrefs("\n", new IGrammarVersionProvider() {

                        public int getGrammarVersion() throws MisconfigurationException {
                            return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
                        }
                    }));
            requests.add(req);
        }

        return requests;
    }

}
