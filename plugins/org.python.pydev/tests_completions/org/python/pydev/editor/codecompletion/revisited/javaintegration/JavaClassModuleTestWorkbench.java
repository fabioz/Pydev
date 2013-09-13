/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;

/**
 * Test that needs to run in the workbench to request a code-completion together with the java integration.
 * 
 * @author Fabio
 */
public class JavaClassModuleTestWorkbench extends AbstractWorkbenchTestCase {

    /**
     * Check many code-completion cases with the java integration.
     */
    public void testJavaClassModule() throws Throwable {
        try {

            //We have to wait a bit until the info is setup for the tests to work...
            waitForModulesManagerSetup();

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

            //case 9: code-completion for properties
            checkCase9();

            //case 10: code-completion for static access (grinder.jar)
            checkCase10();

            //            goToManual();
        } catch (Throwable e) {
            //ok, I like errors to appear in stderr (and not only in the unit-test view)
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Check with grinder static access.
     */
    public void checkCase10() throws CoreException {
        String mod1Contents = "" + "from net.grinder.script.Grinder import grinder\n" + "e = grinder.getLogger().";
        setFileContents(mod1Contents);

        ICompletionProposal[] props = requestProposals(mod1Contents, editor);
        CodeCompletionTestsBase.assertContains("error(string)", props);
        CodeCompletionTestsBase.assertContains("getErrorLogWriter()", props);
    }

    /**
     * Check with the tokens of a defined import
     */
    public void checkCase9() throws CoreException {
        String mod1Contents = "from java.lang.Boolean import TYPE\nTYPE.";
        setFileContents(mod1Contents);
        ICompletionProposal[] props = requestProposals(mod1Contents, editor);
        CodeCompletionTestsBase.assertContains("fields", props); //getFields should generate a 'fields' entry.
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
