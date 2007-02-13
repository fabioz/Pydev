/*
 * Created on Oct 7, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * This is the Model provider for python elements.
 * 
 * It intercepts the adds/removes and changes the original elements for elements
 * that actually reflect the python model (with source folder, etc).
 * 
 * @author Fabio
 */
public class PythonModelProvider extends PythonBaseModelProvider implements IPipelinedTreeContentProvider {

    /**
     * This method basically replaces all the elements for other resource elements
     * or for wrapped elements.
     *  
     * @see org.eclipse.ui.navigator.IPipelinedTreeContentProvider#getPipelinedChildren(java.lang.Object, java.util.Set)
     */
    @SuppressWarnings("unchecked")
    public void getPipelinedChildren(Object parent, Set currentElements) {
        Object[] children = getChildren(parent);
        currentElements.clear();
        currentElements.addAll(Arrays.asList(children));
    }

    /**
     * This method basically replaces all the elements for other resource elements
     * or for wrapped elements.
     * 
     * @see org.eclipse.ui.navigator.IPipelinedTreeContentProvider#getPipelinedElements(java.lang.Object, java.util.Set)
     */
    @SuppressWarnings("unchecked")
    public void getPipelinedElements(Object input, Set currentElements) {
        Object[] children = getElements(input);
        currentElements.clear();
        currentElements.addAll(Arrays.asList(children));
    }

    /**
     * This method basically get the actual parent for the resource or the parent 
     * for a wrapped element (which may be a resource or a wrapped resource).
     * 
     * @see org.eclipse.ui.navigator.IPipelinedTreeContentProvider#getPipelinedParent(java.lang.Object, java.lang.Object)
     */
    public Object getPipelinedParent(Object object, Object aSuggestedParent) {
        return getParent(object);
    }

    /**
     * This method intercepts some addition to the tree and converts its elements to python 
     * elements.
     * 
     * @see org.eclipse.ui.navigator.IPipelinedTreeContentProvider#interceptAdd(org.eclipse.ui.navigator.PipelinedShapeModification)
     */
    public PipelinedShapeModification interceptAdd(PipelinedShapeModification addModification) {
        convertToPythonElements(addModification, true);
        return addModification;
    }
    
    public boolean interceptRefresh(PipelinedViewerUpdate refreshSynchronization) {
        return convertToPythonElements(refreshSynchronization.getRefreshTargets());
    }

    public PipelinedShapeModification interceptRemove(PipelinedShapeModification removeModification) {
        convertToPythonElements(removeModification, false);
        return removeModification;
    }

    public boolean interceptUpdate(PipelinedViewerUpdate updateSynchronization) {
        return convertToPythonElements(updateSynchronization.getRefreshTargets());
    }

    public void init(ICommonContentExtensionSite aConfig) {
    }

    
    /**
     * This is the function that is responsible for restoring the paths in the tree.
     */
    public void restoreState(IMemento memento) {
    	new PyPackageStateSaver(this, viewer, memento).restoreState();
    }

    /**
     * This is the function that is responsible for saving the paths in the tree.
     */
    public void saveState(IMemento memento) {
    	new PyPackageStateSaver(this, viewer, memento).saveState();
    }
    
    /**
     * Converts the shape modification to use Python elements.
     * 
     * @param modification: the shape modification to convert
     * @param isAdd: boolean indicating whether this convertion is happening in an add operation 
     */
    private void convertToPythonElements(PipelinedShapeModification modification, boolean isAdd) {
        Object parent = modification.getParent();
        if (parent instanceof IContainer) {
            Object pythonParent = getResourceInPythonModel((IResource) parent, true);
            
            if (pythonParent instanceof IWrappedResource) {
                IWrappedResource parentResource = (IWrappedResource) pythonParent;
                modification.setParent(parentResource);
                wrapChildren(parentResource, parentResource.getSourceFolder(), modification.getChildren(), isAdd);
                
            }else if(pythonParent == null){
                //this may happen when a source folder is added 
                
                wrapChildren(parent, null, modification.getChildren(), isAdd);
            }
            
        }else if(parent == null){
            wrapChildren(null, null, modification.getChildren(), isAdd);
        }
        
    }
    
