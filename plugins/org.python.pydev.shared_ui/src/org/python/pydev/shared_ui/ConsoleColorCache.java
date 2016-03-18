/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_ui.utils.RunInUiThread;

/**
 * The main use for this class is:
 * 
 * ConsoleColorCache.getDefault().keepConsoleColorsSynched(console);
 * 
 * It should be called when the console is created and whenever a new stream is created (to
 * set the color for that stream -- which must be registered in a map in the themeConsoleStreamToColor 
 * attribute).
 * 
 * @author Fabio
 */
@SuppressWarnings("deprecation")
public class ConsoleColorCache implements IPreferenceChangeListener {

    private static ConsoleColorCache instance;
    private static Object instanceLock = new Object();

    private List<WeakReference<IOConsole>> weakrefs = new ArrayList<WeakReference<IOConsole>>();
    private final Object referencesLock = new Object();

    private ConsoleColorCache() {
        IEclipsePreferences node = new InstanceScope().getNode("org.eclipse.debug.ui");
        node.addPreferenceChangeListener(this);
    }

    public static ConsoleColorCache getDefault() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new ConsoleColorCache();
                }
            }
        }
        return instance;
    }

    protected Map<RGB, Color> cache = new HashMap<RGB, Color>(4);

    public Color getColor(RGB rgb) {
        Color color = cache.get(rgb);
        if (color == null) {
            color = new Color(Display.getCurrent(), rgb);
            cache.put(rgb, color);
        }
        return color;
    }

    private Color getDebugColor(String key) {
        IEclipsePreferences node = new InstanceScope().getNode("org.eclipse.debug.ui");
        String color = node.get(key, null);
        if (color != null) {
            try {
                return getDefault().getColor(StringConverter.asRGB(color));
            } catch (Exception e) {
                Log.log(e);
            }
        }
        return null;
    }

    /**
     * In this method we'll make sure a console will have its colors properly updated.
     * 
     * @param console This is the console to keep updated. 
     * 
     * The console should have a getAttribute("themeConsoleStreamToColor") which returns a Map<IOConsoleOutputStream, String>
     * where the values may be: 
     * 
     * "console.output" or "console.error"
     */
    public void keepConsoleColorsSynched(IOConsole console) {
        updateConsole(console);
        addRef(console);
    }

    @SuppressWarnings({ "unchecked" })
    private void updateConsole(final IOConsole console) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                //Should be: DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_BAKGROUND_COLOR)
                //but we don't want to add that dependency.
                try {
                    Color color = getDebugColor("org.eclipse.debug.ui.consoleBackground");
                    if (color != null) {
                        console.setBackground(color);
                    }
                } catch (Exception e) {
                    Log.log(e);
                }

                try {
                    Map<IOConsoleOutputStream, String> streamToColor = (Map<IOConsoleOutputStream, String>) console
                            .getAttribute("themeConsoleStreamToColor");
                    if (streamToColor != null) {
                        Set<Entry<IOConsoleOutputStream, String>> entrySet = streamToColor.entrySet();
                        for (Entry<IOConsoleOutputStream, String> entry : entrySet) {
                            String value = entry.getValue();
                            if ("console.output".equals(value)) {
                                Color color = getDebugColor("org.eclipse.debug.ui.outColor");
                                if (color != null) {
                                    entry.getKey().setColor(color);
                                }

                            } else if ("console.error".equals(value)) {
                                Color color = getDebugColor("org.eclipse.debug.ui.errorColor");
                                if (color != null) {
                                    entry.getKey().setColor(color);
                                }

                            } else {
                                Log.log("Unrecognized value (expected console.output or console.error):" + value);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        };
        RunInUiThread.async(r, true);
    }

    private void addRef(IOConsole console) {
        synchronized (referencesLock) {
            //We'll clear the current references and add the new one if it's not there already.
            int size = weakrefs.size();
            for (int i = 0; i < size; i++) {
                WeakReference<IOConsole> ref = weakrefs.get(i);
                Object object = ref.get();
                if (object == console) {
                    return; //already there (nothing to add).
                }
                if (object == null) {
                    weakrefs.remove(i);
                    i--;
                    size--;
                }
            }
            //Add the new reference.
            weakrefs.add(new WeakReference<IOConsole>(console));
        }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        String key = event.getKey();
        if ("org.eclipse.debug.ui.consoleBackground".equals(key) || "org.eclipse.debug.ui.outColor".equals(key)
                || "org.eclipse.debug.ui.errorColor".equals(key)) {
            synchronized (referencesLock) {
                ArrayList<IOConsole> currentRefs = getCurrentRefs();
                for (IOConsole console : currentRefs) {
                    updateConsole(console);
                }
            }
        }
    }

    private ArrayList<IOConsole> getCurrentRefs() {
        int size = weakrefs.size();
        ArrayList<IOConsole> currentRefs = new ArrayList<IOConsole>(size);
        for (int i = 0; i < size; i++) {
            WeakReference<IOConsole> ref = weakrefs.get(i);
            IOConsole object = ref.get();
            if (object == null) {
                weakrefs.remove(i);
                i--;
                size--;
            } else {
                currentRefs.add(object);
            }
        }
        return currentRefs;
    }

}
