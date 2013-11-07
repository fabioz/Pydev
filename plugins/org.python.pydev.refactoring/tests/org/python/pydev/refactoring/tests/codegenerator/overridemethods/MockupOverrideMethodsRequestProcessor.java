/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
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
*     Fabio Zadrozny <fabiofz@gmail.com>       - initial implementation
*     Alexander Kurtakov <akurtako@redhat.com> - ongoing maintenance
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.codegenerator.overridemethods;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.codegenerator.overridemethods.request.OverrideMethodsRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.shared_core.string.StringUtils;

public class MockupOverrideMethodsRequestProcessor implements IRequestProcessor<OverrideMethodsRequest> {

    private ModuleAdapter module;

    private String classSelection;

    private int offsetStrategy;

    private List<Integer> methodSelection;

    private int editClass;

    private MockupOverrideMethodsRequestProcessor(ModuleAdapter module, String classSelection,
            List<Integer> methodSelection, int offsetStrategy, int editClass) {
        this.module = module;
        this.methodSelection = methodSelection;
        this.classSelection = classSelection;
        this.offsetStrategy = offsetStrategy;
        this.editClass = editClass;
    }

    public MockupOverrideMethodsRequestProcessor(ModuleAdapter module, MockupOverrideMethodsConfig config) {
        this(module, config.getClassSelection(), config.getMethodSelection(), config.getOffsetStrategy(), config
                .getEditClass());
    }

    public List<OverrideMethodsRequest> getRefactoringRequests() throws MisconfigurationException {
        ClassDefAdapter clazz = (ClassDefAdapter) module.getClasses().get(editClass);

        IClassDefAdapter clazzSelection;

        int parsedInt = -1;
        try {
            parsedInt = Integer.parseInt(classSelection);
        } catch (NumberFormatException e) {
        }

        if (parsedInt != -1) {
            clazzSelection = module.getClasses().get(parsedInt);
        } else {
            List<String> split = StringUtils.split(classSelection, ",");
            if (split.size() != 2) {
                throw new RuntimeException("Right now can only handle with a single comma.");
            }
            clazzSelection = module.getClasses().get(Integer.parseInt(split.get(0)));
            List<IClassDefAdapter> classHierarchy = module.getClassHierarchy(clazz);
            boolean found = false;
            StringBuffer foundClasses = new StringBuffer("\nFound classes:");
            for (IClassDefAdapter iClassDefAdapter : classHierarchy) {
                foundClasses.append(iClassDefAdapter.getName());
                if (iClassDefAdapter.getName().equals(split.get(1))) {
                    clazzSelection = iClassDefAdapter;
                    found = true;
                    break;
                }
            }
            if (!found) {
                String message = "Could not find: " + split.get(1) + foundClasses;

                throw new RuntimeException(message);
            }
        }

        String baseClassName = clazzSelection.getName();
        List<FunctionDefAdapter> methods = new ArrayList<FunctionDefAdapter>();

        for (Object o : methodSelection) {
            if (o instanceof Integer) {
                methods.add(clazzSelection.getFunctions().get((Integer) o));
            } else if (o instanceof String) {
                List<FunctionDefAdapter> functions = clazzSelection.getFunctions();
                boolean found = false;
                for (FunctionDefAdapter f : functions) {
                    if (f.getName().equals(o)) {
                        methods.add(f);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new RuntimeException("Unable to find:" + o);
                }
            } else {
                throw new RuntimeException("Unable to recognize: " + o);
            }
        }

        List<OverrideMethodsRequest> requests = new ArrayList<OverrideMethodsRequest>();

        for (FunctionDefAdapter method : methods) {
            OverrideMethodsRequest req = new OverrideMethodsRequest(clazz, this.offsetStrategy, method, false,
                    baseClassName, new AdapterPrefs("\n", new IGrammarVersionProvider() {

                        public int getGrammarVersion() throws MisconfigurationException {
                            return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
                        }
                    }));
            requests.add(req);
        }

        return requests;

    }
}
