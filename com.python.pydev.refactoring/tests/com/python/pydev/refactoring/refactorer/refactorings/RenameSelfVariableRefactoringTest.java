/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.refactoring.RefactoringRequest;

public class RenameSelfVariableRefactoringTest extends RefactoringTestBase{
    public static void main(String[] args) {
        try {
            RenameSelfVariableRefactoringTest test = new RenameSelfVariableRefactoringTest();
            test.setUp();
            test.testRenameErr();
            test.tearDown();

            junit.textui.TestRunner.run(RenameSelfVariableRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    
    public void testRenameErr() throws Exception {
        String str ="" +
        "class Foo:\n" +
        "    def m1(self):\n" +
        "        self.%s = 1\n" +
        "        print self.%s\n" +
        "    def m2(self):\n" +
        "        print self.%s\n" +
        "";
        Document doc = new Document(StringUtils.format(str, new Object[]{"aa","aa","aa"}));
        int line = 3;
        int col = 20;
        PySelection ps = new PySelection(doc, line, col);
        
        RefactoringRequest request = new RefactoringRequest(null, ps, nature);
        request.moduleName = "foo";
        request.duringProcessInfo.initialName = "aa"; 
        request.duringProcessInfo.initialOffset = ps.getAbsoluteCursorOffset();
        request.duringProcessInfo.name = "bb";
        
        applyRefactoring(request);
        String refactored = doc.get();
        System.out.println(refactored);
        assertEquals(StringUtils.format(str, new Object[]{"bb", "bb", "bb"}),  refactored);

    }


}
