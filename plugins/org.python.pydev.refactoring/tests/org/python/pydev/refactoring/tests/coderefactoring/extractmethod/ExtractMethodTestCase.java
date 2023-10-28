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
 */

package org.python.pydev.refactoring.tests.coderefactoring.extractmethod;

import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ExtractCallEdit;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ExtractMethodEdit;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ParameterReturnDeduce;
import org.python.pydev.refactoring.coderefactoring.extractmethod.request.ExtractMethodRequest;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.base.RefactoringInfo.SelectionComputer;
import org.python.pydev.refactoring.core.base.RefactoringInfo.SelectionComputer.SelectionComputerKind;
import org.python.pydev.refactoring.tests.adapter.PythonNatureStub;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.CoreTextSelection;
import org.python.pydev.shared_core.string.ICoreTextSelection;

import com.thoughtworks.xstream.XStream;

public class ExtractMethodTestCase extends AbstractIOTestCase {

    private static final int EXTENSION = 4;

    public ExtractMethodTestCase(String name) {
        super(name);
    }

    @Override
    public void runTest() throws Throwable {
        FileUtils.IN_TESTS = true;
        MockupExtractMethodConfig config = initConfig();

        IDocument doc = new Document(data.source);
        IGrammarVersionProvider versionProvider = createVersionProvider();
        Module astModule;
        try {
            astModule = VisitorFactory.getRootNode(doc, versionProvider);
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing:\n" + data.source + "\n---\nError: " + e.getMessage(), e);
        } catch (Exception e1) {
            throw new RuntimeException("Error parsing:\n" + data.source, e1);
        }
        String name = data.file.getName();
        name = name.substring(0, name.length() - EXTENSION);
        ModuleAdapter module = new ModuleAdapter(null, data.file, doc, astModule, new PythonNatureStub());

        if (data.sourceSelection == null) {
            System.out.println("here");
        }
        ICoreTextSelection selection = new CoreTextSelection(doc, data.sourceSelection.getOffset(),
                data.sourceSelection.getLength());

        RefactoringInfo info = new RefactoringInfo(doc, selection, versionProvider);

        MockupExtractMethodRequestProcessor requestProcessor = setupRequestProcessor(config, module, info);

        IDocument refactoringDoc = applyExtractMethod(info, requestProcessor);

        this.setTestGenerated(refactoringDoc.get());
        assertContentsEqual(getExpected(), getGenerated());
        FileUtils.IN_TESTS = false;
    }

    private IDocument applyExtractMethod(RefactoringInfo info, MockupExtractMethodRequestProcessor requestProcessor)
            throws BadLocationException, MalformedTreeException, MisconfigurationException {
        ExtractMethodRequest req = requestProcessor.getRefactoringRequests().get(0);

        ExtractMethodEdit extractedMethodEdit = new ExtractMethodEdit(req);
        ExtractCallEdit callExtractedMethodEdit = new ExtractCallEdit(req);

        MultiTextEdit edit = new MultiTextEdit();
        edit.addChild(extractedMethodEdit.getEdit());
        edit.addChild(callExtractedMethodEdit.getEdit());

        IDocument refactoringDoc = new Document(data.source);
        edit.apply(refactoringDoc);
        return refactoringDoc;
    }

    private MockupExtractMethodRequestProcessor setupRequestProcessor(MockupExtractMethodConfig config,
            ModuleAdapter module, RefactoringInfo info) {
        SelectionComputer selectionComputer = info.getSelectionComputer(SelectionComputerKind.extractMethod);
        ModuleAdapter parsedSelection = selectionComputer.selectionModuleAdapter;

        AbstractScopeNode<?> scope = module.getScopeAdapter(selectionComputer.selection);
        ParameterReturnDeduce deducer = new ParameterReturnDeduce(scope, selectionComputer.selection, module);

        SortedMap<String, String> renameMap = new TreeMap<String, String>();
        for (String variable : deducer.getParameters()) {
            String newName = variable;
            if (config.getRenameMap().containsKey(variable)) {
                newName = config.getRenameMap().get(variable);
            }
            renameMap.put(variable, newName);
        }

        return new MockupExtractMethodRequestProcessor(scope, selectionComputer.selection, info.getVersionProvider(),
                parsedSelection, deducer,
                renameMap, config.getOffsetStrategy());
    }

    private MockupExtractMethodConfig initConfig() {
        MockupExtractMethodConfig config = null;
        XStream xstream = new XStream();
        XStream.setupDefaultSecurity(xstream);
        xstream.allowTypesByWildcard(new String[] {
                "org.python.pydev.**"
        });
        xstream.alias("config", MockupExtractMethodConfig.class);

        if (data.config.length() > 0) {
            config = (MockupExtractMethodConfig) xstream.fromXML(data.getConfigContents());
        } else {
            config = new MockupExtractMethodConfig();
        }
        return config;
    }
}
