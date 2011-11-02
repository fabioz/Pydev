/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 13, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;

import com.python.pydev.analysis.AnalysisPlugin;


public class AdditionalSystemInterpreterInfo extends AbstractAdditionalInfoWithBuild{

    private IInterpreterManager manager;
    private final String additionalInfoInterpreter;
    
    /**
     * holds system info (interpreter name points to system info)
     */
    private static Map<Tuple<String, String>, AbstractAdditionalTokensInfo> additionalSystemInfo = 
        new HashMap<Tuple<String, String>, AbstractAdditionalTokensInfo>();

    public AdditionalSystemInterpreterInfo(IInterpreterManager manager, String interpreter) throws MisconfigurationException {
        super(false); //don't call init just right now...
        this.manager = manager;
        this.additionalInfoInterpreter = interpreter;
        init();
    }
    
    public IInterpreterManager getManager(){
        return manager;
    }
    
    public String getAdditionalInfoInterpreter(){
        return additionalInfoInterpreter;
    }
    
    private volatile File persistingFolderCache = null;
    
    /**
     * @return the path to the folder we want to keep things on
     * @throws MisconfigurationException 
     */
    protected File getPersistingFolder(){
        if(persistingFolderCache != null){
            //Only ask once (cached after that).
            return persistingFolderCache;
        }
        
        File base;
        try {
            IPath stateLocation = AnalysisPlugin.getDefault().getStateLocation();
            base = stateLocation.toFile();
        } catch (NullPointerException e) {
            //it may fail in tests... (save it in default folder in this cases)
            Log.logInfo("Error getting persisting folder", e);
            base = new File(".");
        }
        File file = new File(
            base, 
            manager.getManagerRelatedName() + 
            "_"+ 
            StringUtils.getExeAsFileSystemValidPath(this.additionalInfoInterpreter)
        );
        
        if(!file.exists()){
            file.mkdirs();
        }
        persistingFolderCache = file;
        return file;
    }

    private File persistingLocation;
    
    @Override
    protected File getPersistingLocation() throws MisconfigurationException {
        if(persistingLocation == null){
            persistingLocation = new File(getPersistingFolder(), manager.getManagerRelatedName() + ".pydevsysteminfo");
        }
        return persistingLocation;
    }
    
    

    @Override
    protected void setAsDefaultInfo() {
        AdditionalSystemInterpreterInfo.setAdditionalSystemInfo(manager, this.additionalInfoInterpreter, this);
    }

    /**
     * @param interpreter 
     * @return whether the info was successfully loaded or not
     * @throws MisconfigurationException 
     */
    public static boolean loadAdditionalSystemInfo(IInterpreterManager manager, String interpreter) throws MisconfigurationException {
        AbstractAdditionalTokensInfo info = new AdditionalSystemInterpreterInfo(manager, interpreter);
        //when it is successfully loaded, it sets itself as the default (for its type)
        return info.load();
    }

    public static AbstractAdditionalDependencyInfo getAdditionalSystemInfo(
            IInterpreterManager manager, String interpreter) throws MisconfigurationException {
        return getAdditionalSystemInfo(manager, interpreter, false);
    }
    /**
     * @param m the module manager that we want to get info on (python, jython...)
     * @return the additional info for the system
     * @throws MisconfigurationException 
     */
    public static AbstractAdditionalDependencyInfo getAdditionalSystemInfo(
            IInterpreterManager manager, String interpreter, boolean errorIfNotAvailable) throws MisconfigurationException {
        Tuple<String,String> key = new Tuple<String, String>(manager.getManagerRelatedName(), interpreter);
        AbstractAdditionalDependencyInfo info = (AbstractAdditionalDependencyInfo) additionalSystemInfo.get(key);
        if(info == null){
            //temporary until it's loaded!
			return new AdditionalSystemInterpreterInfo(manager, interpreter);
        }
        return info;
    }

    /**
     * sets the additional info (overrides if already set)
     * @param manager the manager we want to set info on
     * @param additionalSystemInfoToSet the info to set
     */
    public static void setAdditionalSystemInfo(IInterpreterManager manager, String interpreter, 
            AbstractAdditionalTokensInfo additionalSystemInfoToSet) {
        
        additionalSystemInfo.put(new Tuple<String, String>(manager.getManagerRelatedName(), interpreter), 
                additionalSystemInfoToSet);
    }

    @Override
    public int hashCode() {
        return this.additionalInfoInterpreter.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof AdditionalSystemInterpreterInfo)){
            return false;
        }
        AdditionalSystemInterpreterInfo additionalSystemInterpreterInfo = (AdditionalSystemInterpreterInfo) obj;
        return this.additionalInfoInterpreter.equals(additionalSystemInterpreterInfo.additionalInfoInterpreter);
    }
}
