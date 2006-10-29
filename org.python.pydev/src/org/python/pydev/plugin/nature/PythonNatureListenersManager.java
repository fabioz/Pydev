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
import org.python.pydev.plugin.PydevPlugin;

/**
 * This class is used to pass notifications about the python nature around for 
 * those interested.
 * 
 * @author Fabio
 */
public class PythonNatureListenersManager {
    private static List<WeakReference<IPythonNatureListener>> pythonNatureListeners = new ArrayList<WeakReference<IPythonNatureListener>>();
    
    public static void addPythonNatureListener(IPythonNatureListener listener){
        pythonNatureListeners.add(new WeakReference<IPythonNatureListener>(listener));
    }
    
    /**
     * Notification that the pythonpath has been rebuilt.
     * 
     * @param project is the project that had the pythonpath rebuilt
     * @param projectPythonpath the project pythonpath used when rebuilding
     */
    public static void notifyPythonPathRebuilt(IProject project, List<String> projectPythonpath){
        for(Iterator<WeakReference<IPythonNatureListener>> it = pythonNatureListeners.iterator();it.hasNext();){
            WeakReference<IPythonNatureListener> ref = it.next();
            try{
                IPythonNatureListener listener = ref.get();
                if(listener == null){
                    it.remove();
                }else{
                    listener.notifyPythonPathRebuilt(project, projectPythonpath);
                }
            }catch(Throwable e){
                PydevPlugin.log(e);
            }
        }
    }
    
    
}
