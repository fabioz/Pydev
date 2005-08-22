/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.pylint.PyLintVisitor;
import org.python.pydev.builder.todo.PyTodoVisitor;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.PyCodeCompletionVisitor;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.extension.ExtensionHelper;
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
    public List<PyDevBuilderVisitor> getVisitors() {
        List<PyDevBuilderVisitor> list = new ArrayList<PyDevBuilderVisitor>();
        list.add(new PyTodoVisitor());
        list.add(new PyLintVisitor());
        list.add(new PyCodeCompletionVisitor());

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

            // first step is just counting them
            IResourceDelta delta = getDelta(getProject());
            PyDevDeltaCounter counterVisitor = new PyDevDeltaCounter();
            delta.accept(counterVisitor);

            if (delta == null) {
                performFullBuild(monitor);
            } else {
                for (PyDevBuilderVisitor element : getVisitors()) {

                    element.monitor = monitor;
                    element.totalResources = counterVisitor.getNVisited();
                    // some visitors cannot visit too many elements because they do a lot of processing
                    if (element.maxResourcesToVisit() == PyDevBuilderVisitor.MAX_TO_VISIT_INFINITE || element.maxResourcesToVisit() >= counterVisitor.getNVisited()) {
                        delta.accept(element);
                    }
                }
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
            
            //and the nature...
            if (nature != null){
                List<IResource> resourcesToParse = new ArrayList<IResource>();
    
                List<PyDevBuilderVisitor> visitors = getVisitors();
    
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
            }
        }
        monitor.done();

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

            IDocument doc = getDocFromResource(r);
            if(doc != null){ //might be out of synch
                for (Iterator it = visitors.iterator(); it.hasNext() && monitor.isCanceled() == false;) {
                    PyDevBuilderVisitor visitor = (PyDevBuilderVisitor) it.next();
    
                    communicateProgress(monitor, totalResources, i, r, visitor);
                    visitor.visitChangedResource(r, doc);
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

    /**
     * Returns a document, created with the contents of a resource.
     * 
     * @param resource
     * @return
     */
    public static IDocument getDocFromResource(IResource resource) {
        IProject project = resource.getProject();
        if (project != null && resource instanceof IFile) {

            IFile file = (IFile) resource;
            try {
                String encoding = file.getCharset();
                InputStream stream = file.getContents();
                StringBuffer buf = new StringBuffer();

                InputStreamReader in = null;
                try {
                    if (encoding != null) {
                        in = new InputStreamReader(stream, encoding);
                    } else {
                        in = new InputStreamReader(stream);
                    }

                    int c;
                    while ((c = in.read()) != -1) {
                        buf.append((char) c);
                    }
                } finally {
                    in.close();
                }
                return new Document(buf.toString());
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
        return null;
    }

}
