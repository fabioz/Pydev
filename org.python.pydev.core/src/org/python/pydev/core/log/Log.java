/*
 * Created on 12/06/2005
 */
package org.python.pydev.core.log;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.REF;


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

    
    //------------ Log that writes to .metadata/.plugins/org.python.pydev.core/PydevLog.log

    private final static Object lock = new Object(); 
    private final static StringBuffer logIndent = new StringBuffer();
    
    public synchronized static void toLogFile(Object obj, String string) {
        synchronized(lock){
        	Class<? extends Object> class1 = obj.getClass();
            toLogFile(string, class1);
        }
    }

	public static void toLogFile(String string, Class<? extends Object> class1) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(logIndent);
		buffer.append(FullRepIterable.getLastPart(class1.getName()));
		buffer.append(": ");
		buffer.append(string);
		
		toLogFile(buffer.toString());
	}

    private synchronized static void toLogFile(String buffer) {
        synchronized(lock){
            try{
                IPath stateLocation = CorePlugin.getDefault().getStateLocation().append("PydevLog.log");
                String file = stateLocation.toOSString();
                REF.appendStrToFile(buffer+"\r\n", file);
            }catch(Throwable e){
                log(e); //default logging facility
            }
        }
    }
    
    public static void toLogFile(Exception e) {
		String msg = getExceptionStr(e);
		toLogFile(msg);
    }

	public static String getExceptionStr(Exception e) {
		final ByteArrayOutputStream str = new ByteArrayOutputStream();
		final PrintStream prnt = new PrintStream(str);
		e.printStackTrace(prnt);
		prnt.flush();
		String msg = new String(str.toByteArray());
		return msg;
	}

    public synchronized static void addLogLevel() {
        synchronized(lock){
            logIndent.append("    ");
        }        
    }

    public synchronized static void remLogLevel() {
        synchronized(lock){
            if(logIndent.length() > 3){
                logIndent.delete(0,4);
            }
        }
    }


}
