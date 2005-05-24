/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;
import org.python.pydev.utils.SimplePythonRunner;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Does not write directly in INTERPRETER_PATH, just loads from it and works with it.
 * 
 * @author Fabio Zadrozny
 */
public class InterpreterManager implements IInterpreterManager {

    private Map exeToInfo = new HashMap();
    private Preferences prefs;

    /**
     * Constructor
     */
    public InterpreterManager(Preferences prefs) {
        this.prefs = prefs;
//        prefs.setDefault(INTERPRETER_PATH, getStringToPersist(new String[]{"python"}));
        prefs.setDefault(INTERPRETER_PATH, "");
    }
    
    /**
     * @see org.python.pydev.ui.IInterpreterManager#getDefaultInterpreter()
     */
    public String getDefaultInterpreter() {
        try {
            return getInterpreters()[0];
        } catch (Exception e) {
            PydevPlugin.log(e);
            return getInterpreterInfo("python", new NullProgressMonitor()).executable;
        }
    }

    /**
     * @see org.python.pydev.ui.IInterpreterManager#getInterpreters()
     */
    public String[] getInterpreters() {
        return getInterpretersFromPersistedString(prefs.getString(INTERPRETER_PATH));
    }
    
    /**
     * @see org.python.pydev.ui.IInterpreterManager#getDefaultInterpreterInfo(org.eclipse.core.runtime.IProgressMonitor)
     */
    public InterpreterInfo getDefaultInterpreterInfo(IProgressMonitor monitor) {
        String interpreter = getDefaultInterpreter();
        return getInterpreterInfo(interpreter, monitor);
    }
    
    /**
     * @see org.python.pydev.ui.IInterpreterManager#getInterpreterInfo(java.lang.String)
     */
    public InterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor) {
        InterpreterInfo info = (InterpreterInfo) exeToInfo.get(executable);
        if(info == null){
            monitor.worked(5);
            //ok, we have to get the info from the executable (and let's cache results for future use...
    		try {
    	        File script = PydevPlugin.getScriptWithinPySrc("interpreterInfo.py");
    	        String string = SimplePythonRunner.runAndGetOutputWithInterpreter(executable, script.getAbsolutePath(), null, null, null, monitor);
    	        info = InterpreterInfo.fromString(string);
    	        info.restoreCompiledLibs(monitor);
    	    } catch (Exception e) {
    	        PydevPlugin.log(e);
    	        //TODO: make dialog: unable to get info for file... 
    	        throw new RuntimeException(e);
    	    }
    	    if(info.executable != null && info.executable.trim().length() > 0){
    	        exeToInfo.put(info.executable, info);
    	        
    	    }else{
                String title = "Invalid interpreter:"+executable;
                String msg = "Unable to get information on interpreter!";
    	        String reason = "The interpreter: '"+executable+"' is not a valid python executable.";
    	        
                ErrorDialog.openError(null, title, msg, new Status(Status.ERROR, PydevPlugin.getPluginID(),0 ,reason, null ));
    	        throw new RuntimeException(reason);
    	    }
        }
        return info;
    }

    /**
     * @see org.python.pydev.ui.IInterpreterManager#addInterpreter(java.lang.String)
     */
    public String addInterpreter(String executable, IProgressMonitor monitor) {
        InterpreterInfo info = getInterpreterInfo(executable, monitor);
        return info.executable;
    }

    //little cache...
    private String persistedCache;
    private String [] persistedCacheRet;
    
    /**
     * @see org.python.pydev.ui.IInterpreterManager#getInterpretersFromPersistedString(java.lang.String)
     */
    public String[] getInterpretersFromPersistedString(String persisted) {

        if(persisted == null || persisted.trim().length() == 0){
            return new String[0];
        }
        
        if(persistedCache == null || persistedCache.equals(persisted) == false){
	        List ret = new ArrayList();
	        BASE64Decoder decoder = new BASE64Decoder();

	        try {
	            InputStream input = new ByteArrayInputStream(decoder.decodeBuffer(persisted));
	            ObjectInputStream in = new ObjectInputStream(input);
	            List list = (List) in.readObject();
	            for (Iterator iter = list.iterator(); iter.hasNext();) {
	                InterpreterInfo info = (InterpreterInfo) iter.next();
	                this.exeToInfo.put(info.executable, info);
	                ret.add(info.executable);
	            }
	            in.close();
                input.close();
            } catch (Exception e) {
                PydevPlugin.log(e);
                
                //ok, some error happened, let's get the default
                InterpreterInfo info = getInterpreterInfo("python", new NullProgressMonitor());
                persisted = getStringToPersist(new String[]{info.executable});
                prefs.setValue(INTERPRETER_PATH, persisted);
                ret.add(info.executable);
            }
            
	        persistedCache = persisted;
	        persistedCacheRet = (String[]) ret.toArray(new String[0]);
        }
        return persistedCacheRet;
    }

    /**
     * @see org.python.pydev.ui.IInterpreterManager#getStringToPersist(java.lang.String[])
     */
    public String getStringToPersist(String[] executables) {
        ArrayList list = new ArrayList();
        for (int i = 0; i < executables.length; i++) {
            Object info = this.exeToInfo.get(executables[i]);
            if(info!=null){
                list.add(info);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream stream = new ObjectOutputStream(out);
            stream.writeObject(list);
            stream.close();
        } catch (Exception e) {
            PydevPlugin.log(e);
            throw new RuntimeException(e);
        }

        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(out.toByteArray());
    }


}
