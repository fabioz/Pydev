/*
 * author: atotic
 * date: 7/8/03
 * IBM's wizard code
 */ 
package org.python.pydev.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.DataFormatException;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.plugin.PydevPlugin;

/**
 * ColorCache gets colors by RGB, or name
 * Named colors are retrieved from preferences
 * 
 * It would be nice if color cache listened to preference changes
 * and modified its colors when prefs changed. But currently colors are
 * immutable, so this can't be done
        implements Preferences.IPropertyChangeListener 
		preferences.addPropertyChangeListener(this);
		preferences.removePropertyChangeListener(this);
*/

public class ColorCache {

	private Map fColorTable = new HashMap(10);
	private Map fNamedColorTable = new HashMap(10);
	private IPreferenceStore preferences;
	
	
	public ColorCache(IPreferenceStore prefs) {
	    preferences = prefs;
	}
	
	public void dispose() {
		Iterator e = fColorTable.values().iterator();
		while (e.hasNext())
			 ((Color) e.next()).dispose();
		e = fNamedColorTable.values().iterator();
		while (e.hasNext())
			((Color) e.next()).dispose();
	}
	
	public Color getColor(RGB rgb) {
		Color color = (Color) fColorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			fColorTable.put(rgb, color);
		}
		return color;
	}
	
	// getNamedColor gets color from preferences
	// if prefernce is not found, then it looks whether color is one
	// of the well-known predefined names
	public Color getNamedColor(String name) {
		Color color = (Color)fNamedColorTable.get(name);
		if (color == null) {
			String colorCode =  preferences.getString(name);
			if (colorCode.length() == 0) {
				if (name.equals("RED")) {
					color = getColor(new RGB(255, 0, 0));
				}
				else if (name.equals("BLACK")) {
					color = getColor(new RGB(0,0,0));
				}
				else {
					PydevPlugin.log("Unknown color:" + name);
					color = getColor(new RGB(255,0,0));
				}
			}
			else {
				try {
					RGB rgb = StringConverter.asRGB(colorCode);
					color = new Color(Display.getCurrent(), rgb);
					fNamedColorTable.put(name, color);
				}
				catch (DataFormatException e) {
					// Data conversion failure, maybe someone edited our prefs by hand
					PydevPlugin.log(e);
					color = new Color(Display.getCurrent(), new RGB(255, 50, 0));
				}
			}
		}
		return color;
	}
	
	//reloads the specified color from preferences
	public void reloadNamedColor(String name)
	{
		if( fNamedColorTable.containsKey(name) ) {
			//UndisposedColors.add(fNamedColorTable.get(name));
			((Color)fNamedColorTable.get(name)).dispose();
			fNamedColorTable.remove(name);
		}
	}
}
