package org.python.pydev.navigator;

/**
 * Interface for a child resource (a resource that has a parent)
 * 
 * @author fabioz
 */
public interface IChildResource {
	
	/**
	 * @return the parent for this resource
	 */
	Object getParent();
	
	/**
	 * @return the actual object we are wrapping here
	 */
	Object getActualObject();
	
	/**
	 * @return the source folder that contains this resource
	 */
	PythonSourceFolder getSourceFolder();
}
