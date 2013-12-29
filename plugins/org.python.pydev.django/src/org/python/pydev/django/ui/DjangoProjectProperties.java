/******************************************************************************
* Copyright (C) 2011-2013  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>       - initial API and implementation
*     Andrew Ferrazzutti <aferrazz@redhat.com> - ongoing maintenance
******************************************************************************/
package org.python.pydev.django.ui;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.django.launching.DjangoConstants;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.StringUtils;

public class DjangoProjectProperties extends PropertyPage {

    /**
     * This is the project we are editing
     */
    private IProject project;
    private Text textDjangoSettings;
    private Text textDjangoManage;
    private Label labelErrorSettings;
    private Label labelErrorManage;

    public DjangoProjectProperties() {
    }

    @Override
    protected Control createContents(Composite parent) {
        project = (IProject) getElement().getAdapter(IProject.class);

        Composite topComp = new Composite(parent, SWT.NONE);

        GridLayout innerLayout = new GridLayout();
        innerLayout.numColumns = 2;
        innerLayout.marginHeight = 0;
        innerLayout.marginWidth = 0;
        topComp.setLayout(innerLayout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        topComp.setLayoutData(gd);

        if (project != null) {
            try {
                IPythonPathNature pathNature = PythonNature.getPythonPathNature(project);
                final PythonNature nature = PythonNature.getPythonNature(project);

                Map<String, String> variableSubstitution = pathNature.getVariableSubstitution(false);

                Label label = new Label(topComp, SWT.None);
                label.setText("Django manage.py");

                Text text = new Text(topComp, SWT.BORDER);
                textDjangoManage = text;
                textDjangoManage
                        .setToolTipText("This is the name of the project-relative location of manage.py (i.e.: src/myapp/manage.py)");

                label = new Label(topComp, SWT.None);
                labelErrorManage = new Label(topComp, SWT.None);
                labelErrorManage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

                ModifyListener manageValidator = new ModifyListener() {

                    public void modifyText(ModifyEvent e) {
                        try {
                            String path = textDjangoManage.getText().trim();
                            if (path.trim().length() == 0) {
                                labelErrorSettings
                                        .setText("Please specify the manage.py relative name (i.e.: src/myapp/manage.py)");
                                return;
                            }

                            IFile file = project.getFile(new Path(path));
                            if (!file.exists()) {
                                labelErrorManage.setText(StringUtils.format("File: %s could not be found.", path));
                            } else {
                                labelErrorManage.setText("");
                            }
                        } catch (Exception e1) {
                            Log.log(e1);
                        }
                    }
                };
                text.addModifyListener(manageValidator);

                text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
                String string = variableSubstitution.get(DjangoConstants.DJANGO_MANAGE_VARIABLE);
                if (string != null) {
                    text.setText(string);
                } else {
                    text.setText("");
                }

                // Settings
                label = new Label(topComp, SWT.None);
                label.setText("Django settings module");
                text = new Text(topComp, SWT.BORDER);
                textDjangoSettings = text;
                textDjangoSettings
                        .setToolTipText("This is the name of the django settings module (i.e.: myapp.settings)");

                label = new Label(topComp, SWT.None);
                labelErrorSettings = new Label(topComp, SWT.None);
                labelErrorSettings.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
                ModifyListener settingsValidator = new ModifyListener() {

                    public void modifyText(ModifyEvent e) {
                        try {
                            String moduleName = textDjangoSettings.getText().trim();
                            if (moduleName.trim().length() == 0) {
                                labelErrorSettings
                                        .setText("Please specify the name of the module (i.e.: myapp.settings)");
                                return;
                            }

                            ICodeCompletionASTManager astManager = nature.getAstManager();
                            ProjectModulesManager modulesManager = (ProjectModulesManager) astManager
                                    .getModulesManager();
                            IModule moduleInDirectManager = modulesManager.getModuleInDirectManager(moduleName, nature,
                                    true);
                            if (moduleInDirectManager == null) {
                                labelErrorSettings.setText(StringUtils.format("Module: %s could not be found.",
                                        moduleName));
                            } else {
                                labelErrorSettings.setText("");
                            }
                        } catch (Exception e1) {
                            Log.log(e1);
                        }
                    }
                };
                text.addModifyListener(settingsValidator);

                text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
                string = variableSubstitution.get(DjangoConstants.DJANGO_SETTINGS_MODULE);
                if (string != null) {
                    text.setText(string);
                } else {
                    text.setText("");
                }

            } catch (Exception e) {
                Log.log(e);
            }

        } else {
            Label label = new Label(topComp, SWT.None);
            label.setText("Internal error: project not set!");
        }
        return topComp;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (textDjangoManage != null) {
            textDjangoManage.dispose();
            textDjangoManage = null;
        }
        if (textDjangoSettings != null) {
            textDjangoSettings.dispose();
            textDjangoSettings = null;
        }
    }

    /**
     * Saves values.
     */
    @Override
    public boolean performOk() {

        try {
            IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);
            Map<String, String> variableSubstitution = pythonPathNature.getVariableSubstitution(false);

            boolean changed = update(DjangoConstants.DJANGO_MANAGE_VARIABLE, variableSubstitution,
                    textDjangoManage.getText(), pythonPathNature);

            changed = update(DjangoConstants.DJANGO_SETTINGS_MODULE, variableSubstitution,
                    textDjangoSettings.getText(), pythonPathNature) || changed;

            if (changed) {
                pythonPathNature.setVariableSubstitution(variableSubstitution);
                PythonNature pythonNature = PythonNature.getPythonNature(project);

                if (pythonNature != null && (changed || pythonNature.getAstManager() == null)) {
                    pythonNature.rebuildPath();
                }
            }

        } catch (Exception e) {
            Log.log(e);
        }
        return true;
    }

    private boolean update(String varName, Map<String, String> currVariableSubstitution, String text,
            IPythonPathNature pythonPathNature) {
        boolean changed = false;
        String currVal = currVariableSubstitution.get(varName);
        String trimmed = text.trim();

        if (currVal == null) {
            changed = trimmed.length() != 0;
        } else {
            changed = !currVal.equals(trimmed);
        }

        if (changed) {
            if (trimmed.length() == 0) {
                currVariableSubstitution.remove(varName);
            } else {
                currVariableSubstitution.put(varName, trimmed);
            }
        }
        return changed;
    }

}
