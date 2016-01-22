package org.python.pydev.shared_core.preferences;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This is an API which takes care of getting preferences we want from a proper place.
 *
 * Some use-cases:
 *
 * - Get whether we should do code-formatting based on a configuration which is saved:
 *     1. In the project (i.e.: .settings/org.python.pydev.yaml)
 *     2. In the user-configuration (i.e.: user.home/.eclipse/org.python.pydev.yaml)
 *     3. In the workspace (which is the Eclipse standard)
 *
 * - Get additional templates (templates should be a collection of templates in project, user configuration, workspace).
 *
 * - Automatically apply defaults from the user-configuration into the workspace settings
 *      (i.e.: %APPDATA%/EclipseSettings/override.workspacesettings)
 */
public interface IScopedPreferences {

    // Note: these settings are not on each call and should usually be passed in a constructor...

    // String pluginName:
    // pluginName the name of the plugin (from which the name of the file in the preferences is derived
    //  -- i.e.: org.python.pydev will get a %APPDATA%/EclipseSettings/org.python.pydev.yaml file)

    /**
     * @param pluginPreferenceStore the preferences store of the plugin (workspace setting)
     * @param keyInPreferenceStore the key to get from the workspace (if needed)
     * @param adaptable an adaptable which can adapt to IProject.
     */
    public boolean getBoolean(IPreferenceStore pluginPreferenceStore, String keyInPreferenceStore, IAdaptable adaptable);

    /**
     * @param pluginPreferenceStore the preferences store of the plugin (workspace setting)
     * @param keyInPreferenceStore the key to get from the workspace (if needed)
     * @param adaptable an adaptable which can adapt to IProject.
     */
    public int getInt(IPreferenceStore pluginPreferenceStore, String keyInPreferenceStore, IAdaptable adaptable);

    /**
     * @param pluginPreferenceStore the preferences store of the plugin (workspace setting)
     * @param keyInPreferenceStore the key to get from the workspace (if needed)
     * @param adaptable an adaptable which can adapt to IProject.
     */
    public String getString(IPreferenceStore pluginPreferenceStore, String keyInPreferenceStore, IAdaptable adaptable);

    /**
     * May throw an exception if it's not possible to save the passed data.
     *
     * Common reasons include not being able to write the file, abort overriding an existing (non-valid) yaml file...
     *
     * Returns a message which may be shown to the user with the confirmation of the save.
     */
    public String saveToUserSettings(Map<String, Object> saveData) throws Exception;

    /**
     * May throw an exception if it's not possible to load the passed data.
     *
     * Returns a tuple with the loaded values and a set with the values which weren't found in the user settings.
     * @throws Exception
     */
    public Tuple<Map<String, Object>, Set<String>> loadFromUserSettings(Map<String, Object> saveData) throws Exception;

    public String saveToProjectSettings(Map<String, Object> saveData, IProject... projects);

    public Tuple<Map<String, Object>, Set<String>> loadFromProjectSettings(Map<String, Object> saveData,
            IProject project) throws Exception;

    /**
     * Returns the .yaml file to be used for writing in the user settings.
     */
    public File getUserSettingsLocation();

    /**
     * Returns the .yaml file to be used for writing in the workspace settings.
     */
    public File getWorkspaceSettingsLocation();

    /**
     * Returns the .yaml file to be used for writing in the project settings.
     */
    public IFile getProjectSettingsLocation(IProject p);

    /**
     * Given a YAML file, returns its contents (always considers its contents to be a Map)
     *
     * Note: return null if the file does not exist!
     */
    public Map<String, Object> getYamlFileContents(final File yamlFile) throws Exception;
}
