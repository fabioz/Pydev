/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * author: atotic
 * date: 7/8/03
 * IBM's wizard code
 */
package org.python.pydev.shared_ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.DataFormatException;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.shared_core.log.Log;

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
public abstract class ColorCache {

    private final Map<RGB, Color> fColorTable = new HashMap<RGB, Color>();
    protected final Map<String, Color> fNamedColorTable = new HashMap<String, Color>();
    protected IPreferenceStore preferences;

    public ColorCache(IPreferenceStore prefs) {
        preferences = prefs;
    }

    public void dispose() {
        Iterator<Color> it = fColorTable.values().iterator();
        while (it.hasNext()) {
            it.next().dispose();
        }

        it = fNamedColorTable.values().iterator();

        while (it.hasNext()) {
            it.next().dispose();
        }

        fColorTable.clear();
        fNamedColorTable.clear();
    }

    public Color getColor(String name) {
        return getNamedColor(name);
    }

    public Color getColor(RGB rgb) {
        Color color = fColorTable.get(rgb);
        if (color == null || color.isDisposed()) {
            color = new Color(Display.getCurrent(), rgb);
            fColorTable.put(rgb, color);
        }
        return color;
    }

    // getNamedColor gets color from preferences
    // if preference is not found, then it looks whether color is one
    // of the well-known predefined names
    protected Color getNamedColor(String name) {
        Color color = fNamedColorTable.get(name);
        if (color == null || color.isDisposed()) {
            String colorCode = preferences != null ? preferences.getString(name) : "";
            if (colorCode.length() == 0) {
                if (name.equals("RED")) {
                    color = getColor(new RGB(255, 0, 0));

                } else if (name.equals("BLACK")) {
                    color = getColor(new RGB(0, 0, 0));

                } else if (name.equals("WHITE")) {
                    color = getColor(new RGB(255, 255, 255));

                } else {
                    Log.log("Unknown color:" + name);
                    color = getColor(new RGB(255, 0, 0));
                }
            } else {
                try {
                    RGB rgb = StringConverter.asRGB(colorCode);
                    color = new Color(Display.getCurrent(), rgb);
                    fNamedColorTable.put(name, color);
                } catch (DataFormatException e) {
                    // Data conversion failure, maybe someone edited our prefs by hand
                    Log.log(e);
                    color = getColor(new RGB(255, 50, 0));
                }
            }
        }
        return color;
    }

    //reloads the specified color from preferences
    public void reloadProperty(String name) {
        if (fNamedColorTable.containsKey(name)) {
            //UndisposedColors.add(fNamedColorTable.get(name));
            Color color = fNamedColorTable.remove(name);
            color.dispose();
        }
    }

    /**
     * When new preferences are set, the contents of the cache are cleared.
     */
    public void setPreferences(IPreferenceStore prefs) {
        this.dispose();
        this.preferences = prefs;
    }
}
