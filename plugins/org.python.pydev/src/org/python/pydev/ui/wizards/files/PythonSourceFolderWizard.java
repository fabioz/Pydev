/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 22, 2006
 */
package org.python.pydev.ui.wizards.files;

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PyStructureConfigHelpers;
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

            @Override
            protected String checkNameText(String text) {
                String result = super.checkNameText(text);
                if (result != null) {
                    return result;
                }
                if (getValidatedProject().findMember(text) != null) {
                    return "The source folder " + text +
                            " already exists in project " + getValidatedProject().getName() + ".";
                }
                return null;
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
        if (folder.exists()) {
            Log.log("Source folder already exists. Nothing new was created");
            return null;
        }
        folder.create(true, true, monitor);
        String newPath = folder.getFullPath().toString();

        String curr = pathNature.getProjectSourcePath(false);
        if (curr == null) {
            curr = "";
        }
        if (curr.endsWith("|")) {
            curr = curr.substring(0, curr.length() - 1);
        }
        String newPathRel = PyStructureConfigHelpers.convertToProjectRelativePath(
                project.getFullPath().toString(), newPath);
        if (curr.length() > 0) {
            //there is already some path
            Set<String> projectSourcePathSet = pathNature.getProjectSourcePathSet(true);
            if (!projectSourcePathSet.contains(newPath)) {
                //only add to the path if it doesn't already contain the new path
                curr += "|" + newPathRel;
            }
        } else {
            //there is still no other path
            curr = newPathRel;
        }
        pathNature.setProjectSourcePath(curr);
        PythonNature.getPythonNature(project).rebuildPath();
        return null;
    }

}
