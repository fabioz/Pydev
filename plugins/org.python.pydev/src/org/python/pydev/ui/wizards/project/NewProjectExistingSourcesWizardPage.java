/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package org.python.pydev.ui.wizards.project;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.ui.wizards.files.PythonExistingSourceListGroup;
import org.python.pydev.ui.wizards.gettingstarted.AbstractNewProjectPage;

/**
 * 
 */
public class NewProjectExistingSourcesWizardPage extends AbstractNewProjectPage
        implements IWizardNewProjectExistingSourcesPage {

    private PythonExistingSourceListGroup group;

    /**
     * Creates a wizard page that can be used in a Java project creation wizard.
     * It contains UI to configure a the classpath and the output folder.
     *
     * <p>
     * After constructing, a call to {@link #init(IJavaProject, IPath, IClasspathEntry[], boolean)} is required.
     * </p>
     */
    public NewProjectExistingSourcesWizardPage(String pageName) {
        super(pageName);
        setTitle("Add Existing Sources");
        setDescription("Add links to existing source folders from external locations.");
        setPageComplete(true);
    }

    /* (non-Javadoc)
     * Method declared on IDialogPage.
     */
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setFont(parent.getFont());

        group = new PythonExistingSourceListGroup(composite, new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                setErrorMessage(group.getErrorMessage());
                setMessage(group.getWarningMessage(), WARNING);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        setErrorMessage(null);
        setMessage(null);
        setControl(composite);
    }

    public List<IPath> getExistingSourceFolders() {
        return group.getLinkTargets();
    }

}
