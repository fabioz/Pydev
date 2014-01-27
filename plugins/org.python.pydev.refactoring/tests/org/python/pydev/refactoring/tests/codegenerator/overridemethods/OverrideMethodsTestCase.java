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

package org.python.pydev.refactoring.tests.codegenerator.overridemethods;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.codegenerator.overridemethods.edit.MethodEdit;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

import com.thoughtworks.xstream.XStream;

public class OverrideMethodsTestCase extends AbstractIOTestCase {

    public OverrideMethodsTestCase(String name) {
        super(name);
    }

    @Override
    public void runTest() throws Throwable {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        try {
            MockupOverrideMethodsConfig config = initConfig();

            MockupOverrideMethodsRequestProcessor requestProcessor = setupRequestProcessor(config);

            IDocument refactoringDoc = applyOverrideMethod(requestProcessor);

            this.setTestGenerated(refactoringDoc.get());
            assertContentsEqual(getExpected(), getGenerated());
        } finally {
            CompiledModule.COMPILED_MODULES_ENABLED = false;
        }
    }

    private IDocument applyOverrideMethod(MockupOverrideMethodsRequestProcessor requestProcessor)
            throws BadLocationException, MalformedTreeException, MisconfigurationException {
        MethodEdit methodEdit = new MethodEdit(requestProcessor.getRefactoringRequests().get(0));

        IDocument refactoringDoc = new Document(data.source);
        methodEdit.getEdit().apply(refactoringDoc);
        return refactoringDoc;
    }

    private MockupOverrideMethodsRequestProcessor setupRequestProcessor(MockupOverrideMethodsConfig config)
            throws Throwable {
        ModuleAdapter module = super.createModuleAdapterFromDataSource();
        List<IClassDefAdapter> classes = module.getClasses();
        assertTrue(classes.size() > 0);

        MockupOverrideMethodsRequestProcessor requestProcessor = new MockupOverrideMethodsRequestProcessor(module,
                config);
        return requestProcessor;
    }

    private MockupOverrideMethodsConfig initConfig() {
        MockupOverrideMethodsConfig config = null;
        XStream xstream = new XStream();
        xstream.alias("config", MockupOverrideMethodsConfig.class);

        if (data.config.length() > 0) {
            config = (MockupOverrideMethodsConfig) xstream.fromXML(data.getConfigContents());
        } else {
            fail("Could not unserialize configuration");
        }
        return config;
    }
}
