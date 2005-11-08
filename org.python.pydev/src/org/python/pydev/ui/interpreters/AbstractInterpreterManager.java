/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.interpreters;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.NotConfiguredInterpreterException;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;



/**
 * Does not write directly in INTERPRETER_PATH, just loads from it and works with it.
 * 
 * @author Fabio Zadrozny
 */
public abstract class AbstractInterpreterManager implements IInterpreterManager {

    /**
     * This is the cache, that points from an interpreter to its information.
     */
    private Map<String, InterpreterInfo> exeToInfo = new HashMap<String, InterpreterInfo>();
    private Preferences prefs;

    /**
     * Constructor
     */
    public AbstractInterpreterManager(Preferences prefs) {
        this.prefs = prefs;
        prefs.setDefault(getPreferenceName(), "");
        List<IInterpreterObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
        for (IInterpreterObserver observer : participants) {
            observer.notifyInterpreterManagerRecreated(this);
        }
    }

    /**
     * @return the preference name where the options for this interpreter manager should be stored
     */
    protected abstract String getPreferenceName();
    
    /**
     * @throws NotConfiguredInterpreterException
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#getDefaultInterpreter()
     */
    public String getDefaultInterpreter() throws NotConfiguredInterpreterException {
        String[] interpreters = getInterpreters();
        if(interpreters.length > 0){
            String interpreter = interpreters[0];
            if(interpreter == null){
                throw new NotConfiguredInterpreterException("The configured interpreter is null, some error happened getting it.\n" +getNotConfiguredInterpreterMsg());
            }
            return interpreter;
        }else{
            throw new NotConfiguredInterpreterException(getNotConfiguredInterpreterMsg());
        }
    }

    public void clearAllBut(List<String> allButTheseInterpreters) {
        synchronized(exeToInfo){
            ArrayList toRemove = new ArrayList();
            for (String interpreter : exeToInfo.keySet()) {
                if(!allButTheseInterpreters.contains(interpreter)){
                    toRemove.add(interpreter);
                }
            }
            //we do not want to remove it while we are iterating...
            for (Object object : toRemove) {
                exeToInfo.remove(object);
            }
        }
    }
    
    /**
     * @return a message to show to the user when there is no configured interpreter
     */
    protected abstract String getNotConfiguredInterpreterMsg(); 

    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#getInterpreters()
     */
    public String[] getInterpreters() {
        return getInterpretersFromPersistedString(prefs.getString(getPreferenceName()));
    }
    
    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#getDefaultInterpreterInfo(org.eclipse.core.runtime.IProgressMonitor)
     */
    public InterpreterInfo getDefaultInterpreterInfo(IProgressMonitor monitor) {
        String interpreter = getDefaultInterpreter();
        return getInterpreterInfo(interpreter, monitor);
    }
    
    /**
     * Given an executable, should create the interpreter info that corresponds to it
     * 
     * @param executable the executable that should be used to create the info
     * @param monitor a monitor to keep track of the info
     * 
     * @return the interpreter info for the executable
     * @throws CoreException 
     */
    public abstract InterpreterInfo createInterpreterInfo(String executable, IProgressMonitor monitor) throws CoreException;
    
    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#getInterpreterInfo(java.lang.String)
     */
    public InterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor) {
        InterpreterInfo info = (InterpreterInfo) exeToInfo.get(executable);
        if(info == null){
            monitor.worked(5);
            //ok, we have to get the info from the executable (and let's cache results for future use...
    		try {

                info = createInterpreterInfo(executable, monitor);
                
    	    } catch (Exception e) {
    	        PydevPlugin.log(e);
    	        throw new RuntimeException(e);
    	    }
    	    if(info.executableOrJar != null && info.executableOrJar.trim().length() > 0){
    	        exeToInfo.put(info.executableOrJar, info);
    	        
    	    }else{ //it is null or no empty
                String title = "Invalid interpreter:"+executable;
                String msg = "Unable to get information on interpreter!";
    	        String reason = "The interpreter (or jar): '"+executable+"' is not valid - info.executable found: "+info.executableOrJar;
    	        
                try {
                    ErrorDialog.openError(null, title, msg, new Status(Status.ERROR, PydevPlugin.getPluginID(), 0, reason, null));
                } catch (Error e) {
                    // ignore error comunication error
                }
    	        throw new RuntimeException(reason);
    	    }
        }
        return info;
    }

    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#addInterpreter(java.lang.String)
     */
    public String addInterpreter(String executable, IProgressMonitor monitor) {
        InterpreterInfo info = getInterpreterInfo(executable, monitor);
        return info.executableOrJar;
    }

    //little cache...
    private String persistedCache;
    private String [] persistedCacheRet;
    
    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#getInterpretersFromPersistedString(java.lang.String)
     */
    public String[] getInterpretersFromPersistedString(String persisted) {

        if(persisted == null || persisted.trim().length() == 0){
            return new String[0];
        }
        
        if(persistedCache == null || persistedCache.equals(persisted) == false){
	        List<String> ret = new ArrayList<String>();

	        try {
		        List list = (List) REF.getStrAsObj(persisted, new ICallback<Object, ObjectInputStream>(){

					public Object call(ObjectInputStream arg) {
		                try {
		                    return arg.readObject();
		                } catch (IOException e) {
		                    throw new RuntimeException(e);
		                } catch (ClassNotFoundException e) {
		                    throw new RuntimeException(e);
		                }
					}});
	            
	            for (Iterator iter = list.iterator(); iter.hasNext();) {
	                InterpreterInfo info = (InterpreterInfo) iter.next();
                    if(info != null && info.executableOrJar != null){
    	                this.exeToInfo.put(info.executableOrJar, info);
    	                ret.add(info.executableOrJar);
                    }
	            }
            } catch (Exception e) {
                PydevPlugin.log(e);
                
                //ok, some error happened (maybe it's not configured)
                return new String[0];
            }
            
	        persistedCache = persisted;
	        persistedCacheRet = ret.toArray(new String[0]);
        }
        return persistedCacheRet;
    }

    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#getStringToPersist(java.lang.String[])
     */
    public String getStringToPersist(String[] executables) {
        ArrayList<InterpreterInfo> list = new ArrayList<InterpreterInfo>();
        for (String exe : executables) {
            InterpreterInfo info = this.exeToInfo.get(exe);
            if(info!=null){
                list.add(info);
            }
        }
        return REF.getObjAsStr(list);
    }


    /**
     * @return whether this interpreter manager can be used to get info on the specified nature
     */
    public abstract boolean canGetInfoOnNature(IPythonNature nature);
    
    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#hasInfoOnDefaultInterpreter(IPythonNature)
     */
    public boolean hasInfoOnDefaultInterpreter(IPythonNature nature) {
        if(!canGetInfoOnNature(nature)){
            throw new RuntimeException("Cannot get info on the requested nature");
        }
        
        try {
            InterpreterInfo info = (InterpreterInfo) exeToInfo.get(getDefaultInterpreter());
            return info != null;
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#restorePythopathFor(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void restorePythopathFor(String defaultSelectedInterpreter, IProgressMonitor monitor) {
        final InterpreterInfo info = getInterpreterInfo(defaultSelectedInterpreter, monitor);
        info.restorePythonpath(monitor); //that's it, info.modulesManager contains the SystemModulesManager

        List<IInterpreterObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
        for (IInterpreterObserver observer : participants) {
            observer.notifyDefaultPythonpathRestored(this, monitor);
        }
        
    }
}

