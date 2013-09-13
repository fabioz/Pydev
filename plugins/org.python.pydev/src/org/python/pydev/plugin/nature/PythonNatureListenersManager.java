/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 29, 2006
 * @author Fabio
 */
package org.python.pydev.plugin.nature;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;


/**
 * This class is used to pass notifications about the python nature around for 
 * those interested.
 * 
 * @author Fabio
 */
public class PythonNatureListenersManager {
    private static List<WeakReference<IPythonNatureListener>> pythonNatureListeners = new ArrayList<WeakReference<IPythonNatureListener>>();

    public static void addPythonNatureListener(IPythonNatureListener listener) {
        pythonNatureListeners.add(new WeakReference<IPythonNatureListener>(listener));
    }

    public static void removePythonNatureListener(IPythonNatureListener provider) {
        for (Iterator<WeakReference<IPythonNatureListener>> it = pythonNatureListeners.iterator(); it.hasNext();) {
            WeakReference<IPythonNatureListener> ref = it.next();
            if (ref.get() == provider) {
                it.remove();
            }
        }
    }

    /**
     * Notification that the pythonpath has been rebuilt.
     * 
     * @param project is the project that had the pythonpath rebuilt
     * @param nature the nature related to the project (can be null if the nature has actually been removed)
     */
    public static void notifyPythonPathRebuilt(IProject project, IPythonNature nature) {
        for (Iterator<WeakReference<IPythonNatureListener>> it = pythonNatureListeners.iterator(); it.hasNext();) {
            WeakReference<IPythonNatureListener> ref = it.next();
            try {
                IPythonNatureListener listener = ref.get();
                if (listener == null) {
                    it.remove();
                } else {
                    listener.notifyPythonPathRebuilt(project, nature);
                }
            } catch (Throwable e) {
                Log.log(e);
            }
        }
    }

}
