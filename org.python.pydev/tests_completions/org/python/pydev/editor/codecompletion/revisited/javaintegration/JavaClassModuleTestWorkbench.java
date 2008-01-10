package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;

/**
 * Test that needs to run in the workbench to request a code-completion together with the java integration.
 * 
 * @author Fabio
 */
public class JavaClassModuleTestWorkbench extends AbstractJavaIntegrationTestWorkbench {

    
    /**
     * Check many code-completion cases with the java integration.
     */
    public void testJavaClassModule() throws Throwable {
        try{
            //case 1: try it with the rt.jar classes
            checkCase1();
            
            //case 2: try with jar added to the project pythonpath
            checkCase2();
            
            //case 3: try with referenced java project
            checkCase3();
            
            //case 4: try with referenced java project with submodules
            checkCase4();
            
            //case 5: check imports completion
            checkCase5();
            
            //case 6: check imports completion for class
            checkCase6();
            
            //case 7: check import for roots (default package and root folders)
            checkCase7();
            
            //case 8: code-completion for tokens of an import
            checkCase8();
            
//            goToManual();
        }catch(Throwable e){
            //ok, I like errors to appear in stderr (and not only in the unit-test view)
            e.printStackTrace();
            throw e;
        }
    }
    
    
    /**
     * Check with the tokens of a defined import
     */
    public void checkCase8() throws CoreException {
        String mod1Contents = "from javamod1 import javamod2\nprint javamod2.";
        setFileContents(mod1Contents);
        ICompletionProposal[] props = requestProposals(mod1Contents, editor);
        CodeCompletionTestsBase.assertContains("JavaClass2", props);
    }

    
    /**
     * Check with 'import' to find the roots
     */
    public void checkCase7() throws CoreException {
        String mod1Contents = "import ";
        setFileContents(mod1Contents);
        ICompletionProposal[] props = requestProposals(mod1Contents, editor);
        CodeCompletionTestsBase.assertContains("javamod1", props);
        CodeCompletionTestsBase.assertContains("JavaDefault", props);
    }
    
    /**
     * Check with javamod1.javamod2.JavaClass2
     */
    public void checkCase6() throws CoreException {
        String mod1Contents = "import javamod1.javamod2.";
        setFileContents(mod1Contents);
        ICompletionProposal[] props = requestProposals(mod1Contents, editor);
        CodeCompletionTestsBase.assertContains("JavaClass2", props);
    }
    
    /**
     * Check with javamod1.javamod2.JavaClass2
     */
    public void checkCase5() throws CoreException {
        String mod1Contents = "import javamod1.";
        setFileContents(mod1Contents);
        ICompletionProposal[] props = requestProposals(mod1Contents, editor);
        CodeCompletionTestsBase.assertContains("javamod2", props);
    }
    
    
    /**
     * Check with javamod1.javamod2.JavaClass2
     */
    public void checkCase4() throws CoreException {
        String mod1Contents = "import javamod1.javamod2.JavaClass2\njavamod1.javamod2.JavaClass2.";
        setFileContents(mod1Contents);
        ICompletionProposal[] props = requestProposals(mod1Contents, editor);
        CodeCompletionTestsBase.assertContains("JAVA_CLASS_CONSTANT_2", props);
        CodeCompletionTestsBase.assertContains("testJavaClass2(int)", props);
        CodeCompletionTestsBase.assertContains("main(str)", props);
    }

    /**
     * Check with javamod1.JavaClass
     */
    public void checkCase3() throws CoreException {
        String mod1Contents = "import javamod1.JavaClass\njavamod1.JavaClass.";
        setFileContents(mod1Contents);
        
        ICompletionProposal[] props = requestProposals(mod1Contents, editor);
        CodeCompletionTestsBase.assertContains("JAVA_CLASS_CONSTANT", props);
        CodeCompletionTestsBase.assertContains("testJavaClass(int)", props);
        CodeCompletionTestsBase.assertContains("main(str)", props);
    }


    /**
     * Check with junit.framework.Assert
     */
    public void checkCase2() throws CoreException {
        String mod1Contents = "import junit.framework.Assert\njunit.framework.Assert.";
        setFileContents(mod1Contents);
        
        ICompletionProposal[] props = requestProposals(mod1Contents, editor);
        CodeCompletionTestsBase.assertContains("assertNotNull(obj)", props);
        CodeCompletionTestsBase.assertContains("assertEquals(obj, obj)", props);
    }

    /**
     * Check with java.lang.Class
     */
    public void checkCase1() throws CoreException {
        String mod1Contents = "import java.lang.Class\njava.lang.Class.";
        setFileContents(mod1Contents);
        
        ICompletionProposal[] props = requestProposals(mod1Contents, editor);
        CodeCompletionTestsBase.assertContains("getDeclaredFields()", props);
        CodeCompletionTestsBase.assertContains("getPrimitiveClass(string)", props);
    }


}
