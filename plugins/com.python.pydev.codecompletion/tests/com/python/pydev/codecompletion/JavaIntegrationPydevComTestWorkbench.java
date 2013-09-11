/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.codecompletion;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.editor.model.ItemPointer;

import com.python.pydev.refactoring.actions.PyGoToDefinition;

public class JavaIntegrationPydevComTestWorkbench extends AbstractWorkbenchTestCase {

    /**
     * Check many code-completion cases with the java integration.
     */
    public void testJavaClassModule() throws Throwable {
        try {
            //case 1: try find definition for java classes
            checkCase1();

            //case 2: try context-insensitive code completion
            checkCase2();

            //            goToManual();
        } catch (Throwable e) {
            //ok, I like errors to appear in stderr (and not only in the unit-test view)
            e.printStackTrace();
            throw e;
        }
    }

    public void checkCase1() throws CoreException {
        String mod1Contents = "from javamod1 import javamod2\njavamod2.JavaClass2";
        setFileContents(mod1Contents);

        PyGoToDefinition pyGoToDefinition = new PyGoToDefinition();
        pyGoToDefinition.setEditor(editor);
        editor.setSelection(mod1Contents.length() - 2, 0);
        editor.doSave(null); //update the caches
        ItemPointer[] itemPointers = pyGoToDefinition.findDefinitionsAndOpen(false);
        for (ItemPointer pointer : itemPointers) {
            System.out.println(pointer);
        }
        assertTrue(itemPointers.length >= 1);
    }

    public void checkCase2() throws CoreException {
        String mod1Contents = "JavaClas";
        setFileContents(mod1Contents);
        ICompletionProposal[] proposals = this.requestProposals(mod1Contents, editor);

        CodeCompletionTestsBase.assertContains("JavaClass - javamod1", proposals);
        CodeCompletionTestsBase.assertContains("JavaClass2 - javamod1.javamod2", proposals);
    }

}
