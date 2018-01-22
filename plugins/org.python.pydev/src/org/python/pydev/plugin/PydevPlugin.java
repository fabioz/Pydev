/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.AbstractTemplateCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.ModulesManager;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.SyncSystemModulesManagerScheduler;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JavaProjectModulesManagerCreator;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JythonModulesManagerUtils;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModuleForZip;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.hover.PyEditorTextHoverDescriptor;
import org.python.pydev.editor.hover.PyHoverPreferencesPage;
import org.python.pydev.editor.hover.PydevCombiningHover;
import org.python.pydev.editor.templates.PyContextType;
import org.python.pydev.editor.templates.TemplateHelper;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_ui.ColorCache;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.bundle.BundleInfo;
import org.python.pydev.shared_ui.bundle.IBundleInfo;
import org.python.pydev.ui.interpreters.IronpythonInterpreterManager;
import org.python.pydev.ui.interpreters.JythonInterpreterManager;
import org.python.pydev.ui.interpreters.PythonInterpreterManager;

/**
 * The main plugin class - initialized on startup - has resource bundle for internationalization - has preferences
 */
public class PydevPlugin extends AbstractUIPlugin {

    private PyEditorTextHoverDescriptor[] fPyEditorTextHoverDescriptors;

    public static String getVersion() {
        try {
            return Platform.getBundle("org.python.pydev").getHeaders().get("Bundle-Version");
        } catch (Exception e) {
            Log.log(e);
            return "Unknown";
        }
    }

    public static IBundleInfo info;

    public static IBundleInfo getBundleInfo() {
        if (PydevPlugin.info == null) {
            PydevPlugin.info = new BundleInfo(PydevPlugin.getDefault().getBundle());
        }
        return PydevPlugin.info;
    }

    public static void setBundleInfo(IBundleInfo b) {
        PydevPlugin.info = b;
    }

    private static PydevPlugin plugin; //The shared instance.

    private ColorCache colorCache;

    private ResourceBundle resourceBundle; //Resource bundle.

    public final SyncSystemModulesManagerScheduler syncScheduler = new SyncSystemModulesManagerScheduler();

    private boolean isAlive;

    private static PyEditorTextHoverDescriptor combiningHoverDescriptor;

    /**
     * The constructor.
     */
    public PydevPlugin() {
        super();
        plugin = this;
    }

