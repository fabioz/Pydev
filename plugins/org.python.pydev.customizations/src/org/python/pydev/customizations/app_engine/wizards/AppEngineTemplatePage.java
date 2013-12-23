/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.app_engine.wizards;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.core.log.Log;
import org.python.pydev.customizations.CustomizationsPlugin;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This page is used to configure templates for google app engine.
 */
public class AppEngineTemplatePage extends WizardPage {

    /**
     * Constant for forcing the user to choose a template
     */
    protected static final String CHOOSE_ONE = "-- Choose One --";

    /**
     * Constant for creating an empty project
     */
    protected static final String EMPTY_PROJECT = "Empty Project";

    /**
     * The names of the templates and the related description to be shown to the user
     */
    protected Map<String, Tuple<String, File>> templateNamesAndDescriptions;

    /**
     * Combo-box with the template names for the user to choose. 
     */
    protected Combo comboTemplateNames;

    /**
     * A label to show the description for the selected template
     */
    protected Label templateDescription;

    /**
     * A string with the last choice the user has done in the combo
     */
    protected String lastTemplateChoice = "";

    /**
     * The UI for entering the text
     */
    private Text appIdText;

    /**
     * The lastAppId used by the user (the value here is the default)
     */
    private String lastAppIdText = "sample-app";

    protected AppEngineTemplatePage(String pageName) {
        super(pageName);
        setChooseOneErrorMessage();
    }

    private void setChooseOneErrorMessage() {
        setErrorMessage("Please select the template to use to create the project");
    }

    public void createControl(Composite parent) {
        Font font = parent.getFont();

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        setFillHorizontalLayoutDataAndFont(composite, font);

        //---- Application id
        Label appIdLabel = new Label(composite, SWT.NONE);
        appIdLabel.setText("What's the application id registered for this project?");
        setFillHorizontalLayoutDataAndFont(appIdLabel, font);

        appIdText = new Text(composite, SWT.BORDER);
        appIdText.setText(lastAppIdText);
        setFillHorizontalLayoutDataAndFont(appIdText, font);
        appIdText.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                handleChange();
            }
        });

        //---- Template label / combo
        Label templateLabel = new Label(composite, SWT.NONE);
        templateLabel.setText("From which template do you want to create your new Google App Project?");
        setFillHorizontalLayoutDataAndFont(templateLabel, font);

        comboTemplateNames = new Combo(composite, SWT.BORDER);
        templateNamesAndDescriptions = new HashMap<String, Tuple<String, File>>();

        try {
            loadTemplates();
        } catch (CoreException e1) {
            Log.log(e1);
        }

        ArrayList<String> keys = new ArrayList<String>(templateNamesAndDescriptions.keySet());
        Collections.sort(keys);

        keys.add(0, EMPTY_PROJECT);
        keys.add(0, CHOOSE_ONE);
        comboTemplateNames.setItems(keys.toArray(new String[0]));
        comboTemplateNames.setText(CHOOSE_ONE);
        setFillHorizontalLayoutDataAndFont(comboTemplateNames, font);

        comboTemplateNames.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                handleChange();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        templateDescription = new Label(composite, SWT.NONE);
        templateDescription.setText("");
        setFillHorizontalLayoutDataAndFont(templateDescription, font);
        setControl(composite);
    }

    /**
     * Sets the font and the grid data for some control.
     */
    private void setFillHorizontalLayoutDataAndFont(Control control, Font font) {
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        control.setLayoutData(data);
        control.setFont(font);
    }

    /**
     * Loads the templates from the filesystem.
     */
    private void loadTemplates() throws CoreException {
        File relativePath = CustomizationsPlugin.getBundleInfo().getRelativePath(
                new Path("templates/google_app_engine"));
        File[] files = relativePath.listFiles();
        if (files != null) {
            for (File dir : files) {
                if (dir.isDirectory()) {
                    File[] secondLevelFiles = dir.listFiles();
                    if (secondLevelFiles != null) {
                        for (File file2 : secondLevelFiles) {
                            if (file2.getName().equals("description.txt")) {
                                String fileContents = FileUtils.getFileContents(file2).trim();
                                Tuple<String, String> nameAndDesc = StringUtils.splitOnFirst(fileContents, ':');
                                templateNamesAndDescriptions.put(nameAndDesc.o1, new Tuple<String, File>(
                                        nameAndDesc.o2, dir));
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * When the selection changes, we update the last choice, description and the error message.
     */
    protected void handleChange() {
        lastTemplateChoice = this.comboTemplateNames.getText();
        lastAppIdText = this.appIdText.getText();

        Tuple<String, File> description = templateNamesAndDescriptions.get(lastTemplateChoice);
        templateDescription.setText(description != null ? description.o1 : "");

        boolean hasError = false;
        if (lastTemplateChoice.equals(CHOOSE_ONE)) {
            setChooseOneErrorMessage();
            hasError = true;
        } else if (lastAppIdText == null || lastAppIdText.trim().length() == 0) {
            setErrorMessage("Please fill the application id (registered in Google App Engine).");
            hasError = true;
        }

        if (!hasError) {
            setErrorMessage(null);
        }
    }

    /**
     * Called so that the initial structure is filled, given the source folder to fill.
     */
    public void fillSourceFolder(IContainer sourceFolder) {
        if (lastTemplateChoice == null || lastTemplateChoice.equals(CHOOSE_ONE)
                || lastTemplateChoice.equals(EMPTY_PROJECT)) {
            //Do nothing
        } else {
            Tuple<String, File> tuple = templateNamesAndDescriptions.get(lastTemplateChoice);
            if (tuple != null && tuple.o2.isDirectory()) {
                try {
                    //copy all but the description.txt file.
                    FileUtils.copyDirectory(tuple.o2, sourceFolder.getLocation().toFile(),
                            new ICallback<Boolean, File>() {

                                public Boolean call(File arg) {
                                    //we don't want to copy description.txt
                                    String filename = arg.getName().toLowerCase();
                                    if (filename.equals("description.txt") || filename.equals(".svn")
                                            || filename.equals("cvs")) {
                                        return true;
                                    }
                                    return false;
                                }
                            }, new ICallback<String, String>() {

                                public String call(String contents) {
                                    //We want to change any references to ${app_id} for the app id entered by the user
                                    return StringUtils.replaceAll(contents,
                                            "${app_id}", lastAppIdText);
                                }
                            });
                } catch (IOException e) {
                    Log.log(e);
                }
                try {
                    sourceFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
                } catch (CoreException e) {
                    Log.log(e);
                }
            }
        }
    }

}
