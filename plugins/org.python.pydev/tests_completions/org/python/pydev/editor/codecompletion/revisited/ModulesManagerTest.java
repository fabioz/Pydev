/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 21, 2006
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.editor.codecompletion.revisited.ModulesManager;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.shared_core.io.FileUtils;

public class ModulesManagerTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        try {
            ModulesManagerTest test = new ModulesManagerTest();
            test.setUp();
            test.testRestoreContents2();
            test.tearDown();

            junit.textui.TestRunner.run(ModulesManagerTest.class);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public void setUp() throws Exception {
        super.setUp();
        this.restorePythonPath(false);
    }

    public void __testIt() throws Exception {
        //change: returns itself too
        ProjectModulesManager modulesManager = (ProjectModulesManager) nature2.getAstManager().getModulesManager();
        assertEquals(1 + 1, modulesManager.getManagersInvolved(false).length);
        assertEquals(2 + 1, modulesManager.getManagersInvolved(true).length);
        assertEquals(0 + 1, modulesManager.getRefencingManagersInvolved(false).length);
        assertEquals(1 + 1, modulesManager.getRefencingManagersInvolved(true).length);

        ProjectModulesManager modulesManager2 = (ProjectModulesManager) nature.getAstManager().getModulesManager();
        assertEquals(0 + 1, modulesManager2.getManagersInvolved(false).length);
        assertEquals(1 + 1, modulesManager2.getManagersInvolved(true).length);
        assertEquals(1 + 1, modulesManager2.getRefencingManagersInvolved(false).length);
        assertEquals(2 + 1, modulesManager2.getRefencingManagersInvolved(true).length);
    }

    public void testLoad() throws Exception {
        SystemModulesManager manager = new SystemModulesManager(null);
        manager.addModule(new ModulesKey("bar", new File("bar.py")));
        manager.addModule(new ModulesKey("foo", new File("foo.py")));
        manager.addModule(new ModulesKey("empty", null));
        manager.addModule(new ModulesKeyForZip("zip", new File("zip.zip"), "path", true));

        PythonPathHelper pythonPathHelper = manager.getPythonPathHelper();
        pythonPathHelper.setPythonPath("rara|boo");
        assertEquals(Arrays.asList("rara", "boo"), manager.getPythonPathHelper().getPythonpath());

        File f = new File("modules_manager_testing.temporary_dir");
        try {
            FileUtils.deleteDirectoryTree(f);
        } catch (Exception e1) {
            //ignore
        }
        try {
            manager.saveToFile(f);

            SystemModulesManager loaded = new SystemModulesManager(null);
            SystemModulesManager.loadFromFile(loaded, f);
            ModulesKey[] onlyDirectModules = loaded.getOnlyDirectModules();
            boolean found = false;
            for (ModulesKey modulesKey : onlyDirectModules) {
                if (modulesKey.name.equals("zip")) {
                    ModulesKeyForZip z = (ModulesKeyForZip) modulesKey;
                    assertEquals(z.zipModulePath, "path");
                    assertEquals(z.file, new File("zip.zip"));
                    assertEquals(z.isFile, true);
                    found = true;
                }
            }
            if (!found) {
                fail("Did not find ModulesKeyForZip.");
            }
            Set<String> set = new HashSet<String>();
            set.add("bar");
            set.add("foo");
            set.add("empty");
            set.add("zip");
            assertEquals(set, loaded.getAllModuleNames(true, ""));
            assertEquals(Arrays.asList("rara", "boo"), loaded.getPythonPathHelper().getPythonpath());
        } finally {
            FileUtils.deleteDirectoryTree(f);
        }

    }

    public void testRestoreContents() throws Exception {
        String contents = "" +
                "A|A.py\n" +
                "B\r\n" +
                "D|0|E|1" +
                "";

        ProjectModulesManager manager = new ProjectModulesManager();
        HashMap<Integer, String> intToString = new HashMap<Integer, String>();
        intToString.put(0, "W.py");
        ModulesManager.handleFileContents(manager, contents, intToString);

        assertEquals(3, manager.modulesKeys.size());

        ModulesKey key = manager.modulesKeys.get(new ModulesKey("A", null));
        assertEquals(key, new ModulesKey("A", null));
        assertEquals(key.file, new File("A.py"));
        assertTrue(!(key instanceof ModulesKeyForZip));

        key = manager.modulesKeys.get(new ModulesKey("B", null));
        assertEquals(key, new ModulesKey("B", null));
        assertNull(key.file);
        assertTrue(!(key instanceof ModulesKeyForZip));

        key = manager.modulesKeys.get(new ModulesKey("D", null));
        assertEquals(key, new ModulesKey("D", null));
        assertEquals(key.file, new File("W.py"));
        assertTrue(key instanceof ModulesKeyForZip);
        ModulesKeyForZip kz = (ModulesKeyForZip) key;
        assertTrue(kz.isFile);
        assertEquals(kz.zipModulePath, "E");

    }

    public void testRestoreContents2() throws Exception {
        String contents = "" +
                "A||A.py||\n" +
                "B|\r\n" +
                "D|0|E|1\n" +
                "";

        ProjectModulesManager manager = new ProjectModulesManager();
        HashMap<Integer, String> intToString = new HashMap<Integer, String>();
        intToString.put(0, "W.py");
        ModulesManager.handleFileContents(manager, contents, intToString);

        assertEquals(3, manager.modulesKeys.size());

        ModulesKey key = manager.modulesKeys.get(new ModulesKey("A", null));
        assertEquals(key, new ModulesKey("A", null));
        assertEquals(key.file, new File("A.py"));
        assertTrue(!(key instanceof ModulesKeyForZip));

        key = manager.modulesKeys.get(new ModulesKey("B", null));
        assertEquals(key, new ModulesKey("B", null));
        assertNull(key.file);
        assertTrue(!(key instanceof ModulesKeyForZip));

        key = manager.modulesKeys.get(new ModulesKey("D", null));
        assertEquals(key, new ModulesKey("D", null));
        assertEquals(key.file, new File("W.py"));
        assertTrue(key instanceof ModulesKeyForZip);
        ModulesKeyForZip kz = (ModulesKeyForZip) key;
        assertTrue(kz.isFile);
        assertEquals(kz.zipModulePath, "E");

    }
}
