package org.python.pydev.debug.newconsole.prefs;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;

/**
 * Generic color manager.
 */
public class ColorManager {    
    
    private static ColorManager fgColorManager;
    
    /**
     * Singleton
     */
    private ColorManager() {
    }
    
    public static ColorManager getDefault() {
        if (fgColorManager == null) {
            fgColorManager= new ColorManager();
        }
        return fgColorManager;
    }
    
    /**
     * Cache for colors
     */
    protected Map<RGB, Color> fColorTable= new HashMap<RGB, Color>(10);
    
    public Color getColor(RGB rgb) {
        Color color= fColorTable.get(rgb);
        if (color == null) {
            color= new Color(Display.getCurrent(), rgb);
            fColorTable.put(rgb, color);
        }
        return color;
    }
    
    public void dispose() {
        Iterator<Color> e= fColorTable.values().iterator();
        while (e.hasNext())
            e.next().dispose();
    }
    
    /**
     * 
     * @param type: see constants at {@link PydevConsoleConstants}
     * @return a color to be used.
     */
    public static Color getPreferenceColor(String type) {
        IPreferenceStore preferenceStore = PydevDebugPlugin.getDefault().getPreferenceStore();
        return ColorManager.getDefault().getColor(PreferenceConverter.getColor(preferenceStore, type));
    }

}


