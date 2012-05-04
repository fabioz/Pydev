/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;
import com.python.pydev.refactoring.wizards.IRefactorRenameProcess;

public class RenameFunctionRefactoringTest2 extends RefactoringRenameTestBase {

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = true;
            RenameFunctionRefactoringTest2 test = new RenameFunctionRefactoringTest2();
            test.setUp();
            test.tearDown();

            junit.textui.TestRunner.run(RenameFunctionRefactoringTest2.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    public void testRename5() throws Exception {
        List<IInfo> toks = AdditionalProjectInterpreterInfo.getTokensEqualTo("RenameFunc2", natureRefactoring,
                AbstractAdditionalTokensInfo.TOP_LEVEL | AbstractAdditionalTokensInfo.INNER);
        assertEquals(4, toks.size());
                
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamefunction2.renamefunc2", 3, 19);
        assertEquals(3, references.size()); 
        assertEquals(2, references.get("reflib.renamefunction2.dontrenamefunc2").size());
        assertEquals(6, references.get("reflib.renamefunction2.renamefunc3").size());
        assertEquals(5, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        checkProcessors();
    }


    @Override
    protected void checkProcessors() {
        if(lastProcessorUsed != null){
            List<IRefactorRenameProcess> processes = lastProcessorUsed.process;
            assertEquals(4, processes.size());
            
//            for (IRefactorRenameProcess process : processes) {
//                System.out.println(process);
//            }
        }
    }
    
    @Override
    protected Class getProcessUnderTest() {
        throw new RuntimeException("Not used in this test!");
    }

}
