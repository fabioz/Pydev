package org.python.pydev.debug.newconsole.prefs;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.plugin.preferences.IPydevPreferencesProvider;

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
    
    private Color getColor(RGB rgb) {
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
    private Color getPreferenceColor(String type) {
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        if(plugin == null){
            return null;
        }
        IPreferenceStore preferenceStore = plugin.getPreferenceStore();
        return getColor(PreferenceConverter.getColor(preferenceStore, type));
    }

    
    
    //Note that to update the code below, the install.py of this plugin should be run.
    
    /*[[[cog
    import cog
    
    template = '''
    @SuppressWarnings("unchecked")
    public TextAttribute get%sTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.get%sTextAttribute();
            if(textAttribute != null){
                return textAttribute;
            }
        }
        Color color = getPreferenceColor(PydevConsoleConstants.%s_COLOR);
        return new TextAttribute(color, null, 0);
    }'''
    
    for s in (
        'console_error', 'console_output', 'console_input', 'console_prompt'):
        
        cog.outl(template % (s.title().replace('_', ''), s.title().replace('_', ''), s.upper()))

    ]]]*/

    @SuppressWarnings("unchecked")
    public TextAttribute getConsoleErrorTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getConsoleErrorTextAttribute();
            if(textAttribute != null){
                return textAttribute;
            }
        }
        Color color = getPreferenceColor(PydevConsoleConstants.CONSOLE_ERROR_COLOR);
        return new TextAttribute(color, null, 0);
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getConsoleOutputTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getConsoleOutputTextAttribute();
            if(textAttribute != null){
                return textAttribute;
            }
        }
        Color color = getPreferenceColor(PydevConsoleConstants.CONSOLE_OUTPUT_COLOR);
        return new TextAttribute(color, null, 0);
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getConsoleInputTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getConsoleInputTextAttribute();
            if(textAttribute != null){
                return textAttribute;
            }
        }
        Color color = getPreferenceColor(PydevConsoleConstants.CONSOLE_INPUT_COLOR);
        return new TextAttribute(color, null, 0);
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getConsolePromptTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getConsolePromptTextAttribute();
            if(textAttribute != null){
                return textAttribute;
            }
        }
        Color color = getPreferenceColor(PydevConsoleConstants.CONSOLE_PROMPT_COLOR);
        return new TextAttribute(color, null, 0);
    }
    //[[[end]]]
    
    @SuppressWarnings("unchecked")
    public Color getConsoleBackgroundColor() {
    	List<IPydevPreferencesProvider> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
    	for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
    		RGB textAttribute = iPydevPreferencesProvider.getConsoleBackgroundRGB();
    		if(textAttribute != null){
    			return getColor(textAttribute);
    		}
    	}
    	Color color = getPreferenceColor(PydevConsoleConstants.CONSOLE_BACKGROUND_COLOR);
    	return color;
    }
    
    @SuppressWarnings("unchecked")
    public TextAttribute getHyperlinkTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getHyperlinkTextAttribute();
            if(textAttribute != null){
                return textAttribute;
            }
        }
        return null; //use default
    }
    
    @SuppressWarnings("unchecked")
    public TextAttribute getForegroundTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getCodeTextAttribute();
            if(textAttribute != null){
                return textAttribute;
            }
        }
        return null; //use default
    }
}


