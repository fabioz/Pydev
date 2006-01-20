/*
 * Created on Jan 19, 2006
 */
package org.python.pydev.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.python.copiedfromeclipsesrc.CopiedWorkbenchLabelProvider;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * Dialog to select some source folder (in the workspace)
 */
public class PythonSrcFolderSelectionDialog  extends ElementTreeSelectionDialog {

    public PythonSrcFolderSelectionDialog(Shell parent) {
        super(parent, new CopiedWorkbenchLabelProvider(){
            @Override
            public String getText(Object element) {
                if(element instanceof IFolder){
                    IFolder f = (IFolder) element;
                    return f.getProjectRelativePath().toString(); // we want the complete path here...
                }
                return super.getText(element);
            }
        }
        , new SourceFolderContentProvider());
        
        this.setValidator(new ISelectionStatusValidator() {
            public IStatus validate(Object selection[]) {
                if(selection.length > 1) {
                    return new Status(IStatus.ERROR, PydevPlugin.getPluginID(),
                            IStatus.ERROR, "Only one source folder can be selected", null);
                }
                if(selection.length == 1) {
                    if(selection[0] instanceof IFolder) {
                        IFolder folder = (IFolder) selection[0];
                        return new Status(IStatus.OK, PydevPlugin.getPluginID(),
                                IStatus.OK, "Source folder: " + folder.getName() + " selected", null);
                    }
                }
                return new Status(IStatus.ERROR, PydevPlugin.getPluginID(),
                        IStatus.ERROR, "No source folder selected", null);

            }           
        });
        this.setInput(ResourcesPlugin.getWorkspace().getRoot());
    }

}

class SourceFolderContentProvider implements ITreeContentProvider{

    private IWorkspaceRoot workspaceRoot;

    public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof IWorkspaceRoot){
            this.workspaceRoot = (IWorkspaceRoot) parentElement;
            List<IProject> ret = new ArrayList<IProject>();
            
            IProject[] projects = workspaceRoot.getProjects();
            for (IProject project : projects) {
                PythonNature nature = PythonNature.getPythonNature(project);
                if(nature != null){
                    ret.add(project);
                }
            }
            return ret.toArray(new IProject[0]);
        }
        if(parentElement instanceof IProject){
            List<Object> ret = new ArrayList<Object>();
            IProject project = (IProject) parentElement;
            IPythonPathNature nature = PythonNature.getPythonPathNature(project);
            if(nature != null){
                
                try {
                    String[] srcPaths = PythonNature.getStrAsStrItems(nature.getProjectSourcePath());
                    for (String str : srcPaths) {
                        IResource resource = this.workspaceRoot.findMember(new Path(str));
                        if(resource instanceof IFolder){
                            IFolder folder = (IFolder) resource;
                            if(folder.exists()){
                                if(folder != null){
                                    ret.add(folder);
                                }
                            }
                        }
                    }
                    return ret.toArray();
                } catch (CoreException e) {
                    PydevPlugin.log(e);
                }
            }
        }
        return new Object[0];
    }

    public Object getParent(Object element) {
        if (element instanceof IResource)
            return ((IResource) element).getParent();
        return null;
    }

    public boolean hasChildren(Object element) {
        return element instanceof IWorkspace || element instanceof IProject;
    }

    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
    
}