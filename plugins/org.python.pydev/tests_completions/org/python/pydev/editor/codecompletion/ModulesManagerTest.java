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

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;

public class ModulesManagerTest extends CodeCompletionTestsBase{

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ModulesManagerTest.class);
    }


    public void setUp() throws Exception {
        super.setUp();
        this.restorePythonPath(false);
    }

    public void __testIt() throws Exception {
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
    
    public void testLoad() throws Exception {
        SystemModulesManager manager = new SystemModulesManager(null);
        manager.addModule(new ModulesKey("bar", new File("bar.py")));
        manager.addModule(new ModulesKey("foo", new File("foo.py")));
        manager.addModule(new ModulesKey("empty", null));
        PythonPathHelper pythonPathHelper = manager.getPythonPathHelper();
        pythonPathHelper.setPythonPath("rara|boo");
        assertEquals(Arrays.asList("rara", "boo"), manager.getPythonPath());
        
        File f = new File("modules_manager_testing.temporary_dir");
        try {
            REF.deleteDirectoryTree(f);
        } catch (Exception e1) {
            //ignore
        }
        try {
            manager.saveToFile(f);
            
            SystemModulesManager loaded = new SystemModulesManager(null);
            SystemModulesManager.loadFromFile(loaded, f);
            Set<String> set = new HashSet<String>();
            set.add("bar");
            set.add("foo");
            set.add("empty");
            assertEquals(set, loaded.getAllModuleNames(true, ""));
            assertEquals(Arrays.asList("rara", "boo"), loaded.getPythonPath());
        } finally {
            REF.deleteDirectoryTree(f);
        }
        
    }
}
