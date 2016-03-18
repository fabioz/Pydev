/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.app_engine.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.python.pydev.customizations.CustomizationsPlugin;
import org.python.pydev.customizations.CustomizationsUIConstants;
import org.python.pydev.customizations.app_engine.launching.AppEngineConstants;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.ui.pythonpathconf.PythonSelectionLibrariesDialog;

/**
 * This wizard page gives the google app engine configuration settings.
 */
public class AppEngineConfigWizardPage extends WizardPage {

    private Label locationLabel;

    private Text locationPathField;

    private Button browseButton;

    private IPath initialLocationFieldValue;

    private String customLocationFieldValue;

    private static final int SIZING_TEXT_FIELD_WIDTH = 250;

    private Tree tree;

    private Image imageSystemLib;

    private Image imageAppEngine;

    private final List<String> externalSourceFolders = new ArrayList<String>();

    private final Map<String, String> variableSubstitution = new HashMap<String, String>();

    private Listener locationModifyListener = new Listener() {
        @Override
        public void handleEvent(Event e) {
            setPageComplete(validatePage());
        }
    };

    protected AppEngineConfigWizardPage(String pageName) {
        super(pageName);
        this.setPageComplete(false);

        initialLocationFieldValue = new Path("");
        customLocationFieldValue = "";

        imageAppEngine = CustomizationsPlugin.getImageCache().get(CustomizationsUIConstants.APP_ENGINE);
        imageSystemLib = PydevPlugin.getImageCache().get(UIConstants.LIB_SYSTEM);
    }

    @Override
    public void createControl(Composite parent) {

        Font font = parent.getFont();

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        composite.setFont(font);

        // App Engine specification group
        Composite appEngineGroup = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        appEngineGroup.setLayout(layout);
        appEngineGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        appEngineGroup.setFont(font);

        createUserSpecifiedGoogleAppEngineLocationGroup(appEngineGroup);

        tree = new Tree(composite, SWT.SINGLE | SWT.BORDER);
        tree.setLayoutData(new GridData(GridData.FILL_BOTH));
        tree.setFont(font);

        setControl(composite);
    }

