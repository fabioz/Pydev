/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.wizards.gettingstarted;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.dialogs.WizardNewProjectReferencePage;

public abstract class AbstractNewProjectWizard extends Wizard implements INewWizard {

    protected WizardNewProjectReferencePage referencePage;

    /**
     * Adds the project references page to the wizard.
     */
    protected void addProjectReferencePage() {
        // only add page if there are already projects in the workspace
        if (ResourcesPlugin.getWorkspace().getRoot().getProjects().length > 0) {
            referencePage = new WizardNewProjectReferencePage("Reference Page");
            referencePage.setTitle("Reference page");
            referencePage.setDescription("Select referenced projects");
            this.addPage(referencePage);
        }
    }

}
