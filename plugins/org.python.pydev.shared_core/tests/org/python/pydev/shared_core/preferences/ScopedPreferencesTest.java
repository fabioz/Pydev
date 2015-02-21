/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under1 the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.preferences;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.resource_stubs.ProjectStub;

public class ScopedPreferencesTest extends TestCase {

    private File baseDir;

    @Override
    protected void setUp() throws Exception {
        FileUtils.IN_TESTS = true;
        baseDir = new File(FileUtils.getFileAbsolutePath(new File("ScopedPreferencesTest.temporary_dir")));
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }
        if (baseDir.exists()) {
            throw new AssertionError("Not expecting: " + baseDir + " to exist.");
        }
        baseDir.mkdirs();
        ScopedPreferences.USER_HOME_IN_TESTS = baseDir.getAbsolutePath();
        ScopedPreferences.WORKSPACE_DIR_IN_TESTS = new File(baseDir, "workspace").getAbsolutePath();
    }

    @Override
    protected void tearDown() throws Exception {
        ScopedPreferences.USER_HOME_IN_TESTS = null;
        ScopedPreferences.WORKSPACE_DIR_IN_TESTS = null;
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }
    }

    public void testUserSettingsScopedPreferences() throws Exception {
        IScopedPreferences iScopedPreferences = ScopedPreferences.get("my.test");
        File eclipsePrefs = new File(baseDir, ".eclipse");
        assertTrue(eclipsePrefs.exists());
        File userSettingsYamlFile = new File(eclipsePrefs, "my.test.yaml");
        assertTrue(!userSettingsYamlFile.exists());
        Map<String, Object> saveData = new HashMap<String, Object>();
        saveData.put("foo", 1);
        iScopedPreferences.saveToUserSettings(saveData);
        assertTrue(userSettingsYamlFile.exists());
        IAdaptable adaptable = new IAdaptable() {

            @Override
            public Object getAdapter(Class adapter) {
                return null;
            }
        };
        IPreferenceStore pluginPreferenceStore = new NullPrefsStore();
        assertEquals(1, iScopedPreferences.getInt(pluginPreferenceStore, "foo", adaptable));
        assertEquals("foo: 1\n", FileUtils.getFileContents(userSettingsYamlFile));
        saveData = new HashMap<String, Object>();
        saveData.put("bar", 2);
        iScopedPreferences.saveToUserSettings(saveData);
        assertEquals("bar: 2\nfoo: 1\n", FileUtils.getFileContents(userSettingsYamlFile));
        assertEquals(2, iScopedPreferences.getInt(pluginPreferenceStore, "bar", adaptable));
        FileUtils.writeStrToFile("bar: 1\nfoo: 1\n", userSettingsYamlFile);
        assertEquals(1, iScopedPreferences.getInt(pluginPreferenceStore, "bar", adaptable));
    }

    public void testProjectSettingsScopedPreferences() throws Exception {
        IScopedPreferences iScopedPreferences = ScopedPreferences.get("my.test");
        File eclipsePrefs = new File(baseDir, ".eclipse");
        File projectDir = new File(baseDir, "project");
        File projectDirSettings = new File(projectDir, ".settings");
        File projectDirYAMLFile = new File(projectDirSettings, "my.test.yaml");
        eclipsePrefs.mkdirs();
        projectDir.mkdirs();
        projectDirSettings.mkdirs();
        FileUtils.writeStrToFile("", projectDirYAMLFile);
        assertTrue(eclipsePrefs.exists());
        File userSettingsYamlFile = new File(eclipsePrefs, "my.test.yaml");
        assertTrue(!userSettingsYamlFile.exists());
        final IProject project = new ProjectStub(projectDir, null);
        Map<String, Object> saveData = new HashMap<String, Object>();
        saveData.put("foo", 1);
        iScopedPreferences.saveToProjectSettings(saveData, project);
        assertTrue(!userSettingsYamlFile.exists());
        assertEquals("foo: 1\n", FileUtils.getFileContents(projectDirYAMLFile));

        IAdaptable adaptable = new IAdaptable() {

            @Override
            public Object getAdapter(Class adapter) {
                if (IProject.class == adapter) {
                    return project;
                }
                return null;
            }
        };
        IPreferenceStore pluginPreferenceStore = new NullPrefsStore();
        assertEquals(1, iScopedPreferences.getInt(pluginPreferenceStore, "foo", adaptable));
        saveData = new HashMap<String, Object>();
        saveData.put("bar", 2);
        iScopedPreferences.saveToProjectSettings(saveData, project);
        assertEquals("bar: 2\nfoo: 1\n", FileUtils.getFileContents(projectDirYAMLFile));
        assertEquals(2, iScopedPreferences.getInt(pluginPreferenceStore, "bar", adaptable));
        FileUtils.writeStrToFile("bar: 1\nfoo: 1\n", projectDirYAMLFile);
        assertEquals(1, iScopedPreferences.getInt(pluginPreferenceStore, "bar", adaptable));
        FileUtils.writeStrToFile("foo: 1\n", projectDirYAMLFile);
        assertEquals(0, iScopedPreferences.getInt(pluginPreferenceStore, "bar", adaptable)); // default in NullPrefsStore
        pluginPreferenceStore.setValue("bar", 2);
        assertEquals(2, iScopedPreferences.getInt(pluginPreferenceStore, "bar", adaptable)); // default in NullPrefsStore
    }
}