    /**
     * Creates the app engine location specification controls.
     *
     * @param appEngineGroup the parent composite
     * @param enabled the initial enabled state of the widgets created
     */
    private void createUserSpecifiedGoogleAppEngineLocationGroup(Composite appEngineGroup) {
        Font font = appEngineGroup.getFont();
        // location label
        locationLabel = new Label(appEngineGroup, SWT.NONE);
        locationLabel.setFont(font);
        locationLabel.setText("Google App Engine Director&y");

        // app engine location entry field
        locationPathField = new Text(appEngineGroup, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = SIZING_TEXT_FIELD_WIDTH;
        locationPathField.setLayoutData(data);
        locationPathField.setFont(font);

        // browse button
        browseButton = new Button(appEngineGroup, SWT.PUSH);
        browseButton.setFont(font);
        browseButton.setText("B&rowse");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleLocationBrowseButtonPressed();
            }
        });

        // Set the initial value first before listener
        // to avoid handling an event during the creation.
        if (initialLocationFieldValue != null) {
            locationPathField.setText(initialLocationFieldValue.toOSString());
        }
        locationPathField.addListener(SWT.Modify, locationModifyListener);
    }

    /**
     *  Open an appropriate directory browser
     */
    private void handleLocationBrowseButtonPressed() {
        DirectoryDialog dialog = new DirectoryDialog(locationPathField.getShell());
        dialog.setMessage("Select the Google App Engine root directory (dir containing dev_appserver.py, appcfg.py, lib, etc).");

        String dirName = getAppEngineLocationFieldValue();
        if (!dirName.equals("")) { //$NON-NLS-1$
            File path = new File(dirName);
            if (path.exists()) {
                dialog.setFilterPath(new Path(dirName).toOSString());
            }
        }

        String selectedDirectory = dialog.open();
        if (selectedDirectory != null) {
            customLocationFieldValue = selectedDirectory;
            locationPathField.setText(customLocationFieldValue);
        }
    }

    /**
     * Returns the value of the app engine location field
     * with leading and trailing spaces removed.
     * 
     * @return the app engine location directory in the field
     */
    private String getAppEngineLocationFieldValue() {
        if (locationPathField == null) {
            return ""; //$NON-NLS-1$
        } else {
            return locationPathField.getText().trim();
        }
    }

    public void setAppEngineLocationFieldValue(String location) {
        locationPathField.setText(location);
    }

    /**
     * @return true if the page is valid and false otherwise.
     */
    private boolean validatePage() {
        tree.removeAll();
        externalSourceFolders.clear();
        variableSubstitution.clear();

        String locationFieldContents = getAppEngineLocationFieldValue();

        if (locationFieldContents.equals("")) { //$NON-NLS-1$
            setErrorMessage(null);
            setMessage("Google App Engine location is empty");
            return false;
        }

        IPath path = new Path(""); //$NON-NLS-1$
        if (!path.isValidPath(locationFieldContents)) {
            setErrorMessage("Google App Engine location is not valid");
            return false;
        }

        File loc = new File(locationFieldContents);
        if (!loc.exists()) {
            setErrorMessage("Google App Engine location does not exist");
            return false;
        }

        if (!loc.isDirectory()) {
            setErrorMessage("Expecting directory to be selected (not a file)");
            return false;
        }

        File[] files = loc.listFiles();
        HashMap<String, File> map = new HashMap<String, File>();
        if (files != null) {
            for (File f : files) {
                map.put(f.getName(), f);
            }
        }
        String[] preconditions = new String[] { "appcfg.py", "bulkload_client.py", "bulkloader.py", "dev_appserver.py",
                "VERSION", "lib", };

        for (String precondition : preconditions) {
            if (!map.containsKey(precondition)) {
                setErrorMessage(StringUtils.format(
                        "Invalid Google App Engine directory. Did not find: %s in %s",
                        precondition, locationFieldContents));

                return false;
            }
        }

        File libDir = new File(loc, "lib");
        if (!libDir.exists()) {
            setErrorMessage(StringUtils.format(
                    "Invalid Google App Engine directory. Did not find 'lib' dir at: %s",
                    libDir.getAbsolutePath()));
        }
        if (!libDir.isDirectory()) {
            setErrorMessage(StringUtils.format(
                    "Invalid Google App Engine directory. Expected 'lib' to be a directory at: %s",
                    libDir.getAbsolutePath()));
        }

        List<String> libFoldersForPythonpath = gatherLibFoldersForPythonpath(libDir, "/lib/");
        Collections.sort(libFoldersForPythonpath); //Show it sorted to the user!

        //We do this because we want to keep only one version of django_0_96 or django_1_21 selected by default (which
        //the user may later change).
        Map<String, String> mapStartToLib = new HashMap<String, String>();
        for (String s : libFoldersForPythonpath) {
            List<String> split = StringUtils.split(s, '_');
            if (split.size() > 0) {
                mapStartToLib.put(split.get(0), s);
            }
        }
        PythonSelectionLibrariesDialog runnable = new PythonSelectionLibrariesDialog(new ArrayList<String>(
                mapStartToLib.values()), libFoldersForPythonpath, false);
        runnable.setMsg("Please select the libraries you want in your PYTHONPATH.");

        List<String> selection;
        if (selectLibraries != null) {
            selection = selectLibraries.call(new ArrayList<String>(mapStartToLib.values()));
        } else {
            RunInUiThread.sync(runnable);
            boolean result = runnable.getOkResult();
            if (result == false) {
                //Canceled by the user
                return false;
            }
            selection = runnable.getSelection();
        }

        //If we got here, all is OK, let's go on and show the templateNamesAndDescriptions that'll be added to the PYTHONPATH (as external folders)
        variableSubstitution.put(AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE, loc.getAbsolutePath());

        String[] paths = new String[selection.size() + 1];
        paths[0] = "${" + AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE + "}";
        int i = 0;
        for (String s : selection) {
            i++;
            paths[i] = "${" + AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE + "}" + s;
        }
        fillExternalSourceFolders(variableSubstitution, paths);

        setErrorMessage(null);
        setMessage(null);
        return true;
    }

    public static ICallback<List<String>, List<String>> selectLibraries; //Only for test-cases.

    /**
     * Given the app engine location, returns the folders paths that should be added to the pythonpath considering
     * the app engine variable.
     * 
     * E.g.: /lib/webob, /lib/yaml/lib, so that they complete with 
     * "${"+AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE+"}"+"/lib/webob"
     */
    private List<String> gatherLibFoldersForPythonpath(File libDir, String currentPath) {
        ArrayList<String> ret = new ArrayList<String>();
        File[] listFiles = libDir.listFiles();
        if (listFiles != null) {
            for (File f : listFiles) {
                if (f.isDirectory()) {
                    if (checkDirHasFolderWithInitInside(f)) {
                        ret.add(currentPath + f.getName());
                    } else {
                        ret.addAll(gatherLibFoldersForPythonpath(f, currentPath + f.getName() + "/"));
                    }
                }
            }
        }
        return ret;
    }

    private boolean checkDirHasFolderWithInitInside(File f) {
        File[] listFiles = f.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                if (file.isDirectory()) {
                    if (new File(file, "__init__.py").exists() || new File(file, "__init__.pyc").exists()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * The tree/externalSourceFolders/varibleSubstitution  must be already empty at this point 
     */
    private void fillExternalSourceFolders(Map<String, String> variableSubstitution, String[] libFoldersForPythonpath) {
        TreeItem item = new TreeItem(tree, SWT.NONE);

        item.setText(AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE + ": "
                + variableSubstitution.get(AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE));
        item.setImage(imageAppEngine);

        for (String file : libFoldersForPythonpath) {
            TreeItem subItem = new TreeItem(item, SWT.NONE);
            subItem.setText(file);
            subItem.setImage(imageSystemLib);
            item.setExpanded(true);

            externalSourceFolders.add(file);
        }
    }

    public List<String> getExternalSourceFolders() {
        return externalSourceFolders;
    }

    public Map<String, String> getVariableSubstitution() {
        return variableSubstitution;
    }

}
