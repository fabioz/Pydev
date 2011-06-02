/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 12/06/2005
 */
package org.python.pydev.core.log;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.Tuple;


/**
 * @author Fabio
 */
public class Log {

    /**
     * Set to true to allow log messages to appear on the console when printToConsole is true.
     * <p>
     * Only applies when not in testing mode as in testing mode message is always printed.
     */
    private static final boolean DEBUG = true;
    
    /**
     * Console used to log contents
     */
    private static MessageConsole fConsole;
	private static IOConsoleOutputStream fOutputStream;

	private static Map<Tuple<Integer, String>, Long> lastLoggedTime = new HashMap<Tuple<Integer,String>, Long>();

    /**
     * @param errorLevel IStatus.[OK|INFO|WARNING|ERROR]
     * @return CoreException that can be thrown for the given log event
     */
	public static CoreException log(int errorLevel, String message, Throwable e, boolean printToConsole) {
	    if (CorePlugin.getDefault() == null) {
	        // testing mode, always print to console as there is no logger to log to
	        printToConsole = true;
	    } else if (!DEBUG) {
	        printToConsole = false;
	    }
	    
        Status s = new Status(errorLevel, CorePlugin.getPluginID(), errorLevel, message, e);
        CoreException coreException = new CoreException(s);

        Tuple<Integer, String> key = new Tuple<Integer, String>(errorLevel, message);
        synchronized (lastLoggedTime) {
            Long lastLoggedMillis = lastLoggedTime.get(key);
            long currentTimeMillis = System.currentTimeMillis();
            if(lastLoggedMillis != null){
                if(currentTimeMillis < lastLoggedMillis + (20 * 1000)) {
                    //System.err.println("Skipped report of:"+message);
                    return coreException; //Logged in the last 20 seconds, so, just skip it for now
                }
            }
            lastLoggedTime.put(key, currentTimeMillis);
        }
        if (printToConsole) {
            System.err.println(message);
            if (e != null) {
                if (!(e instanceof MisconfigurationException)) {
                    e.printStackTrace();
                }
            }
        }
        try {
            if (CorePlugin.getDefault() != null) {
                CorePlugin.getDefault().getLog().log(s);
            }
        } catch (Exception e1) {
            //logging should not fail!
        }
        return coreException;
    }
	
    public static CoreException log(String message, Throwable e, boolean printToConsole) {
        return log(IStatus.ERROR, message, e, true);
    }
    
    public static CoreException log(int errorLevel, String message, Throwable e) {
        return log(errorLevel, message, e, true);
    }

    public static CoreException log(Throwable e, boolean printToConsole) {
        return log(IStatus.ERROR, e.getMessage() != null ? e.getMessage() : "No message gotten (null message).", e, printToConsole);
    }

    public static CoreException log(String msg) {
        return log(IStatus.ERROR, msg, new RuntimeException(msg));
    }
    
    public static CoreException log(String msg, Throwable e) {
        return log(IStatus.ERROR, msg, e);
    }

    public static CoreException log(Throwable e) {
        return log(e, true);
    }

    public static CoreException logInfo(Throwable e) {
        return log(IStatus.INFO, e.getMessage(), e, true);
    }
    
    public static CoreException logInfo(String msg) {
        return log(IStatus.INFO, msg, null, false);
    }
    
    //------------ Log that writes to a new console

    private final static Object lock = new Object(); 
    private final static StringBuffer logIndent = new StringBuffer();
    
    public static void toLogFile(Object obj, String string) {
        synchronized(lock){
            if(obj == null){
                obj = new Object();
            }
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

    private static void toLogFile(final String buffer) {
        final Runnable r = new Runnable(){

            public void run() {
                synchronized(lock){
                    try{
                        CorePlugin default1 = CorePlugin.getDefault();
                        if(default1 == null){
                            //in tests
                            System.out.println(buffer);
                            return;
                        }
                        
                        //also print to console
                        System.out.println(buffer);
                        IOConsoleOutputStream c = getConsoleOutputStream();
                        c.write(buffer.toString());
                        c.write("\r\n");
                        
//                IPath stateLocation = default1.getStateLocation().append("PyDevLog.log");
//                String file = stateLocation.toOSString();
//                REF.appendStrToFile(buffer+"\r\n", file);
                    }catch(Throwable e){
                        log(e); //default logging facility
                    }
                }
                
            }
        };
        
        Display current = Display.getCurrent();
        if(current != null && current.getThread() == Thread.currentThread ()){
            //ok, just run it
            r.run();
        }else{
            if(current == null){
                current = Display.getDefault();
                current.asyncExec(r);
            }
        }
    }
    
    
    private static IOConsoleOutputStream getConsoleOutputStream(){
        if (fConsole == null){
			fConsole = new MessageConsole("PyDev Logging", CorePlugin.getImageCache().getDescriptor("icons/python_logging.png"));
			
            fOutputStream = fConsole.newOutputStream();
            
			HashMap<IOConsoleOutputStream, String> themeConsoleStreamToColor = new HashMap<IOConsoleOutputStream, String>();
			themeConsoleStreamToColor.put(fOutputStream, "console.output");

            fConsole.setAttribute("themeConsoleStreamToColor", themeConsoleStreamToColor);

            ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{fConsole});
        }
        return fOutputStream;
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

    public static void addLogLevel() {
        synchronized(lock){
            logIndent.append("    ");
        }        
    }

    public static void remLogLevel() {
        synchronized(lock){
            if(logIndent.length() > 3){
                logIndent.delete(0,4);
            }
        }
    }
    
    /**
     * @deprecated use one of the other log methods
     * XXX: At time of deprecation, none of the PyDev bundles
     * call this method.
     */
    @Deprecated
    public static void log(IStatus status) {
        CorePlugin.getDefault().getLog().log(status);
    }

}
