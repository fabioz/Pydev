/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.wizards.gettingstarted;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * This is a getting started wizard for Pydev:
 * 
 * It'll guide the user to configure the initial interpreter and create an initial project.
 */
public class PythonGettingStartedWizard extends AbstractNewProjectWizard implements IExecutableExtension {

    private IConfigurationElement fConfigElement;

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
    }

    @Override
    public void addPages() {
        addGettingStartedPage();
        addProjectReferencePage();
    }

    protected GettingStartedPage gettingStartedPage;

    /**
     * Adds the general info page to the wizard.
     */
    protected void addGettingStartedPage() {
        // only add page if there are already projects in the workspace
        if (ResourcesPlugin.getWorkspace().getRoot().getProjects().length > 0) {
            gettingStartedPage = new GettingStartedPage("Getting Started");
            gettingStartedPage.setTitle("Getting Started");
            gettingStartedPage.setDescription("Basic Getting Started on Configuring Pydev");
            this.addPage(gettingStartedPage);
        }
    }

    @Override
    public boolean performFinish() {

        // Switch to default 'Pydev' perspective (asks before changing)
        BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
        return true;
    }

    @Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
            throws CoreException {
        this.fConfigElement = config;
    }

}
