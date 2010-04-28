package org.python.pydev.logging.ping;

import java.util.UUID;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.osgi.service.prefs.BackingStoreException;
import org.python.pydev.core.log.Log;

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
			id = UUID.randomUUID().toString();
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
