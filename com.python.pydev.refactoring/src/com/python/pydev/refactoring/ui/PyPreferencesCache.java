package com.python.pydev.refactoring.ui;

import java.util.HashMap;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public class PyPreferencesCache implements IPropertyChangeListener {

	private IPreferenceStore preferenceStore;
	private HashMap<String, Boolean> cache = new HashMap<String, Boolean>();
	
	public PyPreferencesCache(IPreferenceStore preferenceStore) {
		this.preferenceStore = preferenceStore;
		this.preferenceStore.addPropertyChangeListener(this);
	}

	public boolean getBoolean(String key) {
		Boolean b = cache.get(key);
		if(b == null){
			b = this.preferenceStore.getBoolean(key);
			cache.put(key, b);
		}
		return b;
	}

	public void propertyChange(PropertyChangeEvent event) {
		final Object newValue = event.getNewValue();
		if(newValue == null || newValue instanceof Boolean){
			cache.put(event.getProperty(), (Boolean) newValue);
		}else{
			throw new RuntimeException("Can currently only deal with booleans.");
		}
	}

}
