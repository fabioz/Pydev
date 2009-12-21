package com.leosoto.bingo.plugin.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public class DjangoNature implements IProjectNature {

    public static final String DJANGO_NATURE_ID = "com.leosoto.bingo.djangoNature";

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

		if(monitor == null){
            monitor = new NullProgressMonitor();
        }
        IProjectDescription desc = project.getDescription();

        //only add the nature if it still hasn't been added.
        if (!project.hasNature(DJANGO_NATURE_ID)) {

            String[] natures = desc.getNatureIds();
            String[] newNatures = new String[natures.length + 1];
            System.arraycopy(natures, 0, newNatures, 0, natures.length);
            newNatures[natures.length] = DJANGO_NATURE_ID;
            desc.setNatureIds(newNatures);
            project.setDescription(desc, monitor);
        }
	}

}
