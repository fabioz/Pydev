/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.pycremover.PycRemoverBuilderVisitor;
import org.python.pydev.builder.pylint.PyLintVisitor;
import org.python.pydev.builder.todo.PyTodoVisitor;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.PyCodeCompletionVisitor;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * This builder only passes through python files
 * 
 * @author Fabio Zadrozny
 */
public class PyDevBuilder extends IncrementalProjectBuilder {

    /**
     * 
     * @return a list of visitors for building the application.
     */
    @SuppressWarnings("unchecked")
    public List<PyDevBuilderVisitor> getVisitors() {
        List<PyDevBuilderVisitor> list = new ArrayList<PyDevBuilderVisitor>();
        list.add(new PyTodoVisitor());
        list.add(new PyLintVisitor());
        list.add(new PyCodeCompletionVisitor());
        list.add(new PycRemoverBuilderVisitor());

        list.addAll(ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_BUILDER));
        return list;
    }




    /**
     * Builds the project.
     * 
     * @see org.eclipse.core.internal.events InternalBuilder#build(int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
     */
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {

        if (PyDevBuilderPrefPage.usePydevBuilders() == false)
            return null;

        if (kind == IncrementalProjectBuilder.FULL_BUILD) {
            // Do a Full Build: Use a ResourceVisitor to process the tree.
            performFullBuild(monitor);

        } else {
            // Build it with a delta
            IResourceDelta delta = getDelta(getProject());

            if (delta == null) {
                //no delta (unspecified changes?... let's do a full build...)
                performFullBuild(monitor);
                
            } else {
                // ok, we have a delta
                // first step is just counting them
                PyDevDeltaCounter counterVisitor = new PyDevDeltaCounter();
                delta.accept(counterVisitor);
                
                List<PyDevBuilderVisitor> visitors = getVisitors();
                
                //sort by priority
                Collections.sort(visitors); 

                PydevGrouperVisitor grouperVisitor = new PydevGrouperVisitor(visitors, monitor, counterVisitor.getNVisited());
                notifyVisitingWillStart(visitors, monitor);
                delta.accept(grouperVisitor);
                notifyVisitingEnded(visitors, monitor);
                
            }
        }
        return null;
    }

    /**
     * Processes all python files.
     * 
     * @param monitor
     */
    private void performFullBuild(IProgressMonitor monitor) throws CoreException {
        IProject project = getProject();

        //we need the project...
        if (project != null) {
            PythonNature nature = PythonNature.getPythonNature(project);
            IPythonPathNature pythonPathNature = nature.getPythonPathNature();
            pythonPathNature.getProjectSourcePath(); //this is just to update the paths (in case the project name has just changed)
            
            //and the nature...
            if (nature != null){
                List<IResource> resourcesToParse = new ArrayList<IResource>();
    
                List<PyDevBuilderVisitor> visitors = getVisitors();
                notifyVisitingWillStart(visitors, monitor);
    
                monitor.beginTask("Building...", (visitors.size() * 100) + 30);
    
                IResource[] members = project.members();
    
                if (members != null) {
                    // get all the python files to get information.
                    for (int i = 0; i < members.length; i++) {
                        try {
                            IResource member = members[i];
                            if (member == null) {
                                continue;
                            }
    
                            if (member.getType() == IResource.FILE) {
                                addToResourcesToParse(resourcesToParse, member, nature);
                                
                            } else if (member.getType() == IResource.FOLDER) {
                                //if it is a folder, let's get all python files that are beneath it
                                //the heuristics to know if we have to analyze them are the same we have
                                //for a single file
                                IPath location = ((IFolder) member).getLocation();
                                File folder = new File(location.toOSString());
                                List l = PydevPlugin.getPyFilesBelow(folder, null, true, false)[0];
                                
                                for (Iterator iter = l.iterator(); iter.hasNext();) {
                                    File element = (File) iter.next();
                                    IPath path = PydevPlugin.getPath(new Path(REF.getFileAbsolutePath(element)));
                                    IResource resource = project.findMember(path);
                                    if (resource != null) {
                                        addToResourcesToParse(resourcesToParse, resource, nature);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // that's ok...
                        }
                    }
                    monitor.worked(30);
                    buildResources(resourcesToParse, monitor, visitors);
                }
                notifyVisitingEnded(visitors, monitor);
            }
        }
        monitor.done();

    }

    private void notifyVisitingWillStart(List<PyDevBuilderVisitor> visitors, IProgressMonitor monitor) {
        for (PyDevBuilderVisitor visitor : visitors) {
            visitor.visitingWillStart(monitor);
        }
    }

    private void notifyVisitingEnded(List<PyDevBuilderVisitor> visitors, IProgressMonitor monitor) {
        for (PyDevBuilderVisitor visitor : visitors) {
            visitor.visitingEnded(monitor);
        }
    }
    



    /**
     * @param resourcesToParse the list where the resource may be added
     * @param member the resource we are adding
     * @param nature the nature associated to the resource
     */
    private void addToResourcesToParse(List<IResource> resourcesToParse, IResource member, PythonNature nature) {
        //analyze it only if it is a valid source file 
        String fileExtension = member.getFileExtension();
        if (fileExtension != null && PythonPathHelper.isValidSourceFile("."+fileExtension)) {
            resourcesToParse.add(member);
        }
    }


    /**
     * Default implementation. Visits each resource once at a time. May be overriden if a better implementation is needed.
     * 
     * @param resourcesToParse list of resources from project that are python files.
     * @param monitor
     * @param visitors
     */
    public void buildResources(List resourcesToParse, IProgressMonitor monitor, List visitors) {

        // we have 100 units here
        double inc = (visitors.size() * 100) / (double) resourcesToParse.size();

        double total = 0;
        int totalResources = resourcesToParse.size();
        int i = 0;

        for (Iterator iter = resourcesToParse.iterator(); iter.hasNext() && monitor.isCanceled() == false;) {
            i += 1;
            total += inc;
            IResource r = (IResource) iter.next();
            if(!PythonNature.isResourceInPythonpath(r)){
            	continue; // we only analyze resources that are in the pythonpath
            }
            String moduleNameForResource = PythonNature.getModuleNameForResource(r);
            if(moduleNameForResource == null){
            	PythonNature.getPythonNature(r.getProject());
            }
            IDocument doc = REF.getDocFromResource(r);
            
            HashMap<String, Object> memo = new HashMap<String, Object>();
            memo.put(PyDevBuilderVisitor.IS_FULL_BUILD, true); //mark it as full build
            
            if(doc != null){ //might be out of synch
                for (Iterator it = visitors.iterator(); it.hasNext() && monitor.isCanceled() == false;) {

                    PyDevBuilderVisitor visitor = (PyDevBuilderVisitor) it.next();
                    visitor.memo = memo; //setting the memo must be the first thing.
    
                    communicateProgress(monitor, totalResources, i, r, visitor);
                    
                    //on a full build, all visits are as some add...
                    visitor.visitAddedResource(r, doc, monitor);
                }
    
                if (total > 1) {
                    monitor.worked((int) total);
                    total -= (int) total;
                }
            }
        }
    }

    /**
     * Used so that we can communicate the progress to the user
     */
    public static void communicateProgress(IProgressMonitor monitor, int totalResources, int i, IResource r, PyDevBuilderVisitor visitor) {
        if(monitor != null){
            StringBuffer msgBuf = new StringBuffer();
            msgBuf.append("Visiting... (");
            msgBuf.append(i);
            msgBuf.append(" of ");
            msgBuf.append(totalResources);
            msgBuf.append(") - ");
            msgBuf.append(r.getProjectRelativePath());
            msgBuf.append(" - visitor: ");
            msgBuf.append(visitor.getClass().getName());
       
            //in this case the visitor does not have the progress and therefore does not communicate the progress
            String name = msgBuf.toString();
            monitor.subTask(name);
        }
    }

}
