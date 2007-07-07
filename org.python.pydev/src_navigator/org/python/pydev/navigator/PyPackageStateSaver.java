package org.python.pydev.navigator;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IMemento;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This class saves and restores the expanded and selected items in the tree.
 */
public class PyPackageStateSaver {
	
    private static final boolean DEBUG = false;
	
    private PythonModelProvider provider;
	private Viewer viewer;
	private IMemento memento;

	public PyPackageStateSaver(PythonModelProvider provider, Viewer viewer, IMemento memento) {
		this.provider = provider;
		this.viewer = viewer;
		this.memento = memento;
	}

	public void restoreState() {
        try{
            if(!(viewer instanceof TreeViewer)){
                return;
            }
            
            TreeViewer treeViewer = (TreeViewer) viewer;
            
            IMemento[] expanded = memento.getChildren("expanded");
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            for (IMemento m : expanded) {
                Object resource = getResourceFromPath(root, m);
                if(resource != null){
                    if(DEBUG){
                        System.out.println("Expanding:"+resource);
                    }
                    //it has to be done level by level because the children may be created
                    //for each expand (so, we 1st must expand the source folder in order to
                    //get the correct folders beneath it).
                    treeViewer.expandToLevel(resource, 1);
                }
            }
            
            
            ArrayList<TreePath> paths = new ArrayList<TreePath>();
            IMemento[] selected = memento.getChildren("selected");
            for (IMemento m : selected) {
                Object resource = getResourceFromPath(root, m);
                
                if(resource != null){
                	treeViewer.expandToLevel(resource, 1);
                	if(DEBUG){
                	    System.out.println("Selecting:"+resource);
                	}
                    paths.add(new TreePath(getCompletPath(resource).toArray()));
                }
            }
            
            treeViewer.setSelection(new TreeSelection(paths.toArray(new TreePath[0])), true);
        }catch (Exception e) {
            PydevPlugin.log(e);
        }
    }

	/**
	 * This method will get the complete path in the tree for a resource (or wrapped resource)
	 */
    private ArrayList<Object> getCompletPath(Object resource) {
    	int max = 100; // cannot have more than 100 levels... ok? (this is just a 'safeguard')
    	int i=0;
		ArrayList<Object> ret = new ArrayList<Object>();
		ret.add(0, resource);
		
		while(true){
			i++;
			if(i > max){
				return new ArrayList<Object>();//something strange happened...
				
			}else if(resource instanceof IProject){
				break;
				
			}else if(resource instanceof IWrappedResource){
				IWrappedResource w = (IWrappedResource) resource;
				resource = w.getParentElement();
				if(resource == null){
					break;
				}
				ret.add(0, resource);
				
			}else if(resource instanceof IResource){
				IResource r = (IResource) resource;
				resource = r.getParent();
				if(resource == null){
					break;
				}
				ret.add(0, resource);
			}
		}
		
		return ret;
	}

	private Object getResourceFromPath(IWorkspaceRoot root, IMemento m) {
        IPath path = Path.fromPortableString(m.getID());
        IResource resource = root.getFileForLocation(path);
        if(resource == null || !resource.exists()){
            resource = root.getContainerForLocation(path);
        }
        if(resource != null && resource.exists()){
            return provider.getResourceInPythonModel(resource);
        }
        return null;
    }

    /**
     * This is the function that is responsible for saving the paths in the tree.
     */
    public void saveState() {
        try{
            if(!(viewer instanceof TreeViewer)){
                return;
            }
            
            TreeViewer treeViewer = (TreeViewer) viewer;
            TreePath[] expandedTreePaths = treeViewer.getExpandedTreePaths();
            for (TreePath path : expandedTreePaths) {
                save(path, "expanded");
            }
            
            ISelection selection = viewer.getSelection();
            if(selection instanceof ITreeSelection){
                ITreeSelection treeSelection = (ITreeSelection) selection;
                TreePath[] paths = treeSelection.getPaths();
                for (TreePath path : paths) {
                    save(path, "selected");
                }
            }
        }catch (Exception e) {
            PydevPlugin.log(e);
        }
    }
    
    /**
     * Saves some selection in the memento object.
     */
    private void save(TreePath treePath, String type) {
        if(treePath != null){
            Object object = treePath.getLastSegment();
            if(object instanceof IAdaptable){
                IAdaptable adaptable = (IAdaptable) object;
                IResource resource = (IResource) adaptable.getAdapter(IResource.class);
                if(resource != null){
                    IPath path = resource.getLocation();
                    if(path != null){
                        memento.createChild(type, path.toPortableString());
                    }
                }
            }
        }
    }

}