    /**
     * Actually wraps some resource into a wrapped resource.
     * 
     * @param parent this is the parent 
     *        it may be null -- in the case of a remove
     *        it may be a wrapped resource (if it is in the python model)
     *        it may be a resource (if it is a source folder)
     *  
     * 
     * @param pythonSourceFolder this is the python source folder for the resource (it may be null if the resource itself is a source folder
     *        or if it is actually a resource that has already been removed)
     * @param currentChildren those are the children that should be wrapped
     * @param isAdd whether this is an add operation or not
     * @return
     */
    @SuppressWarnings("unchecked")
    protected boolean wrapChildren(Object parent, PythonSourceFolder pythonSourceFolder, Set currentChildren, boolean isAdd) {
        LinkedHashSet convertedChildren = new LinkedHashSet();
        for (Iterator childrenItr = currentChildren.iterator(); childrenItr.hasNext();) {
            Object child = childrenItr.next();
            Object existing = getResourceInPythonModel((IResource) child, true);
            if(existing != null && isAdd){
                childrenItr.remove();
                convertedChildren.add(existing);
            }
            if(existing == null && !isAdd){
                return false; //it has already been removed
            }
            
            if(existing == null){
                //add
                if(child instanceof IFolder){
                    childrenItr.remove();
                    IFolder folder = (IFolder) child;
                    
                    //it may be a PythonSourceFolder
                    if(pythonSourceFolder == null && parent != null){
                    	try {
                    		IProject project = ((IContainer)parent).getProject();
                            PythonNature nature = PythonNature.getPythonNature(project);
                            if(nature!= null){
	                            //check for source folder
	                            Set<String> sourcePathSet = nature.getPythonPathNature().getProjectSourcePathSet();
	                            IPath fullPath = folder.getFullPath();
	                            if(sourcePathSet.contains(fullPath.toString())){
	                            	PythonSourceFolder sourceFolder = new PythonSourceFolder(parent, folder);
	                            	convertedChildren.add(sourceFolder);
	                                //System.out.println("Created source folder: "+ret[i]+" - "+folder.getProject()+" - "+folder.getProjectRelativePath());
	                                Set<PythonSourceFolder> sourceFolders = getProjectSourceFolders(folder);
	                                sourceFolders.add((PythonSourceFolder) sourceFolder);
	                            }else{
	                                //ok, it is not a source folder (it is a regular folder)... so, let's add it as a regular resource
                                    convertedChildren.add(folder);
                                }
                            }
                        } catch (CoreException e) {
                            throw new RuntimeException(e);
                        }                    	
                    }else{
                    	convertedChildren.add(new PythonFolder((IWrappedResource) parent, folder, pythonSourceFolder));
                    }
                    
                }else if(child instanceof IFile){
                    if(pythonSourceFolder != null){
                        //if the python source folder is null, that means that this is a file that is not actually below a source folder.
                        childrenItr.remove();
                        IFile file = (IFile) child;
                        convertedChildren.add(new PythonFile((IWrappedResource) parent, file, pythonSourceFolder));
                    }
                    
                }else if (child instanceof IResource){
                    childrenItr.remove();
                    convertedChildren.add(new PythonResource((IWrappedResource) parent, (IResource) child, pythonSourceFolder));
                }else{
                    throw new RuntimeException("Unexpected class:"+child.getClass());
                }
            }else if(!isAdd){
                //remove
                childrenItr.remove();
                convertedChildren.add(existing);
                IWrappedResource wrapped = (IWrappedResource) existing;
                wrapped.getSourceFolder().removeChild((IResource) child);
            }
        }
        if (!convertedChildren.isEmpty()) {
            currentChildren.addAll(convertedChildren);
            return true;
        }
        return false;
    }


    

}
