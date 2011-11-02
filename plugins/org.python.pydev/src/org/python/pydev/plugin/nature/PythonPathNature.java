/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jun 2, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.plugin.nature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PropertiesHelper;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringSubstitution;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

/**
 * @author Fabio Zadrozny
 */
public class PythonPathNature implements IPythonPathNature {

    private volatile IProject fProject;
    private volatile PythonNature fNature;


    /**
     * This is the property that has the python path - associated with the project.
     */
    private static QualifiedName projectSourcePathQualifiedName = null;
    static QualifiedName getProjectSourcePathQualifiedName() {
        if(projectSourcePathQualifiedName == null){
            projectSourcePathQualifiedName = new QualifiedName(PydevPlugin.getPluginID(), "PROJECT_SOURCE_PATH");
        }
        return projectSourcePathQualifiedName;
    }
    
    /**
     * This is the property that has the external python path - associated with the project.
     */
    private static QualifiedName projectExternalSourcePathQualifiedName = null;
    static QualifiedName getProjectExternalSourcePathQualifiedName() {
        if(projectExternalSourcePathQualifiedName == null){
            projectExternalSourcePathQualifiedName = new QualifiedName(PydevPlugin.getPluginID(), "PROJECT_EXTERNAL_SOURCE_PATH");
        }
        return projectExternalSourcePathQualifiedName;
    }
    
    /**
     * This is the property that has the external python path - associated with the project.
     */
    private static QualifiedName projectVariableSubstitutionQualifiedName = null;
    static QualifiedName getProjectVariableSubstitutionQualifiedName() {
        if(projectVariableSubstitutionQualifiedName == null){
            projectVariableSubstitutionQualifiedName = new QualifiedName(PydevPlugin.getPluginID(), "PROJECT_VARIABLE_SUBSTITUTION");
        }
        return projectVariableSubstitutionQualifiedName;
    }


    public void setProject(IProject project, IPythonNature nature){
        this.fProject = project;
        this.fNature = (PythonNature) nature;
    }

    
    public IPythonNature getNature(){
        return this.fNature;
    }
    

