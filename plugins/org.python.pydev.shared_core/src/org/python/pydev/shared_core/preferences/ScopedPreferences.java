/**
 * Copyright (c) 2014-2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under11 the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.preferences;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.osgi.framework.Bundle;
import org.python.pydev.shared_core.cache.LRUCache;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.OrderedSet;
import org.python.pydev.shared_core.structure.Tuple;
import org.yaml.snakeyaml.Yaml;

public final class ScopedPreferences implements IScopedPreferences {

    private static final Map<String, IScopedPreferences> yamlFileNameToPreferences = new HashMap<String, IScopedPreferences>();
    private static final Object lock = new Object();

    public static IScopedPreferences get(final String yamlFileName) {
        IScopedPreferences ret = yamlFileNameToPreferences.get(yamlFileName);
        if (ret == null) {
            synchronized (lock) {
                ret = new ScopedPreferences(yamlFileName);
                yamlFileNameToPreferences.put(yamlFileName, ret);
            }
        }
        return ret;
    }

    public static String USER_HOME_IN_TESTS = null;
    public static String WORKSPACE_DIR_IN_TESTS = null;

    private String yamlFileName;
    private File[] trackedDirs;
    private File defaultSettingsDir = null;
    private File workspaceDir = null;

    public ScopedPreferences(String yamlFileName) {
        this.yamlFileName = yamlFileName;
        Set<File> set = new OrderedSet<File>();

        try {
            if (WORKSPACE_DIR_IN_TESTS != null) {
                workspaceDir = new File(WORKSPACE_DIR_IN_TESTS, yamlFileName + ".yaml");
            } else {
                Bundle bundle = Platform.getBundle("org.python.pydev.shared_core");
                if (bundle != null) {
                    IPath stateLocation = Platform.getStateLocation(bundle);
                    workspaceDir = new File(stateLocation.toFile(), yamlFileName + ".yaml");
                }
            }
        } catch (Exception e1) {
            Log.log(e1);
        }

        //Default paths always there!
        String userHome;
        if (USER_HOME_IN_TESTS == null) {
            userHome = System.getProperty("user.home");
        } else {
            userHome = USER_HOME_IN_TESTS;
        }
        if (userHome != null) {
            try {
                File f = new File(userHome);
                if (f.isDirectory()) {
                    f = new File(f, ".eclipse");
                    try {
                        if (!f.exists()) {
                            f.mkdirs();
                        }
                    } catch (Exception e) {
                        Log.log(e);
                    }
                    if (f.isDirectory()) {
                        set.add(f);
                        defaultSettingsDir = f;
                    }
                }
            } catch (Throwable e) {
                Log.log(e);
            }
        }
        if (set.size() == 0) {
            Log.log("System.getProperty(\"user.home\") returned " + userHome + " which is not a directory!");
        }

        // TODO: Add support later on.
        // ScopedPreferenceStore workspaceSettings = new ScopedPreferenceStore(InstanceScope.INSTANCE, yamlFileName);
        // String string = workspaceSettings.getString("ADDITIONAL_TRACKED_DIRS");
        // //Load additional tracked dirs
        // for (String s : StringUtils.split(string, '|')) {
        //     set.add(new File(s));
        // }
        this.trackedDirs = set.toArray(new File[0]);
    }

    @Override
    public File getUserSettingsLocation() {
        return new File(defaultSettingsDir, yamlFileName + ".yaml");
    }

    @Override
    public File getWorkspaceSettingsLocation() {
        return workspaceDir;
    }

    @Override
    public Tuple<Map<String, Object>, Set<String>> loadFromUserSettings(Map<String, Object> saveData) throws Exception {
        Map<String, Object> o1 = new HashMap<>();
        Set<String> o2 = new HashSet<>();
        Tuple<Map<String, Object>, Set<String>> ret = new Tuple<>(o1, o2);

        File yamlFile = getUserSettingsLocation();
        Map<String, Object> loaded = getYamlFileContents(yamlFile);
        if (loaded != null) {
            Set<Entry<String, Object>> initialEntrySet = saveData.entrySet();
            for (Entry<String, Object> entry : initialEntrySet) {
                Object loadedObj = loaded.get(entry.getKey());
                if (loadedObj == null) {
                    //not in loaded file
                    o2.add(entry.getKey());
                } else {
                    o1.put(entry.getKey(), convertValueToTypeOfOldValue(loadedObj, entry.getValue()));
                }
            }
        }
        return ret;
    }

    @Override
    public Tuple<Map<String, Object>, Set<String>> loadFromProjectSettings(Map<String, Object> saveData,
            IProject project) throws Exception {
        Map<String, Object> o1 = new HashMap<>();
        Set<String> o2 = new HashSet<>();
        Tuple<Map<String, Object>, Set<String>> ret = new Tuple<>(o1, o2);
        IFile yamlFile = getProjectConfigFile(project, yamlFileName + ".yaml", false);

        if (yamlFile.exists()) {
            Map<String, Object> loaded = getYamlFileContents(yamlFile);
            Set<Entry<String, Object>> initialEntrySet = saveData.entrySet();
            for (Entry<String, Object> entry : initialEntrySet) {
                Object loadedObj = loaded.get(entry.getKey());
                if (loadedObj == null) {
                    //not in loaded file
                    o2.add(entry.getKey());
                } else {
                    o1.put(entry.getKey(), convertValueToTypeOfOldValue(loadedObj, entry.getValue()));
                }
            }
        }
        return ret;
    }

    @Override
    public String saveToUserSettings(Map<String, Object> saveData) throws Exception {
        if (defaultSettingsDir == null) {
            throw new Exception("user.home is not available!");
        }
        if (!defaultSettingsDir.isDirectory()) {
            throw new Exception("user.home/.settings: " + defaultSettingsDir + "is not a directory!");
        }
        Map<String, Object> yamlMapToWrite = new TreeMap<>();
        Set<Entry<String, Object>> entrySet = saveData.entrySet();
        for (Entry<String, Object> entry : entrySet) {
            yamlMapToWrite.put(entry.getKey(), entry.getValue());
        }
        saveData = null; // make sure we don't use it anymore
        File yamlFile = new File(defaultSettingsDir, yamlFileName + ".yaml");
        if (yamlFile.exists()) {
            try {
                Map<String, Object> initial = new HashMap<>(getYamlFileContents(yamlFile));
                initial.putAll(yamlMapToWrite);
                yamlMapToWrite = new TreeMap<>(initial);
            } catch (Exception e) {
                throw new Exception(
                        StringUtils
                                .format("Error: unable to write settings because the file: %s already exists but "
                                        + "is not a parseable YAML file (aborting to avoid overriding existing file).",
                                        yamlFile), e);
            }
        }

        dumpSaveDataToFile(yamlMapToWrite, yamlFile);
        return "Contents saved to:\n" + yamlFile;
    }

    @Override
    public String saveToProjectSettings(Map<String, Object> saveData, IProject... projects) {
        FastStringBuffer buf = new FastStringBuffer();

        int createdForNProjects = 0;

        for (IProject project : projects) {
            try {
                IFile projectConfigFile = getProjectConfigFile(project, yamlFileName + ".yaml", true);
                if (projectConfigFile == null) {
                    buf.append("Unable to get config file location for: ").append(project.getName()).append("\n");
                    continue;
                }
                if (projectConfigFile.exists()) {
                    Map<String, Object> yamlFileContents = null;
                    try {
                        yamlFileContents = getYamlFileContents(projectConfigFile);
                    } catch (Exception e) {
                        throw new Exception(
                                StringUtils
                                        .format("Error: unable to write settings because the file: %s already exists but "
                                                + "is not a parseable YAML file (aborting to avoid overriding existing file).\n",
                                                projectConfigFile), e);

                    }
                    Map<String, Object> yamlMapToWrite = new TreeMap<>();
                    Set<Entry<String, Object>> entrySet = yamlFileContents.entrySet();
                    for (Entry<String, Object> entry : entrySet) {
                        yamlMapToWrite.put(entry.getKey(), entry.getValue());
                    }
                    yamlMapToWrite.putAll(saveData);
                    dumpSaveDataToFile(yamlMapToWrite, projectConfigFile, true);
                    createdForNProjects += 1;
                    continue;
                } else {
                    //Create file
                    dumpSaveDataToFile(saveData, projectConfigFile, false);
                    createdForNProjects += 1;
                }

            } catch (Exception e) {
                Log.log(e);
                buf.append(e.getMessage());
            }
        }
        if (createdForNProjects > 0) {
            buf.insert(0, "Operation succeeded for " + createdForNProjects + " projects.\n");
        }
        return buf.toString();
    }

    private void dumpSaveDataToFile(Map<String, Object> saveData, IFile yamlFile, boolean exists) throws IOException,
            CoreException {
        Yaml yaml = new Yaml();
        String dumpAsMap = yaml.dumpAsMap(saveData);
        if (!exists) {
            // Create empty (so that we can set the charset properly later on).
            yamlFile.create(new ByteArrayInputStream("".getBytes()), true, new NullProgressMonitor());
        }
        yamlFile.setCharset("UTF-8", new NullProgressMonitor());
        yamlFile.setContents(new ByteArrayInputStream(dumpAsMap.getBytes(Charset.forName("UTF-8"))), true, true,
                new NullProgressMonitor());
    }

    private void dumpSaveDataToFile(Map<String, Object> saveData, File yamlFile) throws IOException {
        Yaml yaml = new Yaml();
        String dumpAsMap = yaml.dumpAsMap(saveData);
        FileUtils.writeStrToFile(dumpAsMap, yamlFile);
        // Don't use the code below because we want to dump as a map to have a better layout for the file.
        //
        // try (Writer output = new FileWriter(yamlFile)) {
        //     yaml.dump(saveData, new BufferedWriter(output));
        // }
    }

    @Override
    public IFile getProjectSettingsLocation(IProject p) {
        return getProjectConfigFile(p, yamlFileName + ".yaml", false);
    }

    /**
     * Returns the contents of the configuration file to be used or null.
     */
    private static IFile getProjectConfigFile(IProject project, String filename, boolean createPath) {
        try {
            if (project != null && project.exists()) {
                IFolder folder = project.getFolder(".settings");
                if (createPath) {
                    if (!folder.exists()) {
                        folder.create(true, true, new NullProgressMonitor());
                    }
                }
                return folder.getFile(filename);
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

    //TODO: We may want to have some caches...
    //long modificationStamp = projectConfigFile.getModificationStamp();

    @Override
    public String getString(IPreferenceStore pluginPreferenceStore, String keyInPreferenceStore, IAdaptable adaptable) {
        Object object = getFromProjectOrUserSettings(keyInPreferenceStore, adaptable);
        if (object != null) {
            return object.toString();
        }
        // Ok, not in project or user settings: get it from the workspace settings.
        return pluginPreferenceStore.getString(keyInPreferenceStore);
    }

    @Override
    public boolean getBoolean(IPreferenceStore pluginPreferenceStore, String keyInPreferenceStore, IAdaptable adaptable) {
        Object object = getFromProjectOrUserSettings(keyInPreferenceStore, adaptable);
        if (object != null) {
            return toBoolean(object);
        }
        // Ok, not in project or user settings: get it from the workspace settings.
        return pluginPreferenceStore.getBoolean(keyInPreferenceStore);
    }

    @Override
    public int getInt(IPreferenceStore pluginPreferenceStore, String keyInPreferenceStore, IAdaptable adaptable) {
        Object object = getFromProjectOrUserSettings(keyInPreferenceStore, adaptable);
        if (object != null) {
            return toInt(object);
        }
        // Ok, not in project or user settings: get it from the workspace settings.
        return pluginPreferenceStore.getInt(keyInPreferenceStore);
    }

    private Object getFromProjectOrUserSettings(String keyInPreferenceStore, IAdaptable adaptable) {
        // In the yaml all keys are lowercase!
        String keyInYaml = keyInPreferenceStore;

        if (adaptable != null) {
            try {
                IProject project;
                if (adaptable instanceof IResource) {
                    project = ((IResource) adaptable).getProject();
                } else {
                    project = (IProject) adaptable.getAdapter(IProject.class);
                }
                IFile projectConfigFile = getProjectSettingsLocation(project);
                if (projectConfigFile != null && projectConfigFile.exists()) {
                    Map<String, Object> yamlFileContents = null;
                    try {
                        yamlFileContents = getYamlFileContents(projectConfigFile);
                    } catch (Exception e) {
                        Log.log(e);
                    }
                    if (yamlFileContents != null) {
                        Object object = yamlFileContents.get(keyInYaml);
                        if (object != null) {
                            return object;
                        }
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }

        // If it got here, it's not in the project, let's try in the user settings...
        for (File dir : trackedDirs) {
            try {
                File yaml = new File(dir, yamlFileName + ".yaml");
                Map<String, Object> yamlFileContents = null;
                try {
                    yamlFileContents = getYamlFileContents(yaml);
                } catch (Exception e) {
                    Log.log(e);
                }
                if (yamlFileContents != null) {
                    Object object = yamlFileContents.get(keyInYaml);
                    if (object != null) {
                        return object;
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        return null;
    }

    public static boolean toBoolean(Object found) {
        if (found == null) {
            return false;
        }
        if (Boolean.FALSE.equals(found)) {
            return false;
        }
        String asStr = found.toString();

        if ("false".equals(asStr) || "False".equals(asStr) || "0".equals(asStr) || asStr.trim().length() == 0) {
            return false;
        }
        return true;
    }

    public static int toInt(Object found) {
        if (found == null) {
            return 0;
        }
        if (found instanceof Integer) {
            return (int) found;
        }

        String asStr = found.toString();
        try {
            return Integer.parseInt(asStr);
        } catch (Exception e) {
            Log.log(e);
            return 0;
        }
    }

    private Object convertValueToTypeOfOldValue(Object loadedObj, Object oldValue) {
        if (oldValue == null) {
            return loadedObj; // Unable to do anything in this case...
        }
        if (loadedObj == null) {
            return null; // Nothing to see?
        }
        if (oldValue instanceof Boolean) {
            return toBoolean(loadedObj);
        }
        if (oldValue instanceof Integer) {
            return toInt(loadedObj);
        }
        if (oldValue instanceof String) {
            return loadedObj.toString();
        }
        throw new RuntimeException("Unable to handle type conversion to: " + oldValue.getClass());
    }

    LRUCache<Object, Map<String, Object>> cache = new LRUCache<>(15);
    LRUCache<Object, Long> lastSeenCache = new LRUCache<>(15);

    private Map<String, Object> getCachedYamlFileContents(Object key, long currentSeen, ICallback0<Object> iCallback0)
            throws Exception {
        Long lastSeen = lastSeenCache.getObj(key);
        if (lastSeen != null) {
            if (lastSeen != currentSeen) {
                cache.remove(key);
            }
        }

        Map<String, Object> obj = cache.getObj(key);
        if (obj != null) {
            return obj;
        }

        // Ok, not in cache...
        Map<String, Object> ret = (Map<String, Object>) iCallback0.call();
        lastSeenCache.add(key, currentSeen);
        cache.add(key, ret);
        return ret;
    }

    /**
     * A number of exceptions may happen when loading the contents...
     */
    private Map<String, Object> getYamlFileContents(final IFile projectConfigFile) throws Exception {
        return getCachedYamlFileContents(projectConfigFile, projectConfigFile.getModificationStamp(),
                new ICallback0<Object>() {

                    @Override
                    public Object call() {
                        IDocument fileContents = getFileContents(projectConfigFile);
                        String yamlContents = fileContents.get();
                        try {
                            return getYamlFileContentsImpl(yamlContents);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

    @Override
    public Map<String, Object> getYamlFileContents(final File yamlFile) throws Exception {
        if (!yamlFile.exists()) {
            return null;
        }
        //Using this API to get a higher precision!
        FileTime ret = Files.getLastModifiedTime(yamlFile.toPath());
        long lastModified = ret.to(TimeUnit.NANOSECONDS);

        return getCachedYamlFileContents(yamlFile, lastModified,
                new ICallback0<Object>() {

                    @Override
                    public Object call() {
                        try {
                            String fileContents = FileUtils.getFileContents(yamlFile);
                            Map<String, Object> initial = getYamlFileContentsImpl(fileContents);
                            return initial;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

    }

    /**
     * A number of exceptions may happen when loading the contents...
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getYamlFileContentsImpl(String yamlContents) throws Exception {
        if (yamlContents.trim().length() == 0) {
            return new HashMap<String, Object>();
        }
        Yaml yaml = new Yaml();
        Object load = yaml.load(yamlContents);
        if (!(load instanceof Map)) {
            if (load == null) {
                throw new Exception("Expected top-level element to be a map. Found: null");
            }
            throw new Exception("Expected top-level element to be a map. Found: " + load.getClass());
        }
        //As this object is from our internal cache, make it unmodifiable!
        return Collections.unmodifiableMap((Map<String, Object>) load);
    }

    private IDocument getFileContents(IFile file) {
        return FileUtils.getDocFromResource(file);
    }

}
