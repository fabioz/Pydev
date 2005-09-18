/*
 * License: Common Public License v1.0
 * Created on Sep 13, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.analysis.additionalinfo;

import java.util.HashMap;
import java.util.Map;

import org.python.pydev.ui.interpreters.IInterpreterManager;


public class AdditionalSystemInterpreterInfo extends AbstractAdditionalInterpreterInfo{

    private IInterpreterManager manager;
    /**
     * holds system info (interpreter name points to system info)
     */
    private static Map<String, AbstractAdditionalInterpreterInfo> additionalSystemInfo = new HashMap<String, AbstractAdditionalInterpreterInfo>();

    public AdditionalSystemInterpreterInfo(IInterpreterManager manager) {
        this.manager = manager;
    }

    @Override
    protected String getPersistingLocation() {
        return getPersistingFolder()+manager.getManagerRelatedName()+".pydevsysteminfo";
    }

    @Override
    protected void setAsDefaultInfo() {
        AdditionalSystemInterpreterInfo.setAdditionalSystemInfo(manager, this);
    }

    /**
     * @return whether the info was succesfully loaded or not
     */
    public static boolean loadAdditionalSystemInfo(IInterpreterManager manager) {
        AbstractAdditionalInterpreterInfo info = new AdditionalSystemInterpreterInfo(manager);
        //when it is successfully loaded, it sets itself as the default (for its type)
        return info.load();
    }

    /**
     * @param m the module manager that we want to get info on (python, jython...)
     * @return the additional info for the system
     */
    public static AbstractAdditionalInterpreterInfo getAdditionalSystemInfo(IInterpreterManager manager) {
        String key = manager.getManagerRelatedName();
        AbstractAdditionalInterpreterInfo info = additionalSystemInfo.get(key);
        if(info == null){
            info = new AdditionalSystemInterpreterInfo(manager);
            additionalSystemInfo.put(key, info);
        }
        return info;
    }

    /**
     * sets the additional info (overrides if already set)
     * @param manager the manager we want to set info on
     * @param additionalSystemInfoToSet the info to set
     */
    public static void setAdditionalSystemInfo(IInterpreterManager manager, AbstractAdditionalInterpreterInfo additionalSystemInfoToSet) {
        additionalSystemInfo.put(manager.getManagerRelatedName(), additionalSystemInfoToSet);
    }

}
