/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.ui.wizards.project;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.wizards.project.NewProjectNameAndLocationWizardPage;

public class DjangoNewProjectPage extends NewProjectNameAndLocationWizardPage {
    /**
     * Creates a new project creation wizard page.
     * 
     * @param pageName
     *            the name of this page
     */
    public DjangoNewProjectPage(String pageName) {
        super(pageName);
        setTitle("PyDev Django Project");
        setDescription("Create a new Pydev Django Project.");
    }

    /*
     * (non-Javadoc) Method declared on IDialogPage.
     */
    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
    }

    @Override
    public IWizardPage getNextPage() {
        String projectType = this.getProjectType();

        IInterpreterManager interpreterManager;
        if (IPythonNature.Versions.ALL_JYTHON_VERSIONS.contains(projectType)) {
            interpreterManager = PydevPlugin.getJythonInterpreterManager();

        } else if (IPythonNature.Versions.ALL_IRONPYTHON_VERSIONS.contains(projectType)) {
            interpreterManager = PydevPlugin.getIronpythonInterpreterManager();

        } else {
            //if others fail, consider it python
            interpreterManager = PydevPlugin.getPythonInterpreterManager();
        }

        try {
            String projectInterpreter = this.getProjectInterpreter();
            IInterpreterInfo interpreterInfo;
            if (projectInterpreter.toLowerCase().equals("default")) {
                interpreterInfo = interpreterManager.getDefaultInterpreterInfo(false);
            } else {
                interpreterInfo = interpreterManager.getInterpreterInfo(projectInterpreter, new NullProgressMonitor());
            }
            IModule module = interpreterInfo.getModulesManager().getModuleWithoutBuiltins("django.core.__init__", null,
                    false);
            if (module == null) {
                DjangoNotAvailableWizardPage page = new DjangoNotAvailableWizardPage("Django not available",
                        interpreterInfo);
                page.setWizard(this.getWizard());
                return page;
            }
        } catch (MisconfigurationException e) {
            ErrorWizardPage page = new ErrorWizardPage("Unexpected error.", "An unexpected error happened:\n"
                    + e.getMessage());
            page.setWizard(this.getWizard());
            return page;
        }

        return super.getNextPage();
    }

    @Override
    protected boolean validatePage() {
        boolean validated = super.validatePage();
        if (!validated) {
            return false; //some error found in the base class
        }

        String projectName = getProjectName();
        if (projectName.trim().toLowerCase().equals("django")) { //$NON-NLS-1$
            setErrorMessage("When creating a Django project it cannot be named Django because of conflicts with the default Django install.");
            return false;
        }
        return true;
    }

}
