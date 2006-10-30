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
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;

/**
 * 
 * 
 * @author Fabio
 */
public class PythonModelProvider extends PythonBaseModelProvider implements IPipelinedTreeContentProvider {

    @SuppressWarnings("unchecked")
    public void getPipelinedChildren(Object parent, Set currentChildren) {
        Object[] children = getChildren(parent);
        for (Iterator iter = currentChildren.iterator(); iter.hasNext();){
            Object next = iter.next();
            if (next instanceof IResource && !(next instanceof IWrappedResource)){
                iter.remove();
            }
        }
        currentChildren.addAll(Arrays.asList(children));
    }

    @SuppressWarnings("unchecked")
    public void getPipelinedElements(Object input, Set currentElements) {
        Object[] children = getElements(input);

        for (Iterator iter = currentElements.iterator(); iter.hasNext();){
            Object next = iter.next();
            if (next instanceof IResource && !(next instanceof IWrappedResource)){
                iter.remove();
            }
        }

        currentElements.addAll(Arrays.asList(children));
    }

    public Object getPipelinedParent(Object object, Object aSuggestedParent) {
        return getParent(object);
    }

    public PipelinedShapeModification interceptAdd(PipelinedShapeModification addModification) {
        convertToPythonElements(addModification, true);
        return addModification;
    }

    /**
     * Converts the shape modification to use Python elements.
     * 
     * @param modification the shape modification to convert
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
                //TODO:Check if it is actually a source folder (and create it in the model as needed)
                wrapChildren(null, null, modification.getChildren(), isAdd, false);
            }
            
        }else if(parent == null){
            wrapChildren(null, null, modification.getChildren(), isAdd);
        }
        
    }
    
    protected boolean wrapChildren(Object parent, PythonSourceFolder pythonSourceFolder, Set currentChildren, boolean isAdd) {
        return wrapChildren(parent, pythonSourceFolder, currentChildren, isAdd, true);
    }
    
    @SuppressWarnings("unchecked")
    protected boolean wrapChildren(Object parent, PythonSourceFolder pythonSourceFolder, Set currentChildren, boolean isAdd, boolean createIfNonExisting) {
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
            
            if(existing == null && createIfNonExisting){
                //add
                if(child instanceof IFolder){
                    childrenItr.remove();
                    IFolder folder = (IFolder) child;
                    convertedChildren.add(new PythonFolder(parent, folder, pythonSourceFolder));
                    
                }else if(child instanceof IFile){
                    childrenItr.remove();
                    IFile file = (IFile) child;
                    convertedChildren.add(new PythonFile(parent, file, pythonSourceFolder));
                    
                }else if (child instanceof IResource){
                    childrenItr.remove();
                    convertedChildren.add(new PythonResource(parent, (IResource) child, pythonSourceFolder));
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

    public void restoreState(IMemento aMemento) {
    }

    public void saveState(IMemento aMemento) {
    }

}
