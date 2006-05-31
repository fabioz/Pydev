/*
 * Created on May 31, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings.extract;

import org.eclipse.jface.text.Document;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;

import com.python.pydev.refactoring.refactorer.Refactorer;
import com.python.pydev.refactoring.refactorer.refactorings.rename.RefactoringTestBase;

public class ExtractMethodTest extends RefactoringTestBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ExtractMethodTest.class);
    }

    protected void setUp() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
        AbstractPyRefactoring.setPyRefactoring(new Refactorer());
    }

    protected void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }

    public void test1() throws Exception {
        Document document = new Document("" +
                "a = 1" +
                "");
        
    }
}
