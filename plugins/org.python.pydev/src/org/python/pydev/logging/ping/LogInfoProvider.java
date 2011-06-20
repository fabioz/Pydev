/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.logging.ping;

import java.util.UUID;
import java.util.prefs.Preferences;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;

public class LogInfoProvider implements ILogPingProvider {
	
	public static final String TESTING_ID = "00000000-0000-0000-0000-000000000000";

	
	public long getCurrentTime() {
		return System.currentTimeMillis();
	}
	

	public String getApplicationId() {
		IPreferencesService preferencesService = Platform.getPreferencesService();
		if(preferencesService == null){
			return TESTING_ID;
		}
		//Note: this is the same qualifier/id used by aptana plugins (so that we don't generate
		//a new one if there's already one registered).
		String qualifier = "com.aptana.db"; 
		String key = "ide-id";
		String id = preferencesService.getString(qualifier, key, null, null);

		if (id == null) {
		    Preferences node = null;
		    String keyNodeInPrefs = qualifier+"."+key;
		    String keySettingInPrefs = "UUID";
		    
		    try {
		        node = Preferences.userRoot().node(keyNodeInPrefs);
                id = node.get(keySettingInPrefs, "");
            } catch (Exception e1) {
                Log.log(e1);
            }
            IPreferenceStore preferenceStore = PydevPlugin.getDefault().getPreferenceStore();
            
		    if(id == null || id.length() == 0){
    			id = preferenceStore.getString(keyNodeInPrefs+keySettingInPrefs);
		    }
		    
		    if(id == null || id.length() == 0){
		        id = UUID.randomUUID().toString();
		    }
		    

		    //if we got here it was not initially found, so, save it in all locations we look for!
		    preferenceStore.putValue(keyNodeInPrefs+keySettingInPrefs, id);
		    
		    if(node != null){
		        try {
		            node.put(keySettingInPrefs, id);
		        } catch (Exception e) {
		            Log.log(e);
		        }
		    }
		    
		    // saves the id in configuration scope so it's shared by all workspaces
		    IEclipsePreferences prefs = (new ConfigurationScope()).getNode(qualifier);
		    prefs.put(key, id);
		    try {
		        prefs.flush();
		    } catch (BackingStoreException e) {
		        Log.log(e);
		    }
		}
		return id;
	}
	
}
