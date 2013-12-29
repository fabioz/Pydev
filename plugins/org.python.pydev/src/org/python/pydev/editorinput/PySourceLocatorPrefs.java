/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editorinput;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Class used to deal with the source locator prefs (even though they're edited in the SourceLocatorPrefsPage that's in
 * org.python.pydev.debug
 * 
 * @author Fabio
 */
public class PySourceLocatorPrefs {

    /**
     * Constant used to define that a path should not be asked for (so, the translation will return nothing and
     * the user won't be bothered by that)
     */
    public static final String DONTASK = "DONTASK";

    public static final String ON_SOURCE_NOT_FOUND = "ON_SOURCE_NOT_FOUND";
    public static final String FILE_CONTENTS_TIMEOUT = "FILE_CONTENTS_TIMEOUT";
    public static final int DEFAULT_FILE_CONTENTS_TIMEOUT = 1500;

    public static final int ASK_FOR_FILE = 0;
    public static final int ASK_FOR_FILE_GET_FROM_SERVER = 1;
    public static final int GET_FROM_SERVER = 2;

    public static final int DEFAULT_ON_FILE_NOT_FOUND_IN_DEBUGGER = ASK_FOR_FILE_GET_FROM_SERVER;

    /**
     * Checks if a translation path passed is valid.
     * 
     * @param translation the translation path entered by the user
     * @return null if it's valid or the error message to be shown to the user
     */
    public static String isValid(String[] translation) {
        if (translation.length != 2) {
            return "Input must have 2 elements.";
        }

        if (translation[1].equals(DONTASK)) {
            return null;
        }
        if (!new File(translation[1]).exists()) {
            return StringUtils.format(
                    "The file: %s does not exist and doesn't match 'DONTASK'.", translation[1]);
        }

        return null;
    }

    /**
     * @see #addPathTranslation(String) -- with toOSString for each path and a comma separator
     */
    public static void addPathTranslation(IPath path, IPath location) {
        addPathTranslation(new String[] { path.toOSString(), location.toOSString() });
    }

    /**
     * Any request to the passed path translation will be ignored.
     * @param path the path that should have the translation ignored (silently)
     */
    public static void setIgnorePathTranslation(IPath path) {
        addPathTranslation(new String[] { path.toOSString(), DONTASK });
    }

    /**
     * Adds a path to the translation table.
     * 
     * @param translation the translation path to be added. 
     * E.g.: 
     * path asked, new path -- means that a request for the "path asked" should return the "new path"
     * path asked, DONTASK -- means that if some request for that file was asked it should silently ignore it
     * 
     * E.g.: 
     * c:\foo\c.py,c:\temp\c.py
     * c:\foo\c.py,DONTASK
     */
    private static void addPathTranslation(String[] translation) {
        String valid = isValid(translation);
        if (valid != null) {
            throw new RuntimeException(valid);
        }
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        String available = store.getString(PydevEditorPrefs.SOURCE_LOCATION_PATHS);

        if (available == null || available.trim().length() == 0) {
            available = StringUtils.join(",", translation);
        } else {
            String pathAsked = translation[0].trim();

            String existent = getPathTranslation(pathAsked);
            if (existent != null) {
                List<String> splitted = StringUtils.splitAndRemoveEmptyTrimmed(available, '\n');
                final int size = splitted.size();
                for (int i = 0; i < size; i++) {
                    String s = splitted.get(i);
                    String initialPart = StringUtils.splitAndRemoveEmptyTrimmed(s, ',').get(0).trim();
                    if (initialPart.equals(pathAsked)) {
                        splitted.set(i, StringUtils.join(",", translation));
                        break;
                    }
                }
                available = StringUtils.join("\n", splitted);
            } else {
                available += "\n";
                available += StringUtils.join(",", translation);
            }
        }
        store.putValue(PydevEditorPrefs.SOURCE_LOCATION_PATHS, available);
    }

    /**
     * @see #getPathTranslation(String) -- with toOSString from path.
     */
    public static String getPathTranslation(IPath pathToTranslate) {
        return getPathTranslation(pathToTranslate.toOSString());
    }

    /**
     * Translates a path given the current translation settings
     * 
     * @param pathToTranslate the path to be translated
     * @return the translated path or DONTASK or null if no translation path was found for it
     */
    public static String getPathTranslation(String pathToTranslate) {
        pathToTranslate = pathToTranslate.trim();
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        String available = store.getString(PydevEditorPrefs.SOURCE_LOCATION_PATHS);
        if (available == null || available.trim().length() == 0) {
            return null; //nothing available
        } else {
            for (String string : StringUtils.splitAndRemoveEmptyTrimmed(available, '\n')) {
                List<String> translation = StringUtils.splitAndRemoveEmptyTrimmed(string, ',');
                if (translation.size() == 2) {
                    if (translation.get(0).trim().equals(pathToTranslate)) {
                        return translation.get(1).trim();
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param words words to be gotten as string
     * @return a string with all the passed words separated by '\n'
     */
    public static String wordsAsString(List<String[]> words) {
        StringBuffer buf = new StringBuffer();
        for (String[] string : words) {
            buf.append(string[0].trim());
            buf.append(',');
            buf.append(string[1].trim());
            buf.append('\n');
        }
        return buf.toString();
    }

    /**
     * @param string the string that has to be returned as a list of strings
     * @return an array of strings from the passed string (reverse logic from wordsAsString)
     */
    public static List<String[]> stringAsWords(String string) {
        ArrayList<String[]> strs = new ArrayList<String[]>();
        for (String str : StringUtils.splitAndRemoveEmptyTrimmed(string, '\n')) {
            final List<String> temp = StringUtils.splitAndRemoveEmptyTrimmed(str, ',');
            strs.add(temp.toArray(new String[temp.size()]));
        }
        return strs;
    }

    public static int getOnSourceNotFound() {
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        int onSourceNotFound = store.getInt(ON_SOURCE_NOT_FOUND);

        //Make sure that it's a valid value.
        if (onSourceNotFound < ASK_FOR_FILE || onSourceNotFound > GET_FROM_SERVER) {
            onSourceNotFound = DEFAULT_ON_FILE_NOT_FOUND_IN_DEBUGGER;
        }
        return onSourceNotFound;
    }

    public static int getFileContentsTimeout() {
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        int timeout = store.getInt(FILE_CONTENTS_TIMEOUT);
        if (timeout < 1000) {
            //Always let at least 1 sec timeout.
            timeout = 1000;
        }
        return timeout;
    }

}
