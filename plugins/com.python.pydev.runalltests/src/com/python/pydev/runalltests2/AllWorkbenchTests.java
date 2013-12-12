/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.runalltests2;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.python.pydev.customizations.app_engine.wizards.AppEngineConfigWizardPageTestWorkbench;
import org.python.pydev.debug.codecoverage.PyCodeCoverageTestWorkbench;
import org.python.pydev.debug.pyunit.PyUnitView2TestTestWorkbench;
import org.python.pydev.debug.pyunit.PyUnitViewTestTestWorkbench;
import org.python.pydev.debug.referrers.PyReferrersViewTestWorkbench;
import org.python.pydev.debug.ui.DebuggerTestWorkbench;
import org.python.pydev.debug.ui.SourceLocatorTestWorkbench;
import org.python.pydev.debug.ui.launching.PythonRunnerConfigTestWorkbench;
import org.python.pydev.dltk.console.codegen.GetGeneratorTestWorkbench;
import org.python.pydev.dltk.console.codegen.StructuredSelectionGeneratorTestWorkbench;
import org.python.pydev.editor.PyEditTitleTestWorkbench;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JavaClassModuleTestWorkbench;
import org.python.pydev.editor.codecompletion.revisited.jython.JythonCompletionWithBuiltinsTestWorkbench;
import org.python.pydev.editor.codecompletion.revisited.jython.JythonFindDefinitionTestWorkbench;
import org.python.pydev.plugin.nature.ProjectImportedHasAstManagerTestWorkbench;
import org.python.pydev.plugin.nature.SaveFileWithoutNatureTestWorkbench;

import com.python.pydev.analysis.AnalysisRequestsTestWorkbench;
import com.python.pydev.codecompletion.JavaIntegrationPydevComTestWorkbench;
import com.python.pydev.debug.remote.client_api.PydevRemoteDebuggerServerTestWorkbench;
import com.python.pydev.refactoring.tdd.TddTestWorkbench;

public class AllWorkbenchTests {

    public static Test suite() {
        TestSuite suite = new TestSuite(AllWorkbenchTests.class.getName());

        //Must be 1st (no nature or interpreter configured)
        addTestSuite(suite, SaveFileWithoutNatureTestWorkbench.class);
        addTestSuite(suite, ProjectImportedHasAstManagerTestWorkbench.class);
        //End the ones that must be 1st (no nature or interpreter configured)

        addTestSuite(suite, AnalysisRequestsTestWorkbench.class);
        addTestSuite(suite, PyEditTitleTestWorkbench.class);
        addTestSuite(suite, TddTestWorkbench.class);

        addTestSuite(suite, JythonCompletionWithBuiltinsTestWorkbench.class);
        addTestSuite(suite, JythonFindDefinitionTestWorkbench.class);
        addTestSuite(suite, JavaClassModuleTestWorkbench.class);
        addTestSuite(suite, JavaIntegrationPydevComTestWorkbench.class);
        addTestSuite(suite, PythonRunnerConfigTestWorkbench.class);
        addTestSuite(suite, SourceLocatorTestWorkbench.class);
        addTestSuite(suite, AppEngineConfigWizardPageTestWorkbench.class);

        addTestSuite(suite, PydevRemoteDebuggerServerTestWorkbench.class);
        addTestSuite(suite, DebuggerTestWorkbench.class);

        addTestSuite(suite, PyUnitViewTestTestWorkbench.class);
        addTestSuite(suite, PyUnitView2TestTestWorkbench.class);

        addTestSuite(suite, PyReferrersViewTestWorkbench.class);

        addTestSuite(suite, PyCodeCoverageTestWorkbench.class);
        addTestSuite(suite, StructuredSelectionGeneratorTestWorkbench.class);

        addTestSuite(suite, GetGeneratorTestWorkbench.class);

        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }

    private static void addTestSuite(TestSuite suite, Class<? extends TestCase> testClass) {
        //Uncomment to filter which tests should actually be run.
        //        if(!testClass.getName().contains("AppEngineConfigWizardPageTestWorkbench")){
        //            return;
        //        }
        suite.addTestSuite(testClass);
    }

}
