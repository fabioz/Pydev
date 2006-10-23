package org.python.pydev.navigator;

/**
 * Interface for a child resource (a resource that has a parent)
 * 
 * @author fabioz
 */
public interface IWrappedResource {
	
	int RANK_SOURCE_FOLDER = 0;
	int RANK_PYTHON_FOLDER = 1;
	int RANK_PYTHON_FILE = 2;
    int RANK_PYTHON_RESOURCE = 3;
    int RANK_PYTHON_NODE = 4;

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

    /**
     * @return the ranking for the object. Lower values have higher priorities
     */
    int getRank();
}
