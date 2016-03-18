/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.nature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;


public class DjangoNature implements IProjectNature {

    public static final String DJANGO_NATURE_ID = PythonNature.DJANGO_NATURE_ID;

    private IProject project;

    @Override
    public void configure() throws CoreException {

    }

    @Override
    public void deconfigure() throws CoreException {

    }

    @Override
    public IProject getProject() {
        return project;
    }

    @Override
    public void setProject(IProject project) {
        this.project = project;
    }

    public static synchronized void addNature(IProject project, IProgressMonitor monitor) throws CoreException {

        if (project == null || !project.isOpen()) {
            return;
        }

        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        IProjectDescription desc = project.getDescription();

        if (!project.hasNature(PythonNature.PYTHON_NATURE_ID)) {
            //also add the python nature if it still wasn't added.
            PythonNature.addNature(project, null, null, null, null, null, null);
        }

        //only add the django nature if it still hasn't been added.
        if (!project.hasNature(DJANGO_NATURE_ID)) {

            String[] natures = desc.getNatureIds();
            String[] newNatures = new String[natures.length + 1];
            System.arraycopy(natures, 0, newNatures, 0, natures.length);
            newNatures[natures.length] = DJANGO_NATURE_ID;
            desc.setNatureIds(newNatures);
            project.setDescription(desc, monitor);
        }
    }

    private static final Object lockGetNature = new Object();

    /**
     * @param project the project we want to know about (if it is null, null is returned)
     * @return the django nature for a project (or null if it does not exist for the project)
     * 
     * @note: it's synchronized because more than 1 place could call getDjangoNature at the same time and more
     * than one nature ended up being created from project.getNature().
     */
    public static DjangoNature getDjangoNature(IProject project) {
        if (project != null && project.isOpen()) {
            try {
                if (project.hasNature(DJANGO_NATURE_ID)) {
                    synchronized (lockGetNature) {
                        IProjectNature n = project.getNature(DJANGO_NATURE_ID);
                        if (n instanceof DjangoNature) {
                            return (DjangoNature) n;
                        }
                    }
                }
            } catch (CoreException e) {
                Log.logInfo(e);
            }
        }
        return null;
    }

    public static synchronized void removeNature(IProject project, IProgressMonitor monitor) throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        DjangoNature nature = DjangoNature.getDjangoNature(project);
        if (nature == null) {
            return;
        }

        //and finally... remove the nature
        IProjectDescription description = project.getDescription();
        List<String> natures = new ArrayList<String>(Arrays.asList(description.getNatureIds()));
        natures.remove(DJANGO_NATURE_ID);
        description.setNatureIds(natures.toArray(new String[natures.size()]));
        project.setDescription(description, monitor);
    }

}
