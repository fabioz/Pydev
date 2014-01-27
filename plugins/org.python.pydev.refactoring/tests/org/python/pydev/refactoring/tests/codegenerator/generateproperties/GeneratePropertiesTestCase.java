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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.codegenerator.generateproperties.edit.DeleteMethodEdit;
import org.python.pydev.refactoring.codegenerator.generateproperties.edit.GetterMethodEdit;
import org.python.pydev.refactoring.codegenerator.generateproperties.edit.PropertyEdit;
import org.python.pydev.refactoring.codegenerator.generateproperties.edit.SetterMethodEdit;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.SelectionState;
import org.python.pydev.refactoring.tests.adapter.PythonNatureStub;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

import com.thoughtworks.xstream.XStream;

public class GeneratePropertiesTestCase extends AbstractIOTestCase {

    private ArrayList<TextEdit> multiEdit;

    public GeneratePropertiesTestCase(String name) {
        super(name);
    }

    protected void addEdit(TextEdit edit) {
        multiEdit.add(edit);
    }

    @Override
    public void runTest() throws Throwable {
        MockupGeneratePropertiesConfig config = initConfig();

        MockupGeneratePropertiesRequestProcessor requestProcessor = setupRequestProcessor(config);

        IDocument refactoringDoc = applyGenerateProperties(requestProcessor);

        this.setTestGenerated(refactoringDoc.get());
        assertEquals(getExpected(), getGenerated());
    }

    private IDocument applyGenerateProperties(MockupGeneratePropertiesRequestProcessor requestProcessor)
            throws BadLocationException, MalformedTreeException, MisconfigurationException {
        IDocument refactoringDoc = new Document(data.source);
        MultiTextEdit multi = new MultiTextEdit();
        for (GeneratePropertiesRequest req : requestProcessor.getRefactoringRequests()) {
            SelectionState state = req.getSelectionState();

            if (state.isGetter()) {
                multi.addChild(new GetterMethodEdit(req).getEdit());
            }
            if (state.isSetter()) {
                multi.addChild(new SetterMethodEdit(req).getEdit());
            }
            if (state.isDelete()) {
                multi.addChild(new DeleteMethodEdit(req).getEdit());
            }
            multi.addChild(new PropertyEdit(req).getEdit());
        }
        multi.apply(refactoringDoc);
        return refactoringDoc;
    }

    private MockupGeneratePropertiesRequestProcessor setupRequestProcessor(MockupGeneratePropertiesConfig config)
            throws Throwable {
        ModuleAdapter module = VisitorFactory.createModuleAdapter(null, null, new Document(data.source),
                new PythonNatureStub(), createVersionProvider());
        List<IClassDefAdapter> classes = module.getClasses();
        assertTrue(classes.size() > 0);

        MockupGeneratePropertiesRequestProcessor requestProcessor = new MockupGeneratePropertiesRequestProcessor(
                module, config);
        return requestProcessor;
    }

    private MockupGeneratePropertiesConfig initConfig() {
        MockupGeneratePropertiesConfig config = null;
        XStream xstream = new XStream();
        xstream.alias("config", MockupGeneratePropertiesConfig.class);

        if (data.config.length() > 0) {
            config = (MockupGeneratePropertiesConfig) xstream.fromXML(data.getConfigContents());
        } else {
            fail("Could not unserialize configuration");
        }
        return config;
    }
}
