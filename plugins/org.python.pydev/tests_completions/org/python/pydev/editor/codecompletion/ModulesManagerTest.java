/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 21, 2006
 */
package org.python.pydev.editor.codecompletion;

import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;

public class ModulesManagerTest extends CodeCompletionTestsBase{

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ModulesManagerTest.class);
    }


    public void setUp() throws Exception {
        super.setUp();
        this.restorePythonPath(false);
    }

    public void testIt() throws Exception {
        //change: returns itself too
        ProjectModulesManager modulesManager = (ProjectModulesManager) nature2.getAstManager().getModulesManager();
        assertEquals(1+1, modulesManager.getManagersInvolved(false).length);
        assertEquals(2+1, modulesManager.getManagersInvolved(true).length);
        assertEquals(0+1, modulesManager.getRefencingManagersInvolved(false).length);
        assertEquals(1+1, modulesManager.getRefencingManagersInvolved(true).length);

        ProjectModulesManager modulesManager2 = (ProjectModulesManager) nature.getAstManager().getModulesManager();
        assertEquals(0+1, modulesManager2.getManagersInvolved(false).length);
        assertEquals(1+1, modulesManager2.getManagersInvolved(true).length);
        assertEquals(1+1, modulesManager2.getRefencingManagersInvolved(false).length);
        assertEquals(2+1, modulesManager2.getRefencingManagersInvolved(true).length);
        
    }
}
