/*
 * Created on Oct 7, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import java.util.Arrays;
import java.util.HashSet;
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
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * This is the Model provider for python elements.
 * 
 * It intercepts the adds/removes and changes the original elements for elements
 * that actually reflect the python model (with source folder, etc).
 * 
 * 
 * Tests for package explorer:
 * 1. Start eclipse with a file deep in the structure and without having anything expanded in the tree, make a 'show in'
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
        convertToPythonElementsAddOrRemove(addModification, true);
        return addModification;
    }
    
    public boolean interceptRefresh(PipelinedViewerUpdate refreshSynchronization) {
        return convertToPythonElementsUpdateOrRefresh(refreshSynchronization.getRefreshTargets());
    }

    public PipelinedShapeModification interceptRemove(PipelinedShapeModification removeModification) {
        convertToPythonElementsAddOrRemove(removeModification, false);
        return removeModification;
    }

    public boolean interceptUpdate(PipelinedViewerUpdate updateSynchronization) {
        return convertToPythonElementsUpdateOrRefresh(updateSynchronization.getRefreshTargets());
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
    private void convertToPythonElementsAddOrRemove(PipelinedShapeModification modification, boolean isAdd) {
        Object parent = modification.getParent();
        if (parent instanceof IContainer) {
        	IContainer parentContainer = (IContainer) parent;
            Object pythonParent = getResourceInPythonModel(parentContainer, true);
            
            if (pythonParent instanceof IWrappedResource) {
                IWrappedResource parentResource = (IWrappedResource) pythonParent;
                modification.setParent(parentResource);
                wrapChildren(parentResource, parentResource.getSourceFolder(), modification.getChildren(), isAdd);
                
            }else if(pythonParent == null){
                
            	Object parentInWrap = parentContainer;
            	PythonSourceFolder sourceFolderInWrap = null;
            	
                //this may happen when a source folder is added or some element that still doesn't have it's parent in the model...
            	//so, we have to get the parent's parent until we actually 'know' that it is not in the model (or until we run
            	//out of parents to try)
            	//the case in which we reproduce this is Test 1 (described in the class)
                FastStack<Object> found = new FastStack<Object>();
            	while(true){
                    
            		//add the current to the found
            		found.add(parentContainer);
            		parentContainer = parentContainer.getParent();
            		if(parentContainer == null){
            			break;
            		}
                    
            		if(parentContainer instanceof IProject){
                        //we got to the project without finding any part of a python model already there, so, let's see
                        //if any of the parts was actually a source folder (that was still not added)
                        tryCreateModelFromProject((IProject) parentContainer, found);
                        //and now, if it was created, try to convert it to the python model (without any further add)
                        convertToPythonElementsUpdateOrRefresh(modification.getChildren());
                        return;
            		}
                    
                    
            		Object p = getResourceInPythonModel(parentContainer, true);
            		
            		if(p instanceof IWrappedResource){
            			IWrappedResource wrappedResource = (IWrappedResource) p;
            			sourceFolderInWrap = wrappedResource.getSourceFolder();
            			
            			while(found.size() > 0){
            				Object f = found.pop();
							//creating is enough to add it to the model
            				if(f instanceof IFile){
            					wrappedResource = new PythonFile(wrappedResource, (IFile)f, sourceFolderInWrap);
            				}else if(f instanceof IFolder){
            					wrappedResource = new PythonFolder(wrappedResource, (IFolder)f, sourceFolderInWrap);
            				}
            			}
            			parentInWrap = wrappedResource;
            			break;
            		}
            		
            	}
            	
            	
                wrapChildren(parentInWrap, sourceFolderInWrap, modification.getChildren(), isAdd);
            }
            
        }else if(parent == null){
            wrapChildren(null, null, modification.getChildren(), isAdd);
        }
        
    }
    
    /**
     * Given a Path from the 1st child of the project, will try to create that path in the python model.
     * @param project the project 
     * @param found a stack so that the last element added is the leaf of the path we want to discover 
     */
    private void tryCreateModelFromProject(IProject project, FastStack<Object> found) {
        PythonNature nature = PythonNature.getPythonNature(project);
        if(nature== null){
            return;//if the python nature is not available, we won't have any python elements here
        }
        Set<String> sourcePathSet = new HashSet<String>();
        try {
            sourcePathSet = nature.getPythonPathNature().getProjectSourcePathSet();
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }        
        
        Object currentParent = project;
        PythonSourceFolder pythonSourceFolder = null;
        for(Iterator it = found.topDownIterator();it.hasNext();){
            Object child = it.next();
            if(child instanceof IFolder){
                if(pythonSourceFolder == null){
                    pythonSourceFolder = tryWrapSourceFolder(currentParent, (IFolder) child, sourcePathSet);
                    if(pythonSourceFolder != null){
                        currentParent = pythonSourceFolder;
                    }else if(child instanceof IContainer){
                        currentParent = (IContainer) child;
                    }
                    //just go on (if we found the source folder or not, because if we found, that's ok, and if
                    //we didn't, then the children will not be in the python model anyway)
                    continue;
                }
            }
            if(pythonSourceFolder != null){
                IWrappedResource r = doWrap(currentParent, pythonSourceFolder, child);
                if(r != null){
                    child = r;
                }
            }
            currentParent = child;
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
            
            if(existing == null){
                if(isAdd){
                    //add
                    IWrappedResource w = doWrap(parent, pythonSourceFolder, child);
                    if(w != null){ //if it is null, it is not below a python source folder
                        childrenItr.remove();
                        convertedChildren.add(w);
                    }
                }else{
                    continue; //it has already been removed
                }
                
            }else{ //existing != null
                childrenItr.remove();
                convertedChildren.add(existing);
                if(!isAdd){
                    //also remove it from the model
                    IWrappedResource wrapped = (IWrappedResource) existing;
                    wrapped.getSourceFolder().removeChild((IResource) child);
                }
            }
        }
        
        //if we did have some wrapping... go on and add them to the out list (and return true)
        if (!convertedChildren.isEmpty()) {
            currentChildren.addAll(convertedChildren);
            return true;
        }
        
        //nothing happened, so, just say it
        return false;
    }

    @SuppressWarnings("unchecked")
    protected IWrappedResource doWrap(Object parent, PythonSourceFolder pythonSourceFolder, Object child) {
        if (child instanceof IProject){
            //do nothing (because a project is never going to be an IWrappedResource)
            
        }else if(child instanceof IFolder){
            IFolder folder = (IFolder) child;
            
            //it may be a PythonSourceFolder
            if(pythonSourceFolder == null && parent != null){
            	try {
            		IProject project = ((IContainer)parent).getProject();
                    PythonNature nature = PythonNature.getPythonNature(project);
                    if(nature!= null){
                        //check for source folder
                        Set<String> sourcePathSet = nature.getPythonPathNature().getProjectSourcePathSet();
                        tryWrapSourceFolder(parent, folder, sourcePathSet);
                    }
                } catch (CoreException e) {
                    throw new RuntimeException(e);
                }                    	
            }
            if(pythonSourceFolder != null){
                return new PythonFolder((IWrappedResource) parent, folder, pythonSourceFolder);
            }
            
        }else if(child instanceof IFile){
            if(pythonSourceFolder != null){
                //if the python source folder is null, that means that this is a file that is not actually below a source folder -- so, don't wrap it
                return new PythonFile((IWrappedResource) parent, (IFile) child, pythonSourceFolder);
            }
            
        }else if (child instanceof IResource){
            if(pythonSourceFolder != null){
                return new PythonResource((IWrappedResource) parent, (IResource) child, pythonSourceFolder);
            }
            
        }else{
            throw new RuntimeException("Unexpected class:"+child.getClass());
        }
        
        return null;
    }

    private PythonSourceFolder tryWrapSourceFolder(Object parent, IFolder folder, Set<String> sourcePathSet) {
        IPath fullPath = folder.getFullPath();
        if(sourcePathSet.contains(fullPath.toString())){
        	PythonSourceFolder sourceFolder = new PythonSourceFolder(parent, folder);
            //System.out.println("Created source folder: "+ret[i]+" - "+folder.getProject()+" - "+folder.getProjectRelativePath());
            Set<PythonSourceFolder> sourceFolders = getProjectSourceFolders(folder);
            sourceFolders.add((PythonSourceFolder) sourceFolder);
            return sourceFolder;
        }
        return null;
    }


    
    /**
     * Converts elements to the python model -- but only creates it if it's parent is found in the python model
     */
    @SuppressWarnings("unchecked")
    protected boolean convertToPythonElementsUpdateOrRefresh(Set currentChildren) {
        LinkedHashSet convertedChildren = new LinkedHashSet();
        for (Iterator childrenItr = currentChildren.iterator(); childrenItr.hasNext();) {
            Object child = childrenItr.next();
            if(child instanceof IResource && !(child instanceof IWrappedResource)){
                IResource res = (IResource) child;
                
                Object resourceInPythonModel = getResourceInPythonModel(res, true);
                if(resourceInPythonModel != null){
                    //if it is in the python model, just go on
                    childrenItr.remove();
                    convertedChildren.add(resourceInPythonModel);
                    
                }else{
                    //now, if it's not but its parent is, go on and create it
                    Object pythonParent = getResourceInPythonModel(res.getParent(), true);
                    if(pythonParent instanceof IWrappedResource){
                        IWrappedResource parent = (IWrappedResource) pythonParent;
                        if (res instanceof IProject){
                            //do nothing (because a project is never going to be an IWrappedResource)
                        }else if(res instanceof IFolder){
                            childrenItr.remove();
                            convertedChildren.add(new PythonFolder(parent, (IFolder) res, parent.getSourceFolder()));
                        }else if(res instanceof IFile){
                            childrenItr.remove();
                            convertedChildren.add(new PythonFile(parent, (IFile) res, parent.getSourceFolder()));
                        }else if (child instanceof IResource){
                            childrenItr.remove();
                            convertedChildren.add(new PythonResource(parent, (IResource) child, parent.getSourceFolder()));
                        }
                    }
                }
                
            }
        }
        if (!convertedChildren.isEmpty()) {
            currentChildren.addAll(convertedChildren);
            return true;
        }
        return false;        
        
    }
}