    Job startSynchSchedulerJob = new Job("SynchScheduler start") {

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            syncScheduler.start();
            return Status.OK_STATUS;
        }
    };

    /**
     * @return true if PyEdit.EDITOR_ID is set as the persistent property (only if the file does not have an extension).
     */
    public static boolean markAsPyDevFileIfDetected(IFile file) {
        String name = file.getName();
        if (name == null || name.indexOf('.') != -1) {
            return false;
        }

        String editorID;
        try {
            editorID = file.getPersistentProperty(IDE.EDITOR_KEY);
            if (editorID == null) {
                InputStream contents = file.getContents(true);
                Reader inputStreamReader = new InputStreamReader(new BufferedInputStream(contents));
                if (FileUtils.hasPythonShebang(inputStreamReader)) {
                    IDE.setDefaultEditor(file, IPyEdit.EDITOR_ID);
                    return true;
                }
            } else {
                return IPyEdit.EDITOR_ID.equals(editorID);
            }

        } catch (Exception e) {
            if (file.exists()) {
                Log.log(e);
            }
        }
        return false;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.isAlive = true;
        super.start(context);

        // Setup extensions in dependencies (could actually be done as extension points, but done like this for
        // ease of implementation right now).
        AbstractTemplateCodeCompletion.getTemplateContextType = () -> TemplateHelper.getContextTypeRegistry()
                .getContextType(PyContextType.PY_COMPLETIONS_CONTEXT_TYPE);

        PydevPrefs.getDefaultStores = (addEditorsUIStore) -> {
            List<IPreferenceStore> stores = new ArrayList<IPreferenceStore>();
            stores.add(PydevPlugin.getDefault().getPreferenceStore());
            if (addEditorsUIStore) {
                stores.add(EditorsUI.getPreferenceStore());
            }
            return stores;
        };

        PydevPrefs.getPreferenceStore = () -> PydevPlugin.getDefault().getPreferenceStore();

        PydevPrefs.getChainedPrefStore = () -> {
            List<IPreferenceStore> stores = PydevPrefs.getDefaultStores(true);
            return new ChainedPreferenceStore(
                    stores.toArray(new IPreferenceStore[stores.size()]));

        };

        ProjectModulesManager.createJavaProjectModulesManagerIfPossible = (
                IProject project) -> JavaProjectModulesManagerCreator
                        .createJavaProjectModulesManagerIfPossible(project);

        ModulesManager.createModuleFromJar = (EmptyModuleForZip emptyModuleForZip,
                IPythonNature nature) -> JythonModulesManagerUtils.createModuleFromJar(emptyModuleForZip, nature);

        CorePlugin.pydevStatelocation = Platform.getStateLocation(getBundle()).toFile();
        // End setup extension in dependencies

        try {
            resourceBundle = ResourceBundle.getBundle("org.python.pydev.PyDevPluginResources");
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
        final IPreferenceStore preferences = plugin.getPreferenceStore();

        //set them temporarily
        //setPythonInterpreterManager(new StubInterpreterManager(true));
        //setJythonInterpreterManager(new StubInterpreterManager(false));

        //changed: the interpreter manager is always set in the initialization (initialization
        //has some problems if that's not done).
        InterpreterManagersAPI.setPythonInterpreterManager(new PythonInterpreterManager(preferences));
        InterpreterManagersAPI.setJythonInterpreterManager(new JythonInterpreterManager(preferences));
        InterpreterManagersAPI.setIronpythonInterpreterManager(new IronpythonInterpreterManager(preferences));

        //This is usually fast, but in lower end machines it could be a bit slow, so, let's do it in a job to make sure
        //that the plugin is properly initialized without any delays.
        startSynchSchedulerJob.schedule(1000);

        //restore the nature for all python projects -- that's done when the project is set now.
        //        new Job("PyDev: Restoring projects python nature"){
        //
        //            protected IStatus run(IProgressMonitor monitor) {
        //                try{
        //
        //                    IProject[] projects = getWorkspace().getRoot().getProjects();
        //                    for (int i = 0; i < projects.length; i++) {
        //                        IProject project = projects[i];
        //                        try {
        //                            if (project.isOpen() && project.hasNature(PythonNature.PYTHON_NATURE_ID)) {
        //                                PythonNature.addNature(project, monitor, null, null);
        //                            }
        //                        } catch (Exception e) {
        //                            PydevPlugin.log(e);
        //                        }
        //                    }
        //                }catch(Throwable t){
        //                    t.printStackTrace();
        //                }
        //                return Status.OK_STATUS;
        //            }
        //
        //        }.schedule();

    }

    private Set<String> erasePrefixes = new HashSet<String>();

    private FormToolkit fDialogsFormToolkit;

    public File getTempFile(String prefix) {
        erasePrefixes.add(prefix);
        IPath stateLocation = getStateLocation();
        File file = stateLocation.toFile();
        File tempFileAt = FileUtils.getTempFileAt(file, prefix);
        return tempFileAt;
    }

    /**
     * This is called when the plugin is being stopped.
     *
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        syncScheduler.stop();
        IPath stateLocation = getStateLocation();
        File file = stateLocation.toFile();
        for (String prefix : erasePrefixes) {
            FileUtils.clearTempFilesAt(file, prefix);
        }
        this.isAlive = false;
        try {
            //stop the running shells
            AbstractShell.shutdownAllShells();

            //save the natures (code completion stuff) -- and only the ones initialized
            //(no point in getting the ones not initialized)
            for (PythonNature nature : PythonNature.getInitializedPythonNatures()) {
                try {
                    nature.saveAstManager();
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        } finally {
            super.stop(context);
        }
    }

    public static boolean isAlive() {
        PydevPlugin p = plugin;
        if (p == null) {
            return false;
        }
        return p.isAlive;
    }

    public static PydevPlugin getDefault() {
        return plugin;
    }

    public static String getPluginID() {
        if (SharedCorePlugin.inTestMode()) {
            return "PyDevPluginID(null plugin)";
        }
        return PydevPlugin.getBundleInfo().getPluginID();
    }

    /**
     * Returns the workspace instance.
     */
    public static IWorkspace getWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not found.
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = plugin.getResourceBundle();
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    private static ImageCache imageCache = null;

    /**
     * @return the cache that should be used to access images within the pydev plugin.
     */
    public static ImageCache getImageCache() {
        if (imageCache == null) {
            imageCache = PydevPlugin.getBundleInfo().getImageCache();
        }
        return imageCache;
    }

    public ImageDescriptor getImageDescriptor(String key) {
        return getImageRegistry().getDescriptor(key);
    }

    //End Images for the console

    /**
     * @return
     */
    public static ColorCache getColorCache() {
        PydevPlugin plugin = getDefault();
        if (plugin.colorCache == null) {
            final IPreferenceStore chainedPrefStore = PydevPrefs.getChainedPrefStore();
            plugin.colorCache = new ColorCache(chainedPrefStore) {
                {
                    chainedPrefStore.addPropertyChangeListener(new IPropertyChangeListener() {

                        @Override
                        public void propertyChange(PropertyChangeEvent event) {
                            if (fNamedColorTable.containsKey(event.getProperty())) {
                                reloadProperty(event.getProperty());
                            }
                        }
                    });
                }
            };
        }
        return plugin.colorCache;
    }

    public static void setCssId(Object control, String id, boolean applyToChildren) {
        SharedUiPlugin.setCssId(control, id, applyToChildren);
    }

    public static void fixSelectionStatusDialogStatusLineColor(Object dialog, Color color) {
        SharedUiPlugin.fixSelectionStatusDialogStatusLineColor(dialog, color);
    }

    /**
     * Returns all PyDev editor text hovers contributed to the workbench.
     *
     * @return an array of PyEditorTextHoverDescriptor
     */
    public synchronized PyEditorTextHoverDescriptor[] getPyEditorTextHoverDescriptors() {
        if (fPyEditorTextHoverDescriptors == null) {
            fPyEditorTextHoverDescriptors = PyEditorTextHoverDescriptor
                    .getContributedHovers();
            ConfigurationElementAttributeSorter sorter = new ConfigurationElementAttributeSorter() {
                /*
                 * @see org.eclipse.ui.texteditor.ConfigurationElementSorter#getConfigurationElement(java.lang.Object)
                 */
                @Override
                public IConfigurationElement getConfigurationElement(Object object) {
                    return ((PyEditorTextHoverDescriptor) object).getConfigurationElement();
                }
            };
            sorter.sort(fPyEditorTextHoverDescriptors, PyEditorTextHoverDescriptor.ATT_PYDEV_HOVER_PRIORITY);
        }

        return fPyEditorTextHoverDescriptors;
    }

    /**
     * Flushes the instance scope of this plug-in.
     */
    public static void flushInstanceScope() {
        try {
            InstanceScope.INSTANCE.getNode(PydevPlugin.getPluginID()).flush();
        } catch (BackingStoreException e) {
            Log.log(e);
        }
    }

    public FormToolkit getDialogsFormToolkit() {
        if (fDialogsFormToolkit == null) {
            FormColors colors = new FormColors(Display.getCurrent());
            colors.setBackground(null);
            colors.setForeground(null);
            fDialogsFormToolkit = new FormToolkit(colors);
        }
        return fDialogsFormToolkit;
    }

    /**
     * Resets the PyDev editor text hovers contributed to the workbench.
     * <p>
     * This will force a rebuild of the descriptors the next time
     * a client asks for them.
     * </p>
     */
    public synchronized void resetPyEditorTextHoverDescriptors() {
        fPyEditorTextHoverDescriptors = null;
        combiningHoverDescriptor = null;
    }

    public static PyEditorTextHoverDescriptor getCombiningHoverDescriptor() {
        if (combiningHoverDescriptor == null) {
            combiningHoverDescriptor = new PyEditorTextHoverDescriptor(new PydevCombiningHover());
            initializeDefaultCombiningHoverPreferences();
            PyEditorTextHoverDescriptor.initializeHoversFromPreferences(
                    new PyEditorTextHoverDescriptor[] { combiningHoverDescriptor });
        }
        return combiningHoverDescriptor;
    }

    private static void initializeDefaultCombiningHoverPreferences() {
        PydevPrefs.getPreferenceStore().setDefault(
                PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER + PydevPlugin.getCombiningHoverDescriptor().getId(),
                PyEditorTextHoverDescriptor.NO_MODIFIER);
        PydevPrefs.getPreferenceStore().setDefault(
                PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER_MASK + PydevPlugin.getCombiningHoverDescriptor().getId(),
                PyEditorTextHoverDescriptor.DEFAULT_MODIFIER_MASK);
        PydevPrefs.getPreferenceStore().setDefault(
                PyHoverPreferencesPage.KEY_TEXT_HOVER_PRIORITY + PydevPlugin.getCombiningHoverDescriptor().getId(),
                PyEditorTextHoverDescriptor.HIGHEST_PRIORITY);
        PydevPrefs.getPreferenceStore().setDefault(
                PyHoverPreferencesPage.KEY_TEXT_HOVER_ENABLE + PydevPlugin.getCombiningHoverDescriptor().getId(),
                true);
    }

}