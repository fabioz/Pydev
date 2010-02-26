/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *    Sebastian Davids <sdavids@gmx.de> - Fix for bug 19346 - Dialog font should be
 *        activated and used by other components.
 *******************************************************************************/
package com.leosoto.bingo.ui.wizards.project;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.ui.wizards.project.NewProjectNameAndLocationWizardPage;

public class DjangoNewProjectPage
extends NewProjectNameAndLocationWizardPage
{
    /**
     * Creates a new project creation wizard page.
     *
     * @param pageName the name of this page
     */
    public DjangoNewProjectPage(String pageName) {
        super(pageName);
        setTitle("Django Project");
        setDescription("Create a new Django Project.");
    }

    /* (non-Javadoc)
     * Method declared on IDialogPage.
     */
    public void createControl(Composite parent) {
    	super.createControl(parent);
    	checkSrcFolder.setVisible(false);
    }
}
