/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.navigator.elements.ProjectConfigError;
import org.python.pydev.navigator.elements.PythonSourceFolder;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * This class contains information about the project (info we need to show in the tree).
 */
public class ProjectInfoForPackageExplorer{
    
    /**
     * Note that the source folders are added/removed lazily (not when the info is recreated)
     */
    public final Set<PythonSourceFolder> sourceFolders = new HashSet<PythonSourceFolder>();
    
    /**
     * Whenever the info is recreated this is also recreated.
     */
    public final List<ProjectConfigError> configErrors = new ArrayList<ProjectConfigError>();

    /**
     * The interpreter info available (may be null)
     */
    public IInterpreterInfo interpreterInfo;

    /**
     * Creates the info for the passed project.
     */
    public ProjectInfoForPackageExplorer(IProject project) {
        this.recreateInfo(project);
    }
    
    /**
     * Recreates the information about the project.
     */
    public void recreateInfo(IProject project) {
        configErrors.clear();
        Tuple<List<ProjectConfigError>, IInterpreterInfo> configErrorsAndInfo = getConfigErrorsAndInfo(project);
        configErrors.addAll(configErrorsAndInfo.o1);
        this.interpreterInfo = configErrorsAndInfo.o2;
    }
    
    /**
     * Never returns null.
     * 
     * This method should only be called through recreateInfo.
     */
    @SuppressWarnings("unchecked")
    private Tuple<List<ProjectConfigError>, IInterpreterInfo> getConfigErrorsAndInfo(IProject project) {
        if(project == null || !project.isOpen()){
            return new Tuple<List<ProjectConfigError>, IInterpreterInfo>(new ArrayList(), null);
        }
        PythonNature nature = PythonNature.getPythonNature(project);
        if(nature == null){
            return new Tuple<List<ProjectConfigError>, IInterpreterInfo>(new ArrayList(), null);
        }
        
        //If the info is not readily available, we try to get some more times... after that, if still not available,
        //we just return as if it's all OK.
        Tuple<List<ProjectConfigError>, IInterpreterInfo> configErrorsAndInfo = null;
        boolean goodToGo = false;
        for(int i=0;i<10&&!goodToGo;i++){
            try{
                configErrorsAndInfo = nature.getConfigErrorsAndInfo(project);
                goodToGo = true;
            }catch(PythonNatureWithoutProjectException e1){
                goodToGo = false;
                synchronized(this){
                    try{
                        wait(100);
                    }catch(InterruptedException e){
                    }
                }
            }
        }
        if(configErrorsAndInfo == null){
            return new Tuple<List<ProjectConfigError>, IInterpreterInfo>(new ArrayList(), null);
        }
        
        if(nature != null){
            try {
                project.deleteMarkers(PythonBaseModelProvider.PYDEV_PACKAGE_EXPORER_PROBLEM_MARKER, true, 0);
            } catch (Exception e) {
                Log.log(e);
            }
            
            List<ProjectConfigError> errors = configErrorsAndInfo.o1;
            for(ProjectConfigError error:errors){
                try {
                    Map attributes = new HashMap();
                    attributes.put(IMarker.MESSAGE, error.getLabel());
                    attributes.put(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                    MarkerUtilities.createMarker(project, attributes, PythonBaseModelProvider.PYDEV_PACKAGE_EXPORER_PROBLEM_MARKER);
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }

        return configErrorsAndInfo;
    }
    
}