/*
 * Created on 12/06/2005
 */
package org.python.pydev.core;

import java.io.File;

import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Fabio
 */
public interface IPythonNature extends IProjectNature, IGrammarVersionProvider{

    /**
     * Constants persisted
     */
    public static final String PYTHON_VERSION_2_3 = "python 2.3";
    public static final String PYTHON_VERSION_2_4 = "python 2.4";
    public static final String PYTHON_VERSION_2_5 = "python 2.5";
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

    IPythonPathNature getPythonPathNature();
    
    String resolveModule(File file);

    ICodeCompletionASTManager getAstManager();

    /**
     * Rebuilds the path with the current path information (just to refresh it).
     */
	void rebuildPath();

	/**
	 * Rebuilds the path with the current path information, but using the interpreter passed
	 */
	void rebuildPath(String defaultSelectedInterpreter, IProgressMonitor monitor);
    
    IInterpreterManager getRelatedInterpreterManager();

    /**
     * @return the tokens for the builtins. As getting the builtins is VERY usual, we'll keep them here.
     * (we can't forget to change it when the interpreter is changed -- on rebuildPath)
     * 
     * May return null if not set
     */
    IToken[] getBuiltinCompletions();
    /**
     * @param toks those are the tokens that are set as builtin completions.
     */
	void setBuiltinCompletions(IToken[] toks);

	/**
	 * @return the module for the builtins (may return null if not set)
	 */
	IModule getBuiltinMod();
	/**
	 * @param mod the builtion module
	 */
	void setBuiltinMod(IModule mod);
}