    private boolean waited = false;
    /**
     * Returns a list of paths with the complete pythonpath for this nature.
     * 
     * This includes the pythonpath for the project, all the referenced projects and the
     * system.
     */
    public List<String> getCompleteProjectPythonPath(IInterpreterInfo interpreter, IInterpreterManager manager) {
        IModulesManager projectModulesManager = getProjectModulesManager();
        if(projectModulesManager == null){
            if(!waited){
                waited = true;
                for(int i=0;i<10 && projectModulesManager == null;i++){
                    //We may get into a race condition, so, try to see if we can get it.
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        //OK
                    }
                    projectModulesManager = getProjectModulesManager();
                }
            }
        }
        if(projectModulesManager == null){
            return null;
        }
        return projectModulesManager.getCompletePythonPath(interpreter, manager);
    }
    
    private IModulesManager getProjectModulesManager(){
        IPythonNature nature = fNature;
        if(nature == null){
            return null;
        }
        
        ICodeCompletionASTManager astManager = nature.getAstManager();
        if(astManager == null) {
            // AST manager might not be yet available
            // Code completion job is scheduled to be run
            return null;
        }
              
        return astManager.getModulesManager();
    }

    private static volatile long doFullSynchAt = -1;
    private static final Map<String, Long> directMembersChecked = new HashMap<String, Long>();

    
    /**
     * @return the project pythonpath with complete paths in the filesystem.
     */
    public String getOnlyProjectPythonPathStr(boolean addExternal) throws CoreException  {
        String source = null;
        String external = null;
        String contributed = null;
        IProject project = fProject;
        PythonNature nature = fNature;

        if(project == null  || nature == null){
            return "";
        }
        
        //Substitute with variables!
        StringSubstitution stringSubstitution = new StringSubstitution(nature);
        
        source = getProjectSourcePath(true);
        if(addExternal){
        	external = getProjectExternalSourcePath(true);
        }
        contributed = stringSubstitution.performPythonpathStringSubstitution(getContributedSourcePath(project));
            
        if(source == null){
            source = "";
        }
        //we have to work on this one to resolve to full files, as what is stored is the position
        //relative to the project location
        List<String> strings = StringUtils.splitAndRemoveEmptyTrimmed(source, '|');
        FastStringBuffer buf = new FastStringBuffer();
        
        IWorkspaceRoot root = null;
        
        ResourcesPlugin resourcesPlugin = ResourcesPlugin.getPlugin();
        for (String currentPath:strings) {
            if(currentPath.trim().length()>0){
                IPath p = new Path(currentPath);
                
                if(resourcesPlugin == null){
                    //in tests
                    buf.append(currentPath);
                    buf.append("|");
                    continue;
                }
                
                if(root == null){
                    root = ResourcesPlugin.getWorkspace().getRoot();
                }
                
                if(p.segmentCount() < 1){
                    Log.log("Found no segment in: "+currentPath+" for: "+project);
                    continue; //No segment? Really weird!
                }
                
                //try to get relative to the workspace 
                IContainer container = null;
                IResource r = null;
                try {
                    r = root.findMember(p);
                } catch (Exception e) {
                    Log.log(e);
                }
                
                if(!(r instanceof IContainer) && !(r instanceof IFile)){
                    
                    //If we didn't find the file, let's try to sync things, as this can happen if the workspace
                    //is still not properly synchronized.
                    String firstSegment = p.segment(0);
                    IResource firstSegmentResource = root.findMember(firstSegment);
                    if(!(firstSegmentResource instanceof IContainer) && !(firstSegmentResource instanceof IFile)){
                        //we cannot even get the 1st part... let's do sync
                        long currentTimeMillis = System.currentTimeMillis();
                        if(doFullSynchAt == -1 || currentTimeMillis > doFullSynchAt){
                            doFullSynchAt = currentTimeMillis + (60 * 2 * 1000); //do a full synch at most once every 2 minutes
                            try {
                                root.refreshLocal(p.segmentCount()+1, null);
                            } catch (CoreException e) {
                                //ignore
                            }
                        }
                        
                    }else{
                        Long doSynchAt = directMembersChecked.get(firstSegment);
                        long currentTimeMillis = System.currentTimeMillis();
                        if(doSynchAt == null || currentTimeMillis > doFullSynchAt){
                            directMembersChecked.put(firstSegment, currentTimeMillis + (60 * 2 * 1000));
                            //OK, we can get to the 1st segment, so, let's do a refresh just from that point on, not in the whole workspace...
                            try {
                                firstSegmentResource.refreshLocal(p.segmentCount(), null);
                            } catch (CoreException e) {
                                //ignore
                            }
                        }
                        
                    } 
                    
                    //Now, try to get it knowing that it's properly synched (it may still not be there, but at least we tried it)
                    try {
                        r = root.findMember(p);
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
                
                if(r instanceof IContainer){
                    container = (IContainer) r;
                    buf.append(REF.getFileAbsolutePath(container.getLocation().toFile()));
                    buf.append("|");
                
                }else if(r instanceof IFile){ //zip/jar/egg file
                    String extension = r.getFileExtension();
                    if(extension == null || FileTypesPreferencesPage.isValidZipFile("."+extension) == false){
                        Log.log("Error: the path "+currentPath+" is a file but is not a recognized zip file.");
                        
                    }else{
                        buf.append(REF.getFileAbsolutePath(r.getLocation().toFile()));
                        buf.append("|");
                    }
                
                }else{
                    //We're now always making sure that it's all synchronized, so, if we got here, it really doesn't exist (let's warn about it)
                    
                    //Not in workspace?... maybe it was removed, so, let the user know about it (and still add it to the pythonpath as is)
                    Log.log(IStatus.WARNING, "Unable to find the path "+currentPath+" in the project were it's \n" +
                            "added as a source folder for pydev (project: "+project.getName()+") member:"+r, null);
                    
                    //No good: try to get it relative to the project
                    String curr = currentPath;
                    IPath path = new Path(curr.trim());
                    if(project.getFullPath().isPrefixOf(path)){
                        path = path.removeFirstSegments(1);
                        if(FileTypesPreferencesPage.isValidZipFile(curr)){
                            r = project.getFile(path);
                            
                        }else{
                            //get it relative to the project
                            r = project.getFolder(path);
                        }
                        if(r!=null){
                            buf.append(REF.getFileAbsolutePath(r.getLocation().toFile()));
                            buf.append("|");
                            continue; //Don't go on to append it relative to the workspace root.
                        }
                    }
                    
                    //Nothing worked: force it to be relative to the workspace.
                    IPath rootLocation = root.getRawLocation();
                    
                    //Note that this'll be cached for later use.
                    buf.append(REF.getFileAbsolutePath(rootLocation.append(currentPath.trim()).toFile()));
                    buf.append("|");
                }
            }
        }
        
            
        if(external == null){
            external = "";
        }
        return buf.append("|").append(external).append("|").append(contributed).toString();
    }


    /**
     * Gets the source path contributed by plugins.
     * 
     * See: http://sourceforge.net/tracker/index.php?func=detail&aid=1988084&group_id=85796&atid=577329
     * 
     * @throws CoreException
     */
    @SuppressWarnings("unchecked")
    private String getContributedSourcePath(IProject project) throws CoreException {
        FastStringBuffer buff = new FastStringBuffer();
        List<IPythonPathContributor> contributors = ExtensionHelper.getParticipants("org.python.pydev.pydev_pythonpath_contrib");
        for (IPythonPathContributor contributor : contributors) {
            String additionalPythonPath = contributor.getAdditionalPythonPath(project);
            if (additionalPythonPath != null && additionalPythonPath.trim().length() > 0) {
                if (buff.length() > 0){
                    buff.append("|");
                }
                buff.append(additionalPythonPath.trim());
            }
        }
        return buff.toString();
    }

    

    public void setProjectSourcePath(String newSourcePath) throws CoreException {
        PythonNature nature = fNature;

        if(nature != null){
            nature.getStore().setPathProperty(PythonPathNature.getProjectSourcePathQualifiedName(), newSourcePath);
        }
    }

    public void setProjectExternalSourcePath(String newExternalSourcePath) throws CoreException {
        PythonNature nature = fNature;
        if(nature != null){
            nature.getStore().setPathProperty(PythonPathNature.getProjectExternalSourcePathQualifiedName(), newExternalSourcePath);
        }
    }
    
    public void setVariableSubstitution(Map<String, String> variableSubstitution) throws CoreException {
        PythonNature nature = fNature;
        if(nature != null){
            nature.getStore().setMapProperty(PythonPathNature.getProjectVariableSubstitutionQualifiedName(), variableSubstitution);
        }
    }

    
    public void clearCaches() {
    }

    
    public Set<String> getProjectSourcePathSet(boolean replace) throws CoreException {
        String projectSourcePath;
        PythonNature nature = fNature;
        if(nature == null){
            return new HashSet<String>();
        }
        projectSourcePath = getProjectSourcePath(replace);
        return new HashSet<String>(StringUtils.splitAndRemoveEmptyTrimmed(projectSourcePath, '|'));
    }
    
    public String getProjectSourcePath(boolean replace) throws CoreException {
        String projectSourcePath;
        boolean restore = false;
        IProject project = fProject;
        PythonNature nature = fNature;
        
        if(project == null || nature == null){
            return "";
        }
        projectSourcePath = nature.getStore().getPathProperty(PythonPathNature.getProjectSourcePathQualifiedName());
        if(projectSourcePath == null){
            //has not been set
            return "";
        }
        
        //we have to validate it, because as we store the values relative to the workspace, and not to the 
        //project, the path may become invalid (in which case we have to make it compatible again).
        StringBuffer buffer = new StringBuffer();
        List<String> paths = StringUtils.splitAndRemoveEmptyTrimmed(projectSourcePath, '|');
        IPath projectPath = project.getFullPath();
        for (String path : paths) {
            if(path.trim().length() > 0){
                if(path.indexOf("${") != -1){ //Account for the string substitution.
                    buffer.append(path); 
                }else{
                    IPath p = new Path(path);
                    if(p.isEmpty()){
                        continue; //go to the next...
                    }
                    if(projectPath != null && !projectPath.isPrefixOf(p)){
                        p = p.removeFirstSegments(1);
                        p = projectPath.append(p);
                        restore = true;
                    }
                    buffer.append(p.toString());
                }
                buffer.append("|");
            }
        }
        
        //it was wrong and has just been fixed
        if(restore){
            projectSourcePath = buffer.toString();
            setProjectSourcePath(projectSourcePath);
            if(nature != null){
                //yeap, everything has to be done from scratch, as all the filesystem paths have just
                //been turned to dust!
                nature.rebuildPath();
            }
        }
        return trimAndReplaceVariablesIfNeeded(replace, projectSourcePath, nature);
    }


    /**
     * Replaces the variables if needed.
     */
    private String trimAndReplaceVariablesIfNeeded(boolean replace, String projectSourcePath, PythonNature nature) throws CoreException{
        String ret = StringUtils.leftAndRightTrim(projectSourcePath, '|');
        if(replace){
            StringSubstitution substitution = new StringSubstitution(nature);
            ret = substitution.performPythonpathStringSubstitution(ret);
        }
        return ret;
    }

    public String getProjectExternalSourcePath(boolean replace) throws CoreException {
        String extPath;

        PythonNature nature = fNature;
        if(nature == null){
            return "";
        }
        //no need to validate because those are always 'file-system' related
        extPath = nature.getStore().getPathProperty(PythonPathNature.getProjectExternalSourcePathQualifiedName());
        
        if(extPath == null){
            extPath = "";
        }
        return trimAndReplaceVariablesIfNeeded(replace, extPath, nature);
    }
    
    public List<String> getProjectExternalSourcePathAsList(boolean replaceVariables) throws CoreException {
        String projectExternalSourcePath = getProjectExternalSourcePath(replaceVariables);
        List<String> externalPaths = StringUtils.splitAndRemoveEmptyTrimmed(projectExternalSourcePath, '|');
        return externalPaths;
    }

    public Map<String,String> getVariableSubstitution() throws CoreException, MisconfigurationException, PythonNatureWithoutProjectException {
    	return getVariableSubstitution(true);
    }
    
    /**
     * Returns the variables in the python nature and in the interpreter.
     */
    public Map<String,String> getVariableSubstitution(boolean addInterpreterInfoSubstitutions) throws CoreException, MisconfigurationException, PythonNatureWithoutProjectException {
    	PythonNature nature = this.fNature;
    	if(nature == null){
    		return new HashMap<String, String>();
    	}
    	
    	Map<String, String> variableSubstitution;
    	if(addInterpreterInfoSubstitutions){
	    	
	    	IInterpreterInfo info = nature.getProjectInterpreter();
	    	Properties stringSubstitutionVariables = info.getStringSubstitutionVariables();
	    	if(stringSubstitutionVariables == null){
	    		variableSubstitution = new HashMap<String, String>();
	    	}else{
	    		variableSubstitution = PropertiesHelper.createMapFromProperties(stringSubstitutionVariables);
	    	}
    	}else{
    		variableSubstitution = new HashMap<String, String>();
    	}
    	
        //no need to validate because those are always 'file-system' related
    	Map<String, String> variableSubstitution2 = nature.getStore().getMapProperty(PythonPathNature.getProjectVariableSubstitutionQualifiedName());
    	if(variableSubstitution2 != null){
    		if(variableSubstitution != null){
    			variableSubstitution.putAll(variableSubstitution2);
    		}else{
    			variableSubstitution = variableSubstitution2;
    		}
    	}
    	
    	//never return null!
    	if(variableSubstitution == null){
    		variableSubstitution = new HashMap<String, String>();
    	}
    	return variableSubstitution;
    }


}
