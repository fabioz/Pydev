/*
 * Created on 12/06/2005
 */
package org.python.pydev.core.log;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.python.pydev.core.CorePlugin;


/**
 * @author Fabio
 */
public class Log {

    
    /**
     * @param errorLevel IStatus.[OK|INFO|WARNING|ERROR]
     */
    public static void log(int errorLevel, String message, Throwable e) {
        System.err.println(message);
        if(e != null){
            e.printStackTrace();
        }
        try {
            
	        Status s = new Status(errorLevel, CorePlugin.getPluginID(), errorLevel, message, e);
	        CorePlugin.getDefault().getLog().log(s);
        } catch (Exception e1) {
            //logging should not fail!
        }
    }

    public static void log(Throwable e) {
        log(IStatus.ERROR, e.getMessage() != null ? e.getMessage() : "No message gotten.", e);
    }

    public static void log(String msg) {
        log(IStatus.ERROR, msg, new RuntimeException(msg));
    }

}
