/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.StringUtils;

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
            fgColorManager = new ColorManager();
        }
        return fgColorManager;
    }

    /**
     * Cache for colors
     */
    protected Map<RGB, Color> fColorTable = new HashMap<RGB, Color>(10);

    public static final RGB dimBlack = new RGB(0, 0, 0);
    public static final RGB dimRed = new RGB(205, 0, 0);
    public static final RGB dimGreen = new RGB(0, 205, 0);
    public static final RGB dimYellow = new RGB(205, 205, 0);
    public static final RGB dimBlue = new RGB(0, 0, 238);
    public static final RGB dimMagenta = new RGB(205, 0, 205);
    public static final RGB dimCyan = new RGB(0, 205, 205);
    public static final RGB dimWhite = new RGB(229, 229, 229);

    public static final RGB brightBlack = new RGB(127, 127, 127);
    public static final RGB brightRed = new RGB(255, 0, 0);
    public static final RGB brightGreen = new RGB(0, 252, 0);
    public static final RGB brightYellow = new RGB(255, 255, 0);
    public static final RGB brightBlue = new RGB(0, 0, 252);
    public static final RGB brightMagenta = new RGB(255, 0, 255);
    public static final RGB brightCyan = new RGB(0, 255, 255);
    public static final RGB brightWhite = new RGB(255, 255, 255);

    /**
     * Receives a string such as:
     *
     * <ESC>[{attr1};...;{attrn}m
     *
     * Where {attr1}...{attrn} are numbers so that:
     *
     * Foreground Colors
     * 30  Black
     * 31  Red
     * 32  Green
     * 33  Yellow
     * 34  Blue
     * 35  Magenta
     * 36  Cyan
     * 37  White
     *
     * Background Colors
     * 40  Black
     * 41  Red
     * 42  Green
     * 43  Yellow
     * 44  Blue
     * 45  Magenta
     * 46  Cyan
     * 47  White
     *
     * If 0;30 is received, it means a 'dim' version of black, if 1;30 is received, it means a 'bright' version is used.
     *
     * If [0m is received, the attributes are reset (and null may be returned in this case).
     *
     * Reference: http://graphcomp.com/info/specs/ansi_col.html
     */
    public TextAttribute getAnsiTextAttribute(String str, TextAttribute prevAttribute, TextAttribute resetAttribute) {
        if (str.startsWith("[")) {
            str = str.substring(1);
        }
        int foundM = str.indexOf('m');
        if (foundM == -1) {
            return prevAttribute;
        }
        str = str.substring(0, foundM);

        if (str.equals("0")) {
            return resetAttribute;
        }

        boolean bright = false;
        Color foreground = null;
        Color background = null;

        List<String> split = StringUtils.split(str, ';');
        for (String string : split) {
            try {
                int parsed = Integer.parseInt(string);
                switch (parsed) {
                    case 0:
                        bright = false;
                        break;

                    case 1:
                        bright = true;
                        break;

                    case 30://  Black
                        foreground = getColor(bright ? brightBlack : dimBlack);
                        break;
                    case 31://  Red
                        foreground = getColor(bright ? brightRed : dimRed);
                        break;
                    case 32://  Green
                        foreground = getColor(bright ? brightGreen : dimGreen);
                        break;
                    case 33://  Yellow
                        foreground = getColor(bright ? brightYellow : dimYellow);
                        break;
                    case 34://  Blue
                        foreground = getColor(bright ? brightBlue : dimBlue);
                        break;
                    case 35://  Magenta
                        foreground = getColor(bright ? brightMagenta : dimMagenta);
                        break;
                    case 36://  Cyan
                        foreground = getColor(bright ? brightCyan : dimCyan);
                        break;
                    case 37://  White
                        foreground = getColor(bright ? brightWhite : dimWhite);
                        break;

                    case 40://  Black
                        background = getColor(bright ? brightBlack : dimBlack);
                        break;
                    case 41://  Red
                        background = getColor(bright ? brightRed : dimRed);
                        break;
                    case 42://  Green
                        background = getColor(bright ? brightGreen : dimGreen);
                        break;
                    case 43://  Yellow
                        background = getColor(bright ? brightYellow : dimYellow);
                        break;
                    case 44://  Blue
                        background = getColor(bright ? brightBlue : dimBlue);
                        break;
                    case 45://  Magenta
                        background = getColor(bright ? brightMagenta : dimMagenta);
                        break;
                    case 46://  Cyan
                        background = getColor(bright ? brightCyan : dimCyan);
                        break;
                    case 47://  White
                        background = getColor(bright ? brightWhite : dimWhite);
                        break;

                    default:
                        break;
                }
            } catch (NumberFormatException e) {
                //ignore
            }
        }

        return new TextAttribute(foreground != null ? foreground : prevAttribute.getForeground(),
                background != null ? background : prevAttribute.getBackground(), prevAttribute.getStyle());
    }

    public Color getColor(RGB rgb) {
        Display current = Display.getCurrent();
        if (current == null) {
            Log.log("Should not try to get color in a non-ui thread (it will fail if the color is not cached!)");
        }
        Color color = fColorTable.get(rgb);
        if (color == null) {
            color = new Color(current, rgb);
            fColorTable.put(rgb, color);
        }
        return color;
    }

    public void dispose() {
        Iterator<Color> e = fColorTable.values().iterator();
        while (e.hasNext()) {
            e.next().dispose();
        }
    }

    /**
     *
     * @param type: see constants at {@link PydevConsoleConstants}
     * @return a color to be used.
     */
    public Color getPreferenceColor(String type) {
        if (SharedCorePlugin.inTestMode()) {
            return null;
        }
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        IPreferenceStore preferenceStore = plugin.getPreferenceStore();
        return getColor(PreferenceConverter.getColor(preferenceStore, type));
    }

    //Note that to update the code below, the install.py of this plugin should be run.

    /*[[[cog
    import cog
    
    template = '''
    public TextAttribute get%sTextAttribute() {
        Color color = getPreferenceColor(PydevConsoleConstants.%s_COLOR);
        return new TextAttribute(color, null, 0);
    }'''
    
    for s in (
        'console_error', 'console_output', 'console_input', 'console_prompt'):
    
        cog.outl(template % (s.title().replace('_', ''), s.upper()))
    
    ]]]*/

    public TextAttribute getConsoleErrorTextAttribute() {
        Color color = getPreferenceColor(PydevConsoleConstants.CONSOLE_ERROR_COLOR);
        return new TextAttribute(color, null, 0);
    }

    public TextAttribute getConsoleOutputTextAttribute() {
        Color color = getPreferenceColor(PydevConsoleConstants.CONSOLE_OUTPUT_COLOR);
        return new TextAttribute(color, null, 0);
    }

    public TextAttribute getConsoleInputTextAttribute() {
        Color color = getPreferenceColor(PydevConsoleConstants.CONSOLE_INPUT_COLOR);
        return new TextAttribute(color, null, 0);
    }

    public TextAttribute getConsolePromptTextAttribute() {
        Color color = getPreferenceColor(PydevConsoleConstants.CONSOLE_PROMPT_COLOR);
        return new TextAttribute(color, null, 0);
    }

    //[[[end]]]

    public Color getConsoleBackgroundColor() {
        Color color = getPreferenceColor(PydevConsoleConstants.CONSOLE_BACKGROUND_COLOR);
        return color;
    }

    /**
     * Default background color for debug console is set to light gray so that
     * the user is able to quickly differentiate between a REPL window and the
     * existing console window
     *
     * @return
     */
    public Color getDebugConsoleBackgroundColor() {
        Color color = getPreferenceColor(PydevConsoleConstants.DEBUG_CONSOLE_BACKGROUND_COLOR);
        return color;
    }

    public TextAttribute getHyperlinkTextAttribute() {
        return null; //use default
    }

    public TextAttribute getForegroundTextAttribute() {
        return null; //use default
    }

}
