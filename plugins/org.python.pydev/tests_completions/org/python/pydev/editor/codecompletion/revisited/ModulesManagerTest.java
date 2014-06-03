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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;

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

    @Override
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

    public void testDiffModules() {
        ModulesKey a = new ModulesKey("a", null);
        ModulesKey b = new ModulesKey("b", null);
        ModulesKey c = new ModulesKey("c", null);

        ProjectModulesManager manager = new ProjectModulesManager();
        manager.addModule(a);
        manager.addModule(b);

        PyPublicTreeMap<ModulesKey, ModulesKey> m = new PyPublicTreeMap<>();
        m.put(b, b);
        m.put(c, c);
        Tuple<List<ModulesKey>, List<ModulesKey>> delta = manager.diffModules(m);

        Set<ModulesKey> added = new TreeSet<>();
        added.add(c);
        assertEqualContents(added, delta.o1);

        Set<ModulesKey> removed = new TreeSet<>();
        removed.add(a);
        assertEqualContents(removed, delta.o2);
    }

    private void assertEqualContents(Collection<ModulesKey> s1, Collection<ModulesKey> s2) {
        assertTrue(s2.containsAll(s1));
        assertTrue(s1.containsAll(s2));
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

    public void testGetAllModuleNamesEmpty() {
        ProjectModulesManager manager = new ProjectModulesManager();
        assertEquals(new HashSet<>(), manager.getAllModuleNames(false, ""));
        assertEquals(new HashSet<>(), manager.getAllModuleNames(false, "foobar"));
    }

    public void testGetAllModuleNames() {
        ModulesKey k1 = new ModulesKey("org.arabidopsis", null);
        ModulesKey k2 = new ModulesKey("org.hashcollision.devel", null);
        ModulesKey k3 = new ModulesKey("edu.wpi.study", null);
        ModulesKey k4 = new ModulesKey("edu.brown.visitor", null);
        ModulesKey k5 = new ModulesKey("com.google.worker", null);

        ProjectModulesManager manager = new ProjectModulesManager();
        manager.addModule(k1);
        manager.addModule(k2);
        manager.addModule(k3);
        manager.addModule(k4);
        manager.addModule(k5);

        assertContainsAll(manager.getAllModuleNames(false, ""),
                "org.arabidopsis", "org.hashcollision.devel", "edu.wpi.study",
                "edu.brown.visitor", "com.google.worker");

        assertContainsAll(manager.getAllModuleNames(false, "org"),
                "org.arabidopsis", "org.hashcollision.devel");

        assertContainsAll(manager.getAllModuleNames(false, "dev"),
                "org.hashcollision.devel");
        assertContainsAll(manager.getAllModuleNames(false, "nothere"));
        assertContainsAll(manager.getAllModuleNames(false, "pi"));
        assertContainsAll(manager.getAllModuleNames(false, "wpi"), "edu.wpi.study");

        assertContainsAll(manager.getAllModuleNames(false, "w"),
                "edu.wpi.study", "com.google.worker");

        // Checks that removal works:
        List<ModulesKey> modulesToRemove = new ArrayList<>();
        modulesToRemove.add(k3);
        manager.removeModules(modulesToRemove);
        assertContainsAll(manager.getAllModuleNames(false, "edu"), "edu.brown.visitor");
        assertContainsAll(manager.getAllModuleNames(false, "wpi"));
    }

    public void testGetAllModulesStartingWith() {
        ModulesKey k1 = new ModulesKey("fabioz", null);
        ProjectModulesManager manager = new ProjectModulesManager();
        manager.addModule(k1);
        SortedMap<ModulesKey, ModulesKey> actual = manager.getAllDirectModulesStartingWith("fabio");
        assertEquals(1, actual.size());
        assertEquals(k1, actual.firstKey());
    }

    public void testGetAllModulesStartingWithEdges1() {
        ModulesKey k1 = new ModulesKey("foo\uffffbar", null);
        ModulesKey k2 = new ModulesKey("food", null);
        ProjectModulesManager manager = new ProjectModulesManager();
        manager.addModule(k1);
        manager.addModule(k2);
        SortedMap<ModulesKey, ModulesKey> actual = manager.getAllDirectModulesStartingWith("foo");
        assertEquals(2, actual.size());
        assertEquals(k2, actual.firstKey());
        assertEquals(k1, actual.keySet().toArray()[1]);
    }

    public void testGetAllModulesStartingWithEdges2() {
        ModulesKey k1 = new ModulesKey("foo\uffffbar", null);
        ProjectModulesManager manager = new ProjectModulesManager();
        manager.addModule(k1);
        SortedMap<ModulesKey, ModulesKey> actual = manager.getAllDirectModulesStartingWith("food");
        assertEquals(0, actual.size());
    }

    // Checks that removal is effective.
    public void testGetAllModulesStartingWithRemoval() {
        ModulesKey k1 = new ModulesKey("a", null);
        ModulesKey k2 = new ModulesKey("ab", null);
        ProjectModulesManager manager = new ProjectModulesManager();
        manager.addModule(k1);
        manager.addModule(k2);
        List<ModulesKey> modulesToRemove = new ArrayList<>();
        modulesToRemove.add(k2);
        manager.removeModules(modulesToRemove);
        SortedMap<ModulesKey, ModulesKey> actual = manager.getAllDirectModulesStartingWith("a");
        assertEquals(1, actual.size());
        assertEquals(k1, actual.firstKey());
        assertEquals(0, manager.getAllDirectModulesStartingWith("ab").size());
    }

    /**
     * Helper to check for membership.
     */
    private void assertContainsAll(Set<String> result, String... expected) {
        assertEquals(expected.length, result.size());
        for (String e : expected) {
            assertTrue(result.contains(e));
        }
    }
}
