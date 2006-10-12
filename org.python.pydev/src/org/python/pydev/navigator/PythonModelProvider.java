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
        for (Iterator iter = currentChildren.iterator(); iter.hasNext();)
            if (iter.next() instanceof IResource)
                iter.remove();
        currentChildren.addAll(Arrays.asList(children));
    }

    @SuppressWarnings("unchecked")
    public void getPipelinedElements(Object input, Set currentElements) {
        Object[] children = getElements(input);

        for (Iterator iter = currentElements.iterator(); iter.hasNext();)
            if (iter.next() instanceof IResource)
                iter.remove();

        currentElements.addAll(Arrays.asList(children));
    }

    public Object getPipelinedParent(Object object, Object aSuggestedParent) {
        return getParent(object);
    }

    public PipelinedShapeModification interceptAdd(PipelinedShapeModification addModification) {
        convertToPythonElements(addModification);
        return addModification;
    }

    /**
     * Converts the shape modification to use Python elements.
     * 
     * @param modification the shape modification to convert
     */
    private boolean convertToPythonElements(PipelinedShapeModification modification) {

        Object parent = modification.getParent();
        if (parent instanceof IContainer) {
            Object pythonParent = getResourceInPythonModel(parent);
            if (pythonParent instanceof IChildResource) {
                IChildResource parentResource = (IChildResource) pythonParent;
                modification.setParent(parentResource);
                return wrapChildren(parentResource, parentResource.getSourceFolder(), modification.getChildren());
            }
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private boolean convertToPythonElements(Set currentChildren) {
        LinkedHashSet convertedChildren = new LinkedHashSet();
        for (Iterator childrenItr = currentChildren.iterator(); childrenItr.hasNext();) {
            Object child = childrenItr.next();
            if(child instanceof IResource){
                childrenItr.remove();
                IResource res = (IResource) child;
                Object pythonParent = getResourceInPythonModel(res.getParent());
                if(pythonParent instanceof IChildResource){
                    IChildResource parent = (IChildResource) pythonParent;
                    if(res instanceof IFolder){
                        convertedChildren.add(new PythonFolder(parent, (IFolder) res, parent.getSourceFolder()));
                    }else if(res instanceof IFile){
                        convertedChildren.add(new PythonFile(parent, (IFile) res, parent.getSourceFolder()));
                    }else if (child instanceof IResource){
                        childrenItr.remove();
                        convertedChildren.add(new PythonResource(parent, child, parent.getSourceFolder()));
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

    public boolean interceptRefresh(PipelinedViewerUpdate refreshSynchronization) {
        return convertToPythonElements(refreshSynchronization.getRefreshTargets());
    }

    public PipelinedShapeModification interceptRemove(PipelinedShapeModification removeModification) {
        convertToPythonElements(removeModification);
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
