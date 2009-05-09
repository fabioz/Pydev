package org.python.pydev.runalltests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.python.pydev.customizations.app_engine.wizards.AppEngineConfigWizardPageWorkbenchTest;
import org.python.pydev.debug.ui.DebuggerTestWorkbench;
import org.python.pydev.debug.ui.SourceLocatorTestWorkbench;
import org.python.pydev.debug.ui.launching.PythonRunnerConfigTestWorkbench;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JavaClassModuleTestWorkbench;
import org.python.pydev.editor.codecompletion.revisited.jython.JythonCompletionWithBuiltinsTestWorkbench;
import org.python.pydev.editor.codecompletion.revisited.jython.JythonFindDefinitionTestWorkbench;
import org.python.pydev.plugin.nature.ProjectImportedHasAstManagerTestWorkbench;
import org.python.pydev.plugin.nature.SaveFileWithoutNatureTestWorkbench;

public class AllWorkbenchTests {

    public static Test suite() {
        TestSuite suite = new TestSuite(AllWorkbenchTests.class.getName());
        
        //This one doesn't really configure anything, so, should have no real side effects and is safe to run here
        
        
        //Must be 1st (no nature or interpreter configured)
        suite.addTestSuite(SaveFileWithoutNatureTestWorkbench.class); 
        suite.addTestSuite(ProjectImportedHasAstManagerTestWorkbench.class); 
        //End the ones that must be 1st (no nature or interpreter configured)

        
        suite.addTestSuite(JythonCompletionWithBuiltinsTestWorkbench.class);
        suite.addTestSuite(JythonFindDefinitionTestWorkbench.class);
        suite.addTestSuite(JavaClassModuleTestWorkbench.class); 
        suite.addTestSuite(PythonRunnerConfigTestWorkbench.class); 
        suite.addTestSuite(SourceLocatorTestWorkbench.class); 
        suite.addTestSuite(AppEngineConfigWizardPageWorkbenchTest.class); 
        
        suite.addTestSuite(DebuggerTestWorkbench.class); 
        
        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }

}
