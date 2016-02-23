/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.app_engine.wizards;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.WizardPage;
import org.python.pydev.plugin.PyStructureConfigHelpers;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.ui.wizards.project.IWizardNewProjectNameAndLocationPage;
import org.python.pydev.ui.wizards.project.NewProjectNameAndLocationWizardPage;
import org.python.pydev.ui.wizards.project.PythonProjectWizard;

/**
 * Wizard that helps in the creation of a Pydev project configured for Google App Engine. 
 * 
 * @author Fabio
 */
public class AppEngineWizard extends PythonProjectWizard {

    private AppEngineConfigWizardPage appEngineConfigWizardPage;
    private AppEngineTemplatePage appEngineTemplatePage;

    /**
     * Add wizard pages to the instance
     * 
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    @Override
    public void addPages() {
        addPage(projectPage);
        addPage(sourcesPage);

        appEngineConfigWizardPage = new AppEngineConfigWizardPage("Google App Engine Page");
        appEngineConfigWizardPage.setTitle("Google App Engine");
        appEngineConfigWizardPage.setDescription("Set Google App Engine Configuration");
        addPage(appEngineConfigWizardPage);

        appEngineTemplatePage = new AppEngineTemplatePage("Initial Structure");
        addPage(appEngineTemplatePage);
    }

    /**
     * Creates the project page.
     */
    @Override
    protected IWizardNewProjectNameAndLocationPage createProjectPage() {
        return new NewProjectNameAndLocationWizardPage("Setting project properties");
    }

    @Override
    protected WizardPage getPageAfterSourcesPage() {
        return appEngineConfigWizardPage;
    }

    /**
     * Overridden to add the external source folders from google app engine.
     */
    @Override
    protected void createAndConfigProject(final IProject newProjectHandle, final IProjectDescription description,
            final String projectType, final String projectInterpreter, IProgressMonitor monitor,
            Object... additionalArgsToConfigProject) throws CoreException {
        ICallback<List<IContainer>, IProject> getSourceFolderHandlesCallback = super.getSourceFolderHandlesCallback;
        ICallback<List<IPath>, IProject> getExistingSourceFolderHandlesCallback = super.getExistingSourceFolderHandlesCallback;

        ICallback<List<String>, IProject> getExternalSourceFolderHandlesCallback = new ICallback<List<String>, IProject>() {

            @Override
            public List<String> call(IProject projectHandle) {
                return appEngineConfigWizardPage.getExternalSourceFolders();
            }
        };

        ICallback<Map<String, String>, IProject> getVariableSubstitutionCallback = new ICallback<Map<String, String>, IProject>() {

            @Override
            public Map<String, String> call(IProject projectHandle) {
                return appEngineConfigWizardPage.getVariableSubstitution();
            }
        };

        PyStructureConfigHelpers.createPydevProject(description, newProjectHandle, monitor, projectType,
                projectInterpreter, getSourceFolderHandlesCallback, getExternalSourceFolderHandlesCallback,
                getExistingSourceFolderHandlesCallback, getVariableSubstitutionCallback);

        //Ok, after the default is created, let's see if we have a template...
        IContainer sourceFolder;

        final int sourceFolderConfigurationStyle = projectPage.getSourceFolderConfigurationStyle();
        switch (sourceFolderConfigurationStyle) {

            case IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PROJECT_AS_SRC_FOLDER:
            case IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_NO_PYTHONPATH:
                sourceFolder = newProjectHandle;
                break;

            default:
                sourceFolder = newProjectHandle.getFolder("src");
        }

        appEngineTemplatePage.fillSourceFolder(sourceFolder);
    }

}
