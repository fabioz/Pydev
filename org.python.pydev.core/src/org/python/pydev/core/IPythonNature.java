/*
 * Created on 12/06/2005
 */
package org.python.pydev.core;

import org.eclipse.core.runtime.CoreException;

/**
 * @author Fabio
 */
public interface IPythonNature {

    public static final String PYTHON_VERSION_2_3 = "python 2.3";
    public static final String PYTHON_VERSION_2_4 = "python 2.4";
    public static final String JYTHON_VERSION_2_1 = "jython 2.1";

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
}
