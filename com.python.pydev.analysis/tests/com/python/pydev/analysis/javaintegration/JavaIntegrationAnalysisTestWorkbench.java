package com.python.pydev.analysis.javaintegration;

import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractJavaIntegrationTestWorkbench;

public class JavaIntegrationAnalysisTestWorkbench extends AbstractJavaIntegrationTestWorkbench {
    
    
    
    /**
     * Check many code-completion cases with the java integration.
     */
    public void testJavaClassModule() throws Throwable {
        try{
            //case 1: try it with the analysis of a java class
            checkCase1();
            
//            goToManual();
        }catch(Throwable e){
            //ok, I like errors to appear in stderr (and not only in the unit-test view)
            e.printStackTrace();
            throw e;
        }
    }

    public void checkCase1() {
//        goToManual();
    }


}
