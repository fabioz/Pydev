package com.python.pydev.analysis.javaintegration;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractJavaIntegrationTestWorkbench;

public class JavaIntegrationAnalysisTestWorkbench extends AbstractJavaIntegrationTestWorkbench {
    
    
    
    /**
     * Check many code-completion cases with the java integration.
     */
    public void testJavaClassModule() throws Throwable {
        try{
            //case 1: try find definition for java classes
            checkCase1();
            
            //case 2: try context-insensitive code completion
            checkCase1();
            
//            goToManual();
        }catch(Throwable e){
            //ok, I like errors to appear in stderr (and not only in the unit-test view)
            e.printStackTrace();
            throw e;
        }
    }

    public void checkCase1() throws CoreException {
        String mod1Contents = "from javamod1 import javamod2\njavamod2.JavaClass2";
        setFileContents(mod1Contents);
        //TODO: Do F3 on JavaClass2
    }
    
    public void checkCase2() throws CoreException {
        String mod1Contents = "JavaClas";
        setFileContents(mod1Contents);
        //TODO: See if JavaClass and JavaClass2 are there (check full package name)
    }


}
