/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.interpreters;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.NotConfiguredInterpreterException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.core.uiutils.AsynchronousProgressMonitorDialog;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.nature.PythonNatureListenersManager;
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
    protected Map<String, InterpreterInfo> exeToInfo = new HashMap<String, InterpreterInfo>();
    private Preferences prefs;
    private String[] interpretersFromPersistedString;
    private IInterpreterInfo[] interpreterInfosFromPersistedString;
    

    //caches that are filled at runtime -------------------------------------------------------------------------------
    /**
     * This is used to keep the builtin completions
     */
    protected final Map<String, IToken[]> builtinCompletions = new HashMap<String, IToken[]>();
    
    /**
     * This is used to keep the builtin module
     */
    protected final Map<String, IModule> builtinMod = new HashMap<String, IModule>();

    public void clearBuiltinCompletions(String projectInterpreterName) {
        this.builtinCompletions.remove(projectInterpreterName);
    }

    public IToken[] getBuiltinCompletions(String projectInterpreterName) {
        //Cache with the internal name.
        projectInterpreterName = getInternalName(projectInterpreterName);
        if(projectInterpreterName == null){
            return null;
        }

        IToken[] toks = this.builtinCompletions.get(projectInterpreterName);
        
        if(toks == null || toks.length == 0){
            IModule builtMod = getBuiltinMod(projectInterpreterName);
            if(builtMod != null){
                toks = builtMod.getGlobalTokens();
                this.builtinCompletions.put(projectInterpreterName, toks);
            }
        }
        return this.builtinCompletions.get(projectInterpreterName);
    }

    public IModule getBuiltinMod(String projectInterpreterName){
        //Cache with the internal name.
        projectInterpreterName = getInternalName(projectInterpreterName);
        if(projectInterpreterName == null){
            return null;
        }
        IModule mod = builtinMod.get(projectInterpreterName);
        if(mod != null){
            return mod;
        }
        
        try {
            InterpreterInfo interpreterInfo = this.getInterpreterInfo(projectInterpreterName, null);
            ISystemModulesManager modulesManager = interpreterInfo.getModulesManager();
            
            mod = modulesManager.getBuiltinModule("__builtin__", false);
            if(mod == null){
                //Python 3.0 has builtins and not __builtin__
                mod = modulesManager.getBuiltinModule("builtins", false);
            }
            if(mod != null){
                builtinMod.put(projectInterpreterName, mod);
            }

        } catch (MisconfigurationException e) {
            Log.log(e);
        }
        return builtinMod.get(projectInterpreterName);
    }

    private String getInternalName(String projectInterpreterName) {
        if(IPythonNature.DEFAULT_INTERPRETER.equals(projectInterpreterName)){
            //if it's the default, let's translate it to the outside world 
            try {
                return this.getDefaultInterpreter();
            } catch (NotConfiguredInterpreterException e) {
                Log.log(e);
                return projectInterpreterName;
            }
        }
        return projectInterpreterName;
    }

    public void clearBuiltinMod(String projectInterpreterName) {
        this.builtinMod.remove(projectInterpreterName);
    }


    /**
     * Constructor
     */
    @SuppressWarnings("unchecked")
    public AbstractInterpreterManager(Preferences prefs) {
        this.prefs = prefs;
        prefs.setDefault(getPreferenceName(), "");
        prefs.addPropertyChangeListener(new Preferences.IPropertyChangeListener(){

            public void propertyChange(PropertyChangeEvent event) {
                clearCaches();
            }
        });
        
        IInterpreterInfo[] interpreterInfos = this.getInterpreterInfos();
        for (IInterpreterInfo interpreterInfo : interpreterInfos) {
            this.exeToInfo.put(interpreterInfo.getExecutableOrJar(), (InterpreterInfo) interpreterInfo);
        }
        List<IInterpreterObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
        for (IInterpreterObserver observer : participants) {
            observer.notifyInterpreterManagerRecreated(this);
        }
    }

    public void clearCaches() {
        builtinMod.clear();
        builtinCompletions.clear();
        interpretersFromPersistedString = null;
        interpreterInfosFromPersistedString = null;
    }

    /**
     * @return the preference name where the options for this interpreter manager should be stored
     */
    protected abstract String getPreferenceName();
    
    /**
     * @throws NotConfiguredInterpreterException
     * @see org.python.pydev.core.IInterpreterManager#getDefaultInterpreter()
     */
    public String getDefaultInterpreter() throws NotConfiguredInterpreterException {
        IInterpreterInfo[] interpreters = getInterpreterInfos();
        if(interpreters.length > 0){
            String interpreter = interpreters[0].getExecutableOrJar();
            if(interpreter == null){
                throw new NotConfiguredInterpreterException("The configured interpreter is null, some error happened getting it.\n" +getNotConfiguredInterpreterMsg());
            }
            return interpreter;
        }else{
            throw new NotConfiguredInterpreterException(FullRepIterable.getLastPart(this.getClass().getName())+":"+getNotConfiguredInterpreterMsg());
        }
    }

    public void setInfos(List<IInterpreterInfo> infos) {
        synchronized(exeToInfo){
            this.exeToInfo.clear();
            for(IInterpreterInfo info:infos){
                exeToInfo.put(info.getExecutableOrJar(), (InterpreterInfo) info);
            }
        }
    }
    
    /**
     * @return a message to show to the user when there is no configured interpreter
     */
    protected abstract String getNotConfiguredInterpreterMsg(); 

    public IInterpreterInfo[] getInterpreterInfos() {
        if(interpreterInfosFromPersistedString == null){
            interpreterInfosFromPersistedString = getInterpretersFromPersistedString(getPersistedString());
        }
        return interpreterInfosFromPersistedString;
        
    }
    
    
    /**
     * @see org.python.pydev.core.IInterpreterManager#hasInfoOnInterpreter(java.lang.String)
     */
    public boolean hasInfoOnInterpreter(String interpreter){
        if(interpreter == null){
            InterpreterInfo info;
			try {
				info = (InterpreterInfo) exeToInfo.get(getDefaultInterpreter());
			} catch (NotConfiguredInterpreterException e) {
				return false;
			}
            return info != null;
        }
        try {
			return getInterpreterInfo(interpreter, null) != null;
		} catch (MisconfigurationException e) {
			return false;
		}
    }
    
    /**
     * @see org.python.pydev.core.IInterpreterManager#getDefaultInterpreterInfo(org.eclipse.core.runtime.IProgressMonitor)
     */
    public InterpreterInfo getDefaultInterpreterInfo(IProgressMonitor monitor)  throws MisconfigurationException{
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
     * @throws JDTNotAvailableException 
     */
    protected abstract Tuple<InterpreterInfo,String> internalCreateInterpreterInfo(String executable, IProgressMonitor monitor) throws CoreException, JDTNotAvailableException;
    
    /**
     * Creates the information for the passed interpreter.
     */
    public IInterpreterInfo createInterpreterInfo(String executable, IProgressMonitor monitor){
        
        monitor.worked(5);
        //ok, we have to get the info from the executable (and let's cache results for future use)...
        Tuple<InterpreterInfo,String> tup = null;
        InterpreterInfo info;
        try {

            tup = internalCreateInterpreterInfo(executable, monitor);
            if(tup == null){
                //Canceled (in the dialog that asks the user to choose the valid paths)
                return null;
            }
            info = tup.o1;
            
        } catch (RuntimeException e) {
            PydevPlugin.log(e);
            throw e;
        } catch (Exception e) {
            PydevPlugin.log(e);
            throw new RuntimeException(e);
        }
        if(info.executableOrJar == null || info.executableOrJar.trim().length() == 0){
            //it is null or empty
            final String title = "Invalid interpreter:"+executable;
            final String msg = "Unable to get information on interpreter!";
            String reasonCreation = "The interpreter (or jar): '"+executable+"' is not valid - info.executable found: "+info.executableOrJar+"\n";
            if(tup != null){
                reasonCreation += "The standard output gotten from the executed shell was: >>"+tup.o2+"<<";
            }
            final String reason = reasonCreation;
            
            try {
                final Display disp = Display.getDefault();
                disp.asyncExec(new Runnable(){
                    public void run() {
                        ErrorDialog.openError(null, title, msg, new Status(Status.ERROR, PydevPlugin.getPluginID(), 0, reason, null));
                    }
                });
            } catch (Throwable e) {
                // ignore error communication error
            }
            throw new RuntimeException(reason);
        }

        return info;
        
    }

    /**
     * Creates the interpreter info from the output. Checks for errors.
     */
    protected static InterpreterInfo createInfoFromOutput(IProgressMonitor monitor, Tuple<String, String> outTup) {
        if(outTup.o1 == null || outTup.o1.trim().length() == 0){
            throw new RuntimeException(
                    "No output was in the standard output when trying to create the interpreter info.\n" +
                    "The error output contains:>>"+outTup.o2+"<<");
        }
        InterpreterInfo info = InterpreterInfo.fromString(outTup.o1);
        return info;
    }

    /**
     * @throws MisconfigurationException 
     * @see org.python.pydev.core.IInterpreterManager#getInterpreterInfo(java.lang.String)
     */
    public InterpreterInfo getInterpreterInfo(String nameOrExecutableOrJar, IProgressMonitor monitor) throws MisconfigurationException {
        synchronized(lock){
            for(IInterpreterInfo info:this.exeToInfo.values()){
                if(info != null){
                    if(info.matchNameBackwardCompatible(nameOrExecutableOrJar)){
                        return (InterpreterInfo) info;
                    }
                }
            }
        }
        
        throw new MisconfigurationException(
        		StringUtils.format("Interpreter: %s not found", nameOrExecutableOrJar));
    }

    /**
     * Called when an interpreter should be added.
     * 
     * @see org.python.pydev.core.IInterpreterManager#addInterpreter(java.lang.String)
     */
    public void addInterpreterInfo(IInterpreterInfo info2) {
        InterpreterInfo info = (InterpreterInfo) info2;
        exeToInfo.put(info.executableOrJar, info);
    }

    private Object lock = new Object();
    //little cache...
    private String persistedCache;
    private IInterpreterInfo [] persistedCacheRet;
    
    /**
     * @see org.python.pydev.core.IInterpreterManager#getInterpretersFromPersistedString(java.lang.String)
     */
    public IInterpreterInfo[] getInterpretersFromPersistedString(String persisted) {
        synchronized(lock){
            if(persisted == null || persisted.trim().length() == 0){
                return new IInterpreterInfo[0];
            }
            
            if(persistedCache == null || persistedCache.equals(persisted) == false){
                List<IInterpreterInfo> ret = new ArrayList<IInterpreterInfo>();
    
                try {
                    List<InterpreterInfo> list = new ArrayList<InterpreterInfo>();                
                    String[] strings = persisted.split("&&&&&");
                    
                    //first, get it...
                    for (String string : strings) {
                        try {
                            list.add(InterpreterInfo.fromString(string));
                        } catch (Exception e) {
                            //ok, its format might have changed
                            String errMsg = "Interpreter storage changed.\r\n" +
                            "Please restore it (window > preferences > Pydev > Interpreter)";
                            PydevPlugin.log(errMsg, e);
                            
                            return new IInterpreterInfo[0];
                        }
                    }
                    
                    //then, put it in cache
                    for (InterpreterInfo info: list) {
                        if(info != null && info.executableOrJar != null){
                            ret.add(info);
                        }
                    }
                    
                    //and at last, restore the system info
                    for (final InterpreterInfo info: list) {
                        try {
                            ISystemModulesManager systemModulesManager = (ISystemModulesManager) PydevPlugin.readFromWorkspaceMetadata(info.getExeAsFileSystemValidPath());
                            info.setModulesManager(systemModulesManager);
                        } catch (Exception e) {
                            PydevPlugin.logInfo(new RuntimeException("Restoring info for: "+info.getExecutableOrJar(), e));
                            
                            //if it does not work it (probably) means that the internal storage format changed among versions,
                            //so, we have to recreate that info.
                            final Display def = Display.getDefault();
                            def.syncExec(new Runnable(){
    
                                public void run() {
                                    Shell shell = def.getActiveShell();
                                    ProgressMonitorDialog dialog = new AsynchronousProgressMonitorDialog(shell);
                                    dialog.setBlockOnOpen(false);
                                    try {
                                        dialog.run(false, false, new IRunnableWithProgress(){

                                            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                                                monitor.beginTask("Updating the interpreter info (internal format changed or corrupted).", 100);
                                                //ok, maybe its file-format changed... let's re-create it then.
                                                info.restorePythonpath(monitor);
                                                //after restoring it, let's save it.
                                                PydevPlugin.writeToWorkspaceMetadata(info.getModulesManager(), info.getExeAsFileSystemValidPath());
                                                monitor.done();
                                            }}
                                        );
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                
                            });
                            System.out.println("Finished restoring information for: "+info.executableOrJar+" at: "+info.getExeAsFileSystemValidPath());
                        }
                    }
                    
                } catch (Exception e) {
                    PydevPlugin.log(e);
                    
                    //ok, some error happened (maybe it's not configured)
                    return new IInterpreterInfo[0];
                }
                
                persistedCache = persisted;
                persistedCacheRet = ret.toArray(new IInterpreterInfo[0]);
            }
        }
        return persistedCacheRet;
    }

    /**
     * @see org.python.pydev.core.IInterpreterManager#getStringToPersist(IInterpreterInfo[])
     */
    public String getStringToPersist(IInterpreterInfo[] executables) {
        FastStringBuffer buf = new FastStringBuffer();
        for (IInterpreterInfo info : executables) {
            if(info!=null){
                buf.append(info.toString());
                buf.append("&&&&&");
            }
        }
        
        return buf.toString();
    }

    String persistedString;
    public String getPersistedString() {
        if(persistedString == null){
            persistedString = prefs.getString(getPreferenceName());
        }
        return persistedString;
    }
    
    public void setPersistedString(String s) {
        persistedString = s;
        prefs.setValue(getPreferenceName(), s);
    }
    
    /**
     * This method persists all the modules managers that are within this interpreter manager
     * (so, all the SystemModulesManagers will be saved -- and can be later restored).
     */
    public void saveInterpretersInfoModulesManager() {
        for(InterpreterInfo info : this.exeToInfo.values()){
            ISystemModulesManager modulesManager = info.getModulesManager();
            Object pythonPathHelper = modulesManager.getPythonPathHelper();
            if(!(pythonPathHelper instanceof PythonPathHelper)){
                continue;
            }
            PythonPathHelper pathHelper = (PythonPathHelper) pythonPathHelper;
            List<String> pythonpath = pathHelper.getPythonpath();
            if(pythonpath == null || pythonpath.size() == 0){
                continue;
            }
            PydevPlugin.writeToWorkspaceMetadata(modulesManager, info.getExeAsFileSystemValidPath());
        }
    }


    /**
     * @return whether this interpreter manager can be used to get info on the specified nature
     */
    public final boolean canGetInfoOnNature(IPythonNature nature) {
        try {
            return nature.getInterpreterType() == this.getInterpreterType();
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @see org.python.pydev.core.IInterpreterManager#hasInfoOnDefaultInterpreter(IPythonNature)
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
     * @see org.python.pydev.core.IInterpreterManager#restorePythopathForAllInterpreters(org.eclipse.core.runtime.IProgressMonitor)
     */
    @SuppressWarnings("unchecked")
    public void restorePythopathForInterpreters(IProgressMonitor monitor, Set<String> interpretersNamesToRestore) {
        synchronized(lock){
            for(String interpreter:exeToInfo.keySet()){
            	if(interpretersNamesToRestore != null){
            		if(!interpretersNamesToRestore.contains(interpreter)){
            			continue; //only restore the ones specified
            		}
            	}
                InterpreterInfo info;
				try {
					info = getInterpreterInfo(interpreter, monitor);
					info.restorePythonpath(monitor); //that's it, info.modulesManager contains the SystemModulesManager
					
					List<IInterpreterObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
					for (IInterpreterObserver observer : participants) {
						try {
							observer.notifyDefaultPythonpathRestored(this, interpreter, monitor);
						} catch (Exception e) {
							PydevPlugin.log(e);
						}
					}
				} catch (MisconfigurationException e1) {
					PydevPlugin.log(e1);
				}
            }
            

            FastStringBuffer buf = new FastStringBuffer();
            //Also notify that all the natures had the pythonpath changed (it's the system pythonpath, but still, 
            //clients need to know about it)
            List<IPythonNature> pythonNatures = PythonNature.getAllPythonNatures();
            for (IPythonNature nature : pythonNatures) {
                try {
                    //If they have the same type of the interpreter manager, notify.
                    if (this.getInterpreterType() == nature.getInterpreterType()) {
                    	IPythonPathNature pythonPathNature = nature.getPythonPathNature();
                    	
                    	
                    	//There's a catch here: if the nature uses some variable defined in the string substitution
                    	//from the interpreter info, we need to do a full build instead of only making a notification.
                    	String complete = pythonPathNature.getProjectExternalSourcePath(false) + 
                    			pythonPathNature.getProjectSourcePath(false);
                    	
                    	PythonNature n = (PythonNature) nature;
                    	String projectInterpreterName = n.getProjectInterpreterName();
                        if(IPythonNature.DEFAULT_INTERPRETER.equals(projectInterpreterName)){
                            //if it's the default, let's translate it to the outside world 
                            projectInterpreterName = this.getDefaultInterpreter();
                        }
                        InterpreterInfo info = exeToInfo.get(projectInterpreterName);
                        boolean makeCompleteRebuild = false;
                        if(info != null){
                        	Properties stringSubstitutionVariables = info.getStringSubstitutionVariables();
                        	if(stringSubstitutionVariables!=null){
	                        	Enumeration<Object> keys = stringSubstitutionVariables.keys();
	                        	while(keys.hasMoreElements()){
	                        		Object key = keys.nextElement();
	                        		buf.clear();
	                        		buf.append("${");
	                        		buf.append(key.toString());
	                        		buf.append("}");
	                        		
	                        		if(complete.indexOf(buf.toString()) != -1){
	                        			makeCompleteRebuild = true;
	                        			break;
	                        		}
                        		}
                        	}
                        }
                        
                        if(!makeCompleteRebuild){
                        	//just notify that it changed
                            if(nature instanceof PythonNature){
                                ((PythonNature) nature).clearCaches();
                            }
                        	PythonNatureListenersManager.notifyPythonPathRebuilt(nature.getProject(), nature);
                        }else{
                        	//Rebuild the whole info.
                        	nature.rebuildPath();
                        }
                    }
                } catch (Throwable e) {
                    PydevPlugin.log(e);
                }
            }
        }        
    }

    public boolean isConfigured() {
        try {
            String defaultInterpreter = getDefaultInterpreter();
            if(defaultInterpreter == null){
                return false;
            }
            if(defaultInterpreter.length() == 0){
                return false;
            }
        } catch (NotConfiguredInterpreterException e) {
            return false;
        }
        return true;
    }

    

}

