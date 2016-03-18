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
        setPageComplete(false);
    }

    /* (non-Javadoc)
     * Method declared on IDialogPage.
     */
    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setFont(parent.getFont());

        group = new PythonExistingSourceListGroup(composite, new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (validatePage()) {
                    setPageComplete(true);
                    setErrorMessage(group.getErrorMessage());
                    setMessage(group.getWarningMessage(), WARNING);
                }
                else {
                    setPageComplete(false);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        setPageComplete(validatePage());
        setControl(composite);
    }

    @Override
    public boolean isPageComplete() {
        //If external sources are not selected, just validate the page. If they aren't, this page gets ignored anyways.
        if (((PythonProjectWizard) getWizard()).projectPage.getSourceFolderConfigurationStyle() != IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_EXISTING_SOURCES) {
            return true;
        }
        return super.isPageComplete();
    }

    /**
     * Returns whether this page's controls currently all contain valid
     * values.
     *
     * @return <code>true</code> if all controls are valid, and
     *   <code>false</code> if at least one is invalid
     */
    protected boolean validatePage() {
        if (group.getLinkTargets().size() == 0) {
            setErrorMessage("No existing sources selected.");
            return false;
        }

        setErrorMessage(null);
        setMessage(null);
        return true;
    }

    @Override
    public List<IPath> getExistingSourceFolders() {
        return group.getLinkTargets();
    }

}
