/*
 * Created on 12/06/2005
 */
package org.python.pydev.core;

import java.io.File;

import org.eclipse.core.runtime.CoreException;

/**
 * @author Fabio
 */
public interface IPythonNature {

    public static final String PYTHON_VERSION_2_3 = "python 2.3";
    public static final String PYTHON_VERSION_2_4 = "python 2.4";
    public static final String JYTHON_VERSION_2_1 = "jython 2.1";

    /**
     * this id is provided so that we can have an identifier for python-related things (independent of its version)
     */
    public static final int PYTHON_RELATED = 0;
    
    /**
     * this id is provided so that we can have an identifier for jython-related things (independent of its version)
     */
    public static final int JYTHON_RELATED = 1;

    /**
     * @return the project version given the constants provided
     * @throws CoreException 
     */
    String getVersion() throws CoreException;
    
    /**
     * @return the default version
     * @throws CoreException 
     */
    String getDefaultVersion();

    /**
     * set the project version given the constants provided
     * @throws CoreException 
     */
    void setVersion(String version) throws CoreException;
 
    /**
     * @return whether this project is a jython project
     * @throws CoreException 
     */
    boolean isJython() throws CoreException;

    /**
     * @return whether this project is a python project
     * @throws CoreException 
     */
    boolean isPython() throws CoreException;
    
    /**
     * @return whether this kind of project should accept the decorators syntax
     * @throws CoreException 
     */
    boolean acceptsDecorators() throws CoreException;
    
    /**
     * @return the id that is related to this nature given its type
     * 
     * @see #PYTHON_RELATED
     * @see #JYTHON_RELATED
     */
    int getRelatedId() throws CoreException;
    
    /**
     * @return the directory where the completions should be saved (as well as deltas)
     */
    public File getCompletionsCacheDir();

    /**
     * Saves the ast manager information so that we can retrieve it later.
     */
    public void saveAstManager();

}
