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
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

public class DjangoNature implements IProjectNature {

    public static final String DJANGO_NATURE_ID = "org.python.pydev.django.djangoNature";

	private IProject project;

	
	public void configure() throws CoreException {

	}

	
	public void deconfigure() throws CoreException {

	}

	
	public IProject getProject() {
		return project;
	}

	
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
        
        if(!project.hasNature(PythonNature.PYTHON_NATURE_ID)){
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

	
    /**
     * @param project the project we want to know about (if it is null, null is returned)
     * @return the django nature for a project (or null if it does not exist for the project)
     */
    public static synchronized DjangoNature getDjangoNature(IProject project) {
        if(project != null && project.isOpen()){
            try {
            	if(project.hasNature(DJANGO_NATURE_ID)){
	                IProjectNature n = project.getNature(DJANGO_NATURE_ID);
	                if(n instanceof DjangoNature){
	                    return (DjangoNature) n;
	                }
            	}
            } catch (CoreException e) {
                PydevPlugin.logInfo(e);
            }
        }
        return null;
    }

    public static synchronized void removeNature(IProject project, IProgressMonitor monitor) throws CoreException {
        if(monitor == null){
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
