/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.runalltests2;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.python.pydev.customizations.app_engine.wizards.AppEngineConfigWizardPageTestWorkbench;
import org.python.pydev.debug.codecoverage.PyCodeCoverageTestWorkbench;
import org.python.pydev.debug.pyunit.PyUnitView2TestTestWorkbench;
import org.python.pydev.debug.pyunit.PyUnitViewTestTestWorkbench;
import org.python.pydev.debug.ui.DebuggerTestWorkbench;
import org.python.pydev.debug.ui.SourceLocatorTestWorkbench;
import org.python.pydev.debug.ui.launching.PythonRunnerConfigTestWorkbench;
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
        suite.addTestSuite(SaveFileWithoutNatureTestWorkbench.class); 
        suite.addTestSuite(ProjectImportedHasAstManagerTestWorkbench.class); 
        //End the ones that must be 1st (no nature or interpreter configured)
        
        
        suite.addTestSuite(AnalysisRequestsTestWorkbench.class); 
        suite.addTestSuite(PyEditTitleTestWorkbench.class); 
        suite.addTestSuite(TddTestWorkbench.class); 

        
        suite.addTestSuite(JythonCompletionWithBuiltinsTestWorkbench.class);
        suite.addTestSuite(JythonFindDefinitionTestWorkbench.class);
        suite.addTestSuite(JavaClassModuleTestWorkbench.class); 
        suite.addTestSuite(JavaIntegrationPydevComTestWorkbench.class); 
        suite.addTestSuite(PythonRunnerConfigTestWorkbench.class); 
        suite.addTestSuite(SourceLocatorTestWorkbench.class); 
        suite.addTestSuite(AppEngineConfigWizardPageTestWorkbench.class);
        
        suite.addTestSuite(PydevRemoteDebuggerServerTestWorkbench.class); 
        suite.addTestSuite(DebuggerTestWorkbench.class); 
        
        suite.addTestSuite(PyUnitViewTestTestWorkbench.class); 
        suite.addTestSuite(PyUnitView2TestTestWorkbench.class); 
        
        suite.addTestSuite(PyCodeCoverageTestWorkbench.class); 
        suite.addTestSuite(StructuredSelectionGeneratorTestWorkbench.class); 
        
        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }

}
