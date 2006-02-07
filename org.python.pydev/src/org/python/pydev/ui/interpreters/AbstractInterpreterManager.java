/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.interpreters;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
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
    @SuppressWarnings("unchecked")
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
            throw new NotConfiguredInterpreterException(this.getClass().getName()+":"+getNotConfiguredInterpreterMsg());
        }
    }

    public void clearAllBut(List<String> allButTheseInterpreters) {
        synchronized(exeToInfo){
            ArrayList<String> toRemove = new ArrayList<String>();
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
    public abstract Tuple<InterpreterInfo,String> createInterpreterInfo(String executable, IProgressMonitor monitor) throws CoreException;

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
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#getInterpreterInfo(java.lang.String)
     */
    public InterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor) {
    	synchronized(lock){
	        InterpreterInfo info = (InterpreterInfo) exeToInfo.get(executable);
	        if(info == null){
	            monitor.worked(5);
	            //ok, we have to get the info from the executable (and let's cache results for future use)...
	            Tuple<InterpreterInfo,String> tup = null;
	    		try {
	
	    			tup = createInterpreterInfo(executable, monitor);
	                info = tup.o1;
	                
	    	    } catch (Exception e) {
	    	        PydevPlugin.log(e);
	    	        throw new RuntimeException(e);
	    	    }
	    	    if(info.executableOrJar != null && info.executableOrJar.trim().length() > 0){
	    	        exeToInfo.put(info.executableOrJar, info);
	    	        
	    	    }else{ //it is null or empty
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
	                    // ignore error comunication error
	                }
	    	        throw new RuntimeException(reason);
	    	    }
	        }
	        return info;
    	}
    }

    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#addInterpreter(java.lang.String)
     */
    public String addInterpreter(String executable, IProgressMonitor monitor) {
        InterpreterInfo info = getInterpreterInfo(executable, monitor);
        return info.executableOrJar;
    }

    private Object lock = new Object();
    //little cache...
    private String persistedCache;
    private String [] persistedCacheRet;
    
    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#getInterpretersFromPersistedString(java.lang.String)
     */
    public String[] getInterpretersFromPersistedString(String persisted) {
    	synchronized(lock){
	        if(persisted == null || persisted.trim().length() == 0){
	            return new String[0];
	        }
	        
	        if(persistedCache == null || persistedCache.equals(persisted) == false){
		        List<String> ret = new ArrayList<String>();
	
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
	                        
	                        return new String[0];
	                    }
	                }
	                
	                //then, put it in cache
	                for (InterpreterInfo info: list) {
	                    if(info != null && info.executableOrJar != null){
	    	                this.exeToInfo.put(info.executableOrJar, info);
	    	                ret.add(info.executableOrJar);
	                    }
		            }
	                
	                //and at last, restore the system info
		            for (final InterpreterInfo info: list) {
		                try {
	                        info.modulesManager = (SystemModulesManager) PydevPlugin.readFromPlatformFile(info.getExeAsFileSystemValidPath());
	                    } catch (Exception e) {
	                    	PydevPlugin.log(e);
	                    	
	                    	//if it does not work
	                    	final Display def = Display.getDefault();
	                    	def.syncExec(new Runnable(){
	
								public void run() {
									Shell shell = def.getActiveShell();
			                    	ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
			                        dialog.setBlockOnOpen(false);
			                        try {
										dialog.run(false, false, new IRunnableWithProgress(){
	
											public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
												monitor.beginTask("Updating the interpreter info (internal storage format changed).", 100);
												//ok, maybe its file-format changed... let's re-create it then.
												doRestore(info, monitor);
												monitor.done();
											}}
										);
									} catch (Exception e) {
										throw new RuntimeException(e);
									}
								}
								
	                		});
	                    	System.out.println("Finished restoring information for: "+info.executableOrJar);
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
        }
        return persistedCacheRet;
    }

    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#getStringToPersist(java.lang.String[])
     */
    public String getStringToPersist(String[] executables) {
        StringBuffer buf = new StringBuffer();
        for (String exe : executables) {
            InterpreterInfo info = this.exeToInfo.get(exe);
            if(info!=null){
                PydevPlugin.writeToPlatformFile(info.modulesManager, info.getExeAsFileSystemValidPath());
                buf.append(info.toString());
                buf.append("&&&&&");
            }
        }
        
        return buf.toString();
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
    @SuppressWarnings("unchecked")
	public void restorePythopathFor(String defaultSelectedInterpreter, IProgressMonitor monitor) {
    	synchronized(lock){
	        final InterpreterInfo info = getInterpreterInfo(defaultSelectedInterpreter, monitor);
	        info.restorePythonpath(monitor); //that's it, info.modulesManager contains the SystemModulesManager
	        
	        List<IInterpreterObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
	        for (IInterpreterObserver observer : participants) {
	            try {
					observer.notifyDefaultPythonpathRestored(this, defaultSelectedInterpreter, monitor);
				} catch (Exception e) {
					PydevPlugin.log(e);
				}
	        }
	        
	        //update the natures...
	        List<IPythonNature> pythonNatures = PythonNature.getAllPythonNatures();
	        for (IPythonNature nature : pythonNatures) {
	        	try {
	        		//if they have the same type of the interpreter manager.
					if (this.isPython() == nature.isPython() || this.isJython() == nature.isJython()) {
						nature.rebuildPath(defaultSelectedInterpreter, monitor);
					}
				} catch (Throwable e) {
					PydevPlugin.log(e);
				}
	        }
    	}        
    }

	private void doRestore(final InterpreterInfo info, IProgressMonitor monitor) {
		info.restorePythonpath(monitor);
		//after restoring it, let's save it.
		PydevPlugin.writeToPlatformFile(info.modulesManager, info.getExeAsFileSystemValidPath());
	}
}

