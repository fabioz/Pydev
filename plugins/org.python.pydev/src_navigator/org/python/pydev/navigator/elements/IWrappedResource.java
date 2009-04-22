package org.python.pydev.navigator.elements;

import org.eclipse.core.runtime.IAdaptable;


/**
 * Interface for a child resource (a resource that has a parent)
 * 
 * @author fabioz
 */
public interface IWrappedResource extends IAdaptable, ISortedElement {
    

    /**
     * @return the parent for this resource
     */
    Object getParentElement();
    
    /**
     * @return the actual object we are wrapping here
     */
    Object getActualObject();
    
    /**
     * @return the source folder that contains this resource
     */
    PythonSourceFolder getSourceFolder();

}
