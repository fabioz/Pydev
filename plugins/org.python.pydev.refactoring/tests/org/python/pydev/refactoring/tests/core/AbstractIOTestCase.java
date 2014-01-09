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
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>       - initial implementation
*     Alexander Kurtakov <akurtako@redhat.com> - ongoing maintenance
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.core;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.refactoring.ast.PythonModuleManager;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public abstract class AbstractIOTestCase extends TestCase implements IInputOutputTestCase {
    private String generated;
    protected TestData data;
    protected CodeCompletionTestsBase codeCompletionTestsBase = new CodeCompletionTestsBase();

    protected ModuleAdapter createModuleAdapterFromDataSource() throws Throwable {
        return createModuleAdapterFromDataSource(null);
    }

    /**
     * @param version IPythonNature.PYTHON_VERSION_XXX
     */
    protected ModuleAdapter createModuleAdapterFromDataSource(String version) throws Throwable {
        codeCompletionTestsBase.restorePythonPath(FileUtils.getFileAbsolutePath(data.file.getParentFile()), true);
        PythonModuleManager pythonModuleManager = new PythonModuleManager(CodeCompletionTestsBase.nature);
        if (version != null) {
            //As the files will be found in the system, we need to set the system modules manager info.
            IModulesManager modulesManager = pythonModuleManager.getIModuleManager();
            SystemModulesManager systemModulesManager = (SystemModulesManager) modulesManager.getSystemModulesManager();
            systemModulesManager.setInfo(new InterpreterInfo(version, "", new ArrayList<String>()));

            CodeCompletionTestsBase.nature.setVersion(version, null);
        }
        ModuleAdapter module = VisitorFactory.createModuleAdapter(pythonModuleManager, data.file, new Document(
                data.source), CodeCompletionTestsBase.nature, CodeCompletionTestsBase.nature);
        return module;
    }

    protected IGrammarVersionProvider createVersionProvider() {
        IGrammarVersionProvider versionProvider = new IGrammarVersionProvider() {

            public int getGrammarVersion() throws MisconfigurationException {
                if (data.file.toString().contains("_grammar3")) {
                    return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0;
                }
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        };
        return versionProvider;
    }

    public AbstractIOTestCase(String name) {
        this(name, false);
    }

    public AbstractIOTestCase(String name, boolean ignoreEmptyLines) {
        super(name);
    }

    protected void assertContentsEqual(String expected, String generated) {
        assertEquals(StringUtils.replaceNewLines(expected, "\n"), StringUtils.replaceNewLines(generated, "\n"));
    }

    @Override
    protected void setUp() throws Exception {
        PythonModuleManager.setTesting(true);
        codeCompletionTestsBase.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        PythonModuleManager.setTesting(false);
        codeCompletionTestsBase.tearDown();
    }

    protected String getGenerated() {
        return StringUtils.replaceNewLines(generated.trim(), "\n");
    }

    public void setTestGenerated(String source) {
        this.generated = source;
    }

    public void setData(TestData data) {
        this.data = data;
    }

    public String getExpected() {
        return StringUtils.replaceNewLines(data.result, "\n");
    }
}
