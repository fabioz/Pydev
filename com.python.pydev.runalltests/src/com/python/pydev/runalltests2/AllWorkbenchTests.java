package com.python.pydev.runalltests2;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.python.pydev.debug.ui.DebuggerTestWorkbench;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JavaClassModuleTestWorkbench;
import org.python.pydev.editor.codecompletion.revisited.jython.JythonCompletionWithBuiltinsTestWorkbench;
import org.python.pydev.editor.codecompletion.revisited.jython.JythonFindDefinitionTestWorkbench;
import org.python.pydev.plugin.nature.ProjectImportedHasAstManagerTestWorkbench;
import org.python.pydev.plugin.nature.SaveFileWithoutNatureTestWorkbench;

import com.python.pydev.codecompletion.JavaIntegrationPydevComTestWorkbench;

public class AllWorkbenchTests {

    public static Test suite() {
        TestSuite suite = new TestSuite(AllWorkbenchTests.class.getName());
        
        //Must be 1st (no nature or interpreter configured)
        suite.addTestSuite(SaveFileWithoutNatureTestWorkbench.class); 
        suite.addTestSuite(ProjectImportedHasAstManagerTestWorkbench.class); 
        
        suite.addTestSuite(JythonCompletionWithBuiltinsTestWorkbench.class);
        suite.addTestSuite(JythonFindDefinitionTestWorkbench.class);
        suite.addTestSuite(JavaClassModuleTestWorkbench.class); 
        suite.addTestSuite(JavaIntegrationPydevComTestWorkbench.class); 
        
        suite.addTestSuite(DebuggerTestWorkbench.class); 
        
        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }

}
