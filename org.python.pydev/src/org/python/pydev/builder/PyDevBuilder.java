/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import java.io.File;
import java.util.ArrayList;
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
import org.python.pydev.builder.pychecker.PyCheckerVisitor;
import org.python.pydev.builder.pylint.PyLintVisitor;
import org.python.pydev.builder.todo.PyTodoVisitor;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public class PyDevBuilder extends IncrementalProjectBuilder {
    /**
     * 
     * @see IncrementalProjectBuilder#build
     */
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {

        if (kind == IncrementalProjectBuilder.FULL_BUILD) {
            // Do a Full Build: Use a ResourceVisitor to process the tree.
            performFullBuild(monitor);
        } else { // Build it with a delta
            IResourceDelta delta = getDelta(getProject());
            if (delta == null) {
                performFullBuild(monitor);
            } else {
                for (Iterator it = getVisitors().iterator(); it.hasNext();) {
                    PyDevBuilderVisitor element = (PyDevBuilderVisitor) it.next();
                    delta.accept(element);
                }
            }
        }
        return null;
    }

    private List getVisitors() {
        ArrayList list = new ArrayList();
//        list.add(new PyCheckerVisitor());
        list.add(new PyTodoVisitor());
        list.add(new PyLintVisitor());

        return list;
    }

    /**
     * Processes all python files.
     * 
     * @param monitor
     */
    private void performFullBuild(IProgressMonitor monitor) throws CoreException {

        IProject project = getProject();

        List resourcesToParse = new ArrayList();


        if (project != null) {
	        List visitors = getVisitors();
	        monitor.beginTask("Building...", (visitors.size() * 100) + 30);

            IResource[] members = project.members();

            //get all the python files to get information.
            for (int i = 0; i < members.length; i++) {
                if (members[i].getType() == IResource.FILE) {
                    if (members[i].getFileExtension().equals("py")) {
                        resourcesToParse.add(members[i]);
                    }
                } else if (members[i].getType() == IResource.FOLDER) {
                    IPath location = ((IFolder) members[i]).getLocation();
                    File folder = new File(location.toOSString());
                    List l = PydevPlugin.getPyFilesBelow(folder, null, false)[0];
                    for (Iterator iter = l.iterator(); iter.hasNext();) {
                        File element = (File) iter.next();
                        IPath path = PydevPlugin.getPath(new Path(element.getAbsolutePath()));
                        IResource resource = project.findMember(path);
                        if (resource != null) {
                            resourcesToParse.add(resource);
                        }
                    }
                }
            }
            monitor.worked(30);
            
            
            for (Iterator it = visitors.iterator(); it.hasNext();) {
                PyDevBuilderVisitor element = (PyDevBuilderVisitor) it.next();
                element.fullBuild(resourcesToParse, monitor);
            }
        }
        monitor.done();

    }
}

