/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 22, 2006
 */
package org.python.pydev.ui.wizards.files;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.plugin.nature.PythonNature;

public class PythonSourceFolderWizard extends AbstractPythonWizard {

    public PythonSourceFolderWizard() {
        super("Create a new Source Folder");
    }

    public static final String WIZARD_ID = "org.python.pydev.ui.wizards.files.PythonSourceFolderWizard";

    @Override
    protected AbstractPythonWizardPage createPathPage() {
        return new AbstractPythonWizardPage(this.description, selection) {

            @Override
            protected boolean shouldCreateSourceFolderSelect() {
                return false;
            }

            @Override
            protected boolean shouldCreatePackageSelect() {
                return false;
            }

        };
    }

    @Override
    protected IFile doCreateNew(IProgressMonitor monitor) throws CoreException {
        IProject project = filePage.getValidatedProject();
        String name = filePage.getValidatedName();
        if (project == null || !project.exists()) {
            throw new RuntimeException("The project selected does not exist in the workspace.");
        }
        IPythonPathNature pathNature = PythonNature.getPythonPathNature(project);
        if (pathNature == null) {
            IPythonNature nature = PythonNature.addNature(project, monitor, null, null, null, null, null);
            pathNature = nature.getPythonPathNature();
            if (pathNature == null) {
                throw new RuntimeException("Unable to add the nature to the seleted project.");
            }
        }
        IFolder folder = project.getFolder(name);
        if (!folder.exists()) {
            folder.create(true, true, monitor);
        }
        String newPath = folder.getFullPath().toString();

        String curr = pathNature.getProjectSourcePath(true);
        if (curr == null) {
            curr = "";
        }
        if (curr.endsWith("|")) {
            curr = curr.substring(0, curr.length() - 1);
        }
        if (curr.length() > 0) {
            //there is already some path
            curr += "|" + newPath;
        } else {
            //there is still no other path
            curr = newPath;
        }
        pathNature.setProjectSourcePath(curr);
        PythonNature.getPythonNature(project).rebuildPath();
        return null;
    }

}
