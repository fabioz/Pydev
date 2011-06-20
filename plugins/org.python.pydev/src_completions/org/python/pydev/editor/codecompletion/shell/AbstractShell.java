/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.shell;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.net.LocalHost;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.editor.codecompletion.revisited.ModulesManager;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.SocketUtil;
import org.python.pydev.runners.ThreadStreamReader;

/**
 * This is the shell that 'talks' to the python / jython process (it is intended to be subclassed so that
 * we know how to deal with each). 
 * 
 * Its methods are synched to prevent concurrent access.
 * 
 * @author fabioz
 *
 */
public abstract class AbstractShell {

    public static final int BUFFER_SIZE = 1024 ;
    public static final int OTHERS_SHELL = 2;
    public static final int COMPLETION_SHELL = 1;
    protected static final int DEFAULT_SLEEP_BETWEEN_ATTEMPTS = 1000; //1sec, so we can make the number of attempts be shown as elapsed in secs
    protected static final int DEBUG_SHELL = -1;
    
    private final String TYPE_UNKNOWN_STR = ""+IToken.TYPE_UNKNOWN;
    
    /**
     * Determines if we are already in a method that starts the shell
     */
    private boolean inStart = false;

    /**
     * Determines if we are (theoretically) already connected (meaning that trying to start the shell
     * again will not do anything)
     * 
     * Ending the shell sets this to false and starting it sets it to true (if successful) 
     */
    private boolean isConnected = false;

    private boolean isInRead = false;
    private boolean isInWrite = false;
    private boolean isInRestart = false;
    private IInterpreterInfo shellInterpreter;
    private int shellMillis;
    
    /**
     * Lock to know if there is someone already using this shell for some operation
     */
    private boolean isInOperation = false;

    private static void dbg(String string, int priority) {
        if(priority <= DEBUG_SHELL){
            System.out.println(string);
        }
        if(DebugSettings.DEBUG_CODE_COMPLETION){
            Log.toLogFile(string, AbstractShell.class);
        }
    }

    /**
     * the encoding used to encode messages
     */
    private static final String ENCODING_UTF_8 = "UTF-8";
    
    /**
     * Reference to 'global python shells'
     * 
     * this works as follows:
     * we have the interpreter as that the shell is related to as the 1st key
     * 
     * and then we have the id with the shell type that points to the actual shell
     * 
     * @see #COMPLETION_SHELL
     * @see #OTHERS_SHELL
     */
    protected static Map<String,Map<Integer,AbstractShell>> shells = new HashMap<String,Map<Integer,AbstractShell>>();
    
    /**
     * if we are already finished for good, we may not start new shells (this is a static, because this 
     * should be set only at shutdown).
     */
    private static boolean finishedForGood = false;
    
    /**
     * simple stop of a shell (it may be later restarted)
     */
    public synchronized static void stopServerShell(IInterpreterInfo interpreter, int id) {
        synchronized(shells){
            Map<Integer, AbstractShell> typeToShell = getTypeToShellFromId(interpreter);
            AbstractShell pythonShell = (AbstractShell) typeToShell.get(new Integer(id));
            
            if(pythonShell != null){
                try {
                    pythonShell.endIt();
                } catch (Exception e) {
                    // ignore... we are ending it anyway...
                }
            }
            typeToShell.remove(id); //there's no exception if it was not there in the 1st place...
        }
    }

    
    /**
     * stops all registered shells 
     *
     */
    public synchronized static void shutdownAllShells(){
        synchronized(shells){
            if(DebugSettings.DEBUG_CODE_COMPLETION){
                Log.toLogFile("Shutting down all shells (for good)...", AbstractShell.class);
            }

            for (Iterator<Map<Integer, AbstractShell>> iter = shells.values().iterator(); iter.hasNext();) {
                finishedForGood = true;  //we may no longer restart shells
                
                Map<Integer,AbstractShell> rel = (Map<Integer, AbstractShell>) iter.next();
                if(rel != null){
                    for (Iterator<AbstractShell> iter2 = rel.values().iterator(); iter2.hasNext();) {
                        AbstractShell element = iter2.next();
                        if(element != null){
                            try {
                                element.shutdown(); //shutdown
                            } catch (Exception e) {
                                PydevPlugin.log(e); //let's log it... this should not happen
                            }
                        }
                    }
                }
            }
            shells.clear();
        }
    }
    
    

    /**
     * Restarts all the shells and clears any related cache.
     * 
     * @return an error message if some exception happens in this process (an empty string means all went smoothly).
     */
	public static String restartAllShells() {
		String ret = "";
        synchronized(shells){
        	try {
                if(DebugSettings.DEBUG_CODE_COMPLETION){
                    Log.toLogFile("Restarting all shells and clearing caches...", AbstractShell.class);
                }

				for(Map<Integer,AbstractShell> val:shells.values()){
					for(AbstractShell val2:val.values()){
					    if(val2 != null){
					        val2.endIt();
					    }
					}
					IInterpreterManager[] interpreterManagers = PydevPlugin.getAllInterpreterManagers();
					for (IInterpreterManager iInterpreterManager : interpreterManagers) {
					    if(iInterpreterManager == null){
					        continue; //Should happen only on testing...
					    }
						try {
							iInterpreterManager.clearCaches();
						} catch (Exception e) {
							PydevPlugin.log(e);
							ret += e.getMessage()+"\n";
						}
					}
					//Clear the global modules cache!
					ModulesManager.clearCache();
				}
			} catch (Exception e) {
				PydevPlugin.log(e);
				ret += e.getMessage()+"\n";
			}
        }
        return ret;
	}

    /**
     * @param interpreter the interpreter whose shell we want.
     * @return a map with the type of the shell mapping to the shell itself
     */
    private synchronized static Map<Integer, AbstractShell> getTypeToShellFromId(IInterpreterInfo interpreter) {
        synchronized(shells){
            Map<Integer, AbstractShell> typeToShell = shells.get(interpreter.getExecutableOrJar());
            
            if (typeToShell == null) {
                typeToShell = new HashMap<Integer, AbstractShell>();
                shells.put(interpreter.getExecutableOrJar(), typeToShell);
            }
            return typeToShell;
        }
    }

    /**
     * register a shell and give it an id
     * 
     * @param nature the nature (which has the information on the interpreter we want to used)
     * @param id the shell id
     * @see #COMPLETION_SHELL
     * @see #OTHERS_SHELL
     *
     * @param shell the shell to register
     */
    public synchronized static void putServerShell(IPythonNature nature, int id, AbstractShell shell) {
        synchronized(shells){
            try {
                Map<Integer, AbstractShell> typeToShell = getTypeToShellFromId(nature.getProjectInterpreter());
                typeToShell.put(new Integer(id), shell);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public synchronized static AbstractShell getServerShell(IPythonNature nature, int id) throws IOException, JDTNotAvailableException, CoreException, MisconfigurationException, PythonNatureWithoutProjectException{
        return getServerShell(nature.getProjectInterpreter(), nature.getInterpreterType(), id);
    }
    
    /**
     * @param interpreter the interpreter that should create the shell
     * 
     * @param relatedTo identifies to which kind of interpreter the shell should be related.
     * @see org.python.pydev.core.IPythonNature#INTERPRETER_TYPE_PYTHON
     * @see org.python.pydev.core.IPythonNature#INTERPRETER_TYPE_JYTHON
     * 
     * @param a given id for the shell
     * @see #COMPLETION_SHELL
     * @see #OTHERS_SHELL
     * 
     * @return the shell with the given id related to some nature
     * 
     * @throws CoreException
     * @throws IOException
     * @throws MisconfigurationException 
     */
    private synchronized static AbstractShell getServerShell(IInterpreterInfo interpreter, int relatedTo, int id) throws IOException, JDTNotAvailableException, CoreException, MisconfigurationException {
        AbstractShell pythonShell = null;
        synchronized(shells){
            if(DebugSettings.DEBUG_CODE_COMPLETION){
                Log.toLogFile("Synchronizing on shells...", AbstractShell.class);
            }
            if(DebugSettings.DEBUG_CODE_COMPLETION){
                String flavor;
                switch(relatedTo){
	                case IPythonNature.INTERPRETER_TYPE_JYTHON:
	                	flavor = "Jython";
	                	break;
	                case IPythonNature.INTERPRETER_TYPE_IRONPYTHON:
	                	flavor = "IronPython";
	                	break;
	                default:
	                	flavor = "Python";
                };
				Log.toLogFile( "Getting shell related to:"+ flavor+
                        " id:"+id+" interpreter: "+interpreter.getExecutableOrJar(), AbstractShell.class);
            }
            Map<Integer, AbstractShell> typeToShell = getTypeToShellFromId(interpreter);
            pythonShell = (AbstractShell) typeToShell.get(new Integer(id));
            
            if(pythonShell == null){
                if(DebugSettings.DEBUG_CODE_COMPLETION){
                    Log.toLogFile("pythonShell == null", AbstractShell.class);
                }
                if(relatedTo == IPythonNature.INTERPRETER_TYPE_PYTHON){
                    pythonShell = new PythonShell();
                    
                }else if(relatedTo == IPythonNature.INTERPRETER_TYPE_JYTHON){
                    pythonShell = new JythonShell();
                    
                }else if(relatedTo == IPythonNature.INTERPRETER_TYPE_IRONPYTHON){
                    pythonShell = new IronpythonShell();
                    
                }else{
                    throw new RuntimeException("unknown related id");
                }
                if(DebugSettings.DEBUG_CODE_COMPLETION){
                    Log.toLogFile("pythonShell.startIt()", AbstractShell.class);
                    Log.addLogLevel();
                }
                pythonShell.startIt(interpreter, AbstractShell.DEFAULT_SLEEP_BETWEEN_ATTEMPTS); //first start it
                if(DebugSettings.DEBUG_CODE_COMPLETION){
                    Log.remLogLevel();
                    Log.toLogFile("Finished pythonShell.startIt()", AbstractShell.class);
                }
                
                //then make it accessible
                typeToShell.put(new Integer(id), pythonShell);
            }
            
        }
        return pythonShell;
    }

    /**
     * Python server process.
     */
    protected Process process;
    /**
     * We should write in this socket.
     */
    protected Socket socketToWrite;
    /**
     * We should read this socket.
     */
    protected Socket socketToRead;
    /**
     * Python file that works as the server.
     */
    protected File serverFile;
    /**
     * Server socket (accept connections).
     */
    protected ServerSocket serverSocket;
    private ThreadStreamReader stdReader;
    private ThreadStreamReader errReader;

    
    /**
     * Initialize given the file that points to the python server (execute it
     * with python).
     *  
     * @param f file pointing to the python server
     * 
     * @throws IOException
     * @throws CoreException
     */
    protected AbstractShell(File f) throws IOException, CoreException {
        if(finishedForGood){
            throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to create a new shell.");
        }
        
        serverFile = f;
        if(!serverFile.exists()){
            throw new RuntimeException("Can't find python server file");
        }
    }

    /**
     * Just wait a little...
     */
    protected synchronized void sleepALittle(int t) {
        try {
            synchronized(this){
                wait(t); //millis
            }
        } catch (InterruptedException e) {
        }
    }

    /**
     * This method creates the python server process and starts the sockets, so that we
     * can talk with the server.
     * @throws IOException
     * @throws CoreException
     * @throws MisconfigurationException 
     * @throws PythonNatureWithoutProjectException 
     */
    /*package*/ synchronized void startIt(IPythonNature nature) throws IOException, JDTNotAvailableException, CoreException, MisconfigurationException, PythonNatureWithoutProjectException {
        synchronized(this){
            this.startIt(nature.getProjectInterpreter(), AbstractShell.DEFAULT_SLEEP_BETWEEN_ATTEMPTS);
        }
    }


    /**
     * This method creates the python server process and starts the sockets, so that we
     * can talk with the server.
     * 
     * @param milisSleep: time to wait after creating the process.
     * @throws IOException is some error happens creating the sockets - the process is terminated.
     * @throws JDTNotAvailableException 
     * @throws CoreException 
     * @throws CoreException
     * @throws MisconfigurationException 
     */
    protected synchronized void startIt(IInterpreterInfo interpreter, int milisSleep) throws IOException, JDTNotAvailableException, CoreException, MisconfigurationException {
        this.shellMillis = milisSleep;
        this.shellInterpreter = interpreter;
        if(inStart || isConnected){
            //it is already in the process of starting, so, if we are in another thread, just forget about it.
            return;
        }
        inStart = true;
        try {
            if (finishedForGood) {
                throw new RuntimeException(
                        "Shells are already finished for good, so, it is an invalid state to try to restart it.");
            }

            try {

                serverSocket = new ServerSocket(0); //read in this port
                int pRead = serverSocket.getLocalPort();
                SocketUtil.checkValidPort(pRead);
                int pWrite = SocketUtil.findUnusedLocalPorts(1)[0];

                if (process != null) {
                    endIt(); //end the current process
                }

                String execMsg = createServerProcess(interpreter, pWrite, pRead);
                dbg("executing " + execMsg,1);

                sleepALittle(200);
                String osName = System.getProperty("os.name");
                if (process == null) {
                    String msg = "Error creating python process - got null process(" + execMsg + ") - os:" + osName;
                    dbg(msg, 1);
                    PydevPlugin.log(msg);
                    throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, msg, new Exception(msg)));
                }
                try {
                    int exitVal = process.exitValue(); //should throw exception saying that it still is not terminated...
                    String msg = "Error creating python process - exited before creating sockets - exitValue = ("+ exitVal + ")(" + execMsg + ") - os:" + osName;
                    dbg(msg, 1);
                    PydevPlugin.log(msg);
                    throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, msg, new Exception(msg)));
                } catch (IllegalThreadStateException e2) { //this is ok
                }

                dbg("afterCreateProcess ",1);
                //ok, process validated, so, let's get its output and store it for further use.
                afterCreateProcess();

                boolean connected = false;
                int attempts = 0;

                dbg("connecting... ",1);
                sleepALittle(milisSleep);
                socketToWrite = null;
                int maxAttempts = PyCodeCompletionPreferencesPage.getNumberOfConnectionAttempts();
                
                dbg("attempts: "+attempts,1);
                dbg("maxAttempts: "+maxAttempts,1);
                dbg("finishedForGood: "+finishedForGood,1);
                
                while (!connected && attempts < maxAttempts && !finishedForGood) {
                    attempts += 1;
                    dbg("connecting attept..."+attempts,1);
                    try {
                        if (socketToWrite == null || socketToWrite.isConnected() == false) {
                            socketToWrite = new Socket(LocalHost.getLocalHost(), pWrite); //we should write in this port
                        }

                        if (socketToWrite != null || socketToWrite.isConnected()) {
                            serverSocket.setSoTimeout(milisSleep * 2); //let's give it a higher timeout, as we're already half - connected
                            try {
                                dbg("serverSocket.accept()! ",1);
                                socketToRead = serverSocket.accept();
                                dbg("socketToRead.setSoTimeout(5000) ",1);
                                socketToRead.setSoTimeout(5000);
                                connected = true;
                                dbg("connected! ",1);
                            } catch (SocketTimeoutException e) {
                                //that's ok, timeout for waiting connection expired, let's check it again in the next loop
                            }
                        }
                    } catch (IOException e1) {
                        if (socketToWrite != null && socketToWrite.isConnected() == true) {
                            String msg = "Attempt: " + attempts + " of " + maxAttempts + 
                            " failed, trying again...(socketToWrite already binded)";
                            
                            dbg(msg,1);
                            PydevPlugin.log(IStatus.ERROR, msg, e1);
                        }
                        if (socketToWrite != null && !socketToWrite.isConnected() == true) {
                            String msg = "Attempt: " + attempts + " of " + maxAttempts +
                            " failed, trying again...(socketToWrite still not binded)";
                            
                            dbg(msg,1);
                            PydevPlugin.log(IStatus.ERROR, msg, e1);
                        }
                    }

                    //if not connected, let's sleep a little for another attempt
                    if (!connected) {
                        sleepALittle(milisSleep);
                    }
                }

                if (!connected && !finishedForGood) {
                    dbg("NOT connected ",1);

                    //what, after all this trouble we are still not connected????!?!?!?!
                    //let's communicate this to the user...
                    String isAlive;
                    try {
                        int exitVal = process.exitValue(); //should throw exception saying that it still is not terminated...
                        isAlive = " - the process in NOT ALIVE anymore (output=" + exitVal + ") - ";
                    } catch (IllegalThreadStateException e2) { //this is ok
                        isAlive = " - the process in still alive (killing it now)- ";
                        process.destroy();
                    }

                    String output = getProcessOutput();
                    String msg = "Error connecting to python process (" + execMsg + ") " +
                    isAlive + " the output of the process is: " + output;
                    
                    RuntimeException exception = new RuntimeException(msg);
                    dbg(msg, 1);
                    PydevPlugin.log(exception);
                    throw exception;
                }

            } catch (IOException e) {

                if (process != null) {
                    process.destroy();
                    process = null;
                }
                throw e;
            }
        } finally {
            this.inStart = false;
        }
        
        
        //if it got here, everything went ok (otherwise we would have gotten an exception).
        isConnected = true;
    }



    private synchronized void afterCreateProcess() {
        try {
            process.getOutputStream().close(); //we won't write to it...
        } catch (IOException e2) {
        }
        
        //will print things if we are debugging or just get it (and do nothing except emptying it)
        stdReader = new ThreadStreamReader(process.getInputStream());
        errReader = new ThreadStreamReader(process.getErrorStream());
        
        stdReader.setName("Shell reader (stdout)");
        errReader.setName("Shell reader (stderr)");
        
        stdReader.start();
        errReader.start();
    }


    /**
     * @return the current output of the process
     */
    protected synchronized String getProcessOutput(){
        try {
            String output = "";
            output += "Std output:\n" + stdReader.getContents();
            output += "\n\nErr output:\n" + errReader.getContents();
            return output;
        } catch (Exception e) {
            return "Unable to get output";
        }
    }


    /**
     * @param pWrite the port where we should write
     * @param pRead the port where we should read
     * @return the command line that was used to create the process 
     * 
     * @throws IOException
     * @throws JDTNotAvailableException 
     * @throws MisconfigurationException 
     */
    protected abstract String createServerProcess(IInterpreterInfo interpreter, int pWrite, int pRead) throws IOException, JDTNotAvailableException, MisconfigurationException;

    protected synchronized void communicateWork(String desc, IProgressMonitor monitor) {
        if(monitor != null){
            monitor.setTaskName(desc);
            monitor.worked(1);
        }
    }

    public synchronized void clearSocket() throws IOException {
    	long maxTime = System.currentTimeMillis()+(1000*50); //50 secs timeout
    	
        while(System.currentTimeMillis() < maxTime){ //clear until we get no message and timeout is not elapsed
            byte[] b = new byte[AbstractShell.BUFFER_SIZE];
            if(this.socketToRead != null){
                this.socketToRead.getInputStream().read(b);
        
                String s = new String(b);
                s = s.replaceAll((char)0+"",""); //python sends this char as payload.
                if(s.length() == 0){
                    return;
                }
            }else{
            	//if we have no socket, simply return (nothing to clear)
            	return;
            }
        }        
    }

    /**
     * @param operation
     * @return
     * @throws IOException
     */
    public synchronized String read(IProgressMonitor monitor) throws IOException {
        if(finishedForGood){
            throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to read from it.");
        }
        if(inStart){
            throw new RuntimeException("The shell is still not completely started, so, it is an invalid state to try to read from it..");
        }
        if(!isConnected){
            throw new RuntimeException("The shell is still not connected, so, it is an invalid state to try to read from it..");
        }
        if(isInRead){
            throw new RuntimeException("The shell is already in read mode, so, it is an invalid state to try to read from it..");
        }
        if(isInWrite){
            throw new RuntimeException("The shell is already in write mode, so, it is an invalid state to try to read from it..");
        }
        
        isInRead = true;

        try {
            StringBuffer str = new StringBuffer();
            int j = 0;
            while (j < 200) {
                byte[] b = new byte[AbstractShell.BUFFER_SIZE];

                this.socketToRead.getInputStream().read(b);

                String s = new String(b);

                //processing without any status to present to the user
                if (s.indexOf("@@PROCESSING_END@@") != -1) { //each time we get a processing message, reset j to 0.
                    s = s.replaceAll("@@PROCESSING_END@@", "");
                    j = 0;
                    communicateWork("Processing...", monitor);
                }

                //processing with some kind of status
                if (s.indexOf("@@PROCESSING:") != -1) { //each time we get a processing message, reset j to 0.
                    s = s.replaceAll("@@PROCESSING:", "");
                    s = s.replaceAll("END@@", "");
                    j = 0;
                    s = URLDecoder.decode(s, ENCODING_UTF_8);
                    if (s.trim().equals("") == false) {
                        communicateWork("Processing: " + s, monitor);
                    } else {
                        communicateWork("Processing...", monitor);
                    }
                    s = "";
                }

                s = s.replaceAll((char) 0 + "", ""); //python sends this char as payload.
                str.append(s);

                if (str.indexOf("END@@") != -1) {
                    break;
                } else {

                    if (s.length() == 0) { //only raise if nothing was received.
                        j++;
                    } else {
                        j = 0; //we are receiving, even though that may take a long time if the namespace is really polluted...
                    }
                    sleepALittle(10);
                }

            }

            String ret = str.toString().replaceFirst("@@COMPLETIONS", "");
            //remove END@@
            try {
                if (ret.indexOf("END@@") != -1) {
                    ret = ret.substring(0, ret.indexOf("END@@"));
                    return ret;
                } else {
                    throw new RuntimeException("Couldn't find END@@ on received string.");
                }
            } catch (RuntimeException e) {
                PydevPlugin.log(e);
                if (ret.length() > 500) {
                    ret = ret.substring(0, 499) + "...(continued)...";//if the string gets too big, it can crash Eclipse...
                }
                PydevPlugin.log(IStatus.ERROR, "ERROR WITH STRING:" + ret, e);
                return "";
            }
        } finally{
            isInRead = false;
        }
    }

    /**
     * @return s string with the contents read.
     * @throws IOException
     */
    protected synchronized String read() throws IOException {
        String r = read(null);
        //System.out.println("RETURNING:"+URLDecoder.decode(URLDecoder.decode(r,ENCODING_UTF_8),ENCODING_UTF_8));
        return r;
    }

    /**
     * @param str
     * @throws IOException
     */
    public synchronized void write(String str) throws IOException {
        if(finishedForGood){
            throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to write to it.");
        }
        if(inStart){
            throw new RuntimeException("The shell is still not completely started, so, it is an invalid state to try to write to it.");
        }
        if(!isConnected){
            throw new RuntimeException("The shell is still not connected, so, it is an invalid state to try to write to it.");
        }
        if(isInRead){
            throw new RuntimeException("The shell is already in read mode, so, it is an invalid state to try to write to it.");
        }
        if(isInWrite){
            throw new RuntimeException("The shell is already in write mode, so, it is an invalid state to try to write to it.");
        }
        
        isInWrite = true;

        //dbg("WRITING:"+str);
        try {
            OutputStream outputStream = this.socketToWrite.getOutputStream();
            outputStream.write(str.getBytes());
            outputStream.flush();
        } finally {
            isInWrite = false;
        }
    }

    /**
     * @throws IOException
     */
    private synchronized void closeConn() throws IOException {
//let's not send a message... just close the sockets and kill it
//        try {
//            write("@@KILL_SERVER_END@@");
//        } catch (Exception e) {
//        }
        try {
            if (socketToWrite != null) {
                socketToWrite.close();
            }
        } catch (Exception e) {
        }
        socketToWrite = null;
        
        try {
            if (socketToRead != null) {
                socketToRead.close();
            }
        } catch (Exception e) {
        }
        socketToRead = null;
        
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception e) {
        }
        serverSocket = null;
    }

    /**
     * this function should be used with care... it only destroys our processes without closing the
     * connections correctly (intended for shutdowns)
     */
    public synchronized void shutdown() {
        socketToRead = null;
        socketToWrite = null;
        serverSocket = null;
        if (process!= null){
            process.destroy();
            process = null;
        }
    }

    
    /**
     * Kill our sub-process.
     * @throws IOException
     */
    public synchronized void endIt() {
        try {
            closeConn();
        } catch (Exception e) {
            //that's ok...
        }

        //set that we are still not connected
        isConnected = false;
        
        if (process!= null){
            process.destroy();
            process = null;
        }
    }


    /**
     * @return list with tuples: new String[]{token, description}
     * @throws CoreException
     */
    public synchronized Tuple<String, List<String[]>> getImportCompletions(String str, List<String> pythonpath) throws CoreException {
        while(isInOperation){
            sleepALittle(25);
        }
        isInOperation = true;
        try {
            internalChangePythonPath(pythonpath);

            try {
                str = URLEncoder.encode(str, ENCODING_UTF_8);
                return this.getTheCompletions("@@IMPORTS:" + str + "\nEND@@");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            isInOperation = false;
        }
    }

    /**
     * @param pythonpath
     * @throws CoreException
     */
    public synchronized void changePythonPath(List<String> pythonpath) throws CoreException {
        while(isInOperation){
            sleepALittle(25);
        }
        isInOperation = true;
        try {
            internalChangePythonPath(pythonpath); 
        } finally {
            isInOperation = false;
        } 
    }


    /**
     * @param pythonpath
     */
    private void internalChangePythonPath(List<String> pythonpath) {
        if(finishedForGood){
            throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to change its dir.");
        }
        StringBuffer buffer = new StringBuffer();
        for (Iterator<String> iter = pythonpath.iterator(); iter.hasNext();) {
            String path = iter.next();
            buffer.append(path);
            buffer.append("|");
        }
        try {
            getTheCompletions("@@CHANGE_PYTHONPATH:" + URLEncoder.encode(buffer.toString(), ENCODING_UTF_8) + "\nEND@@");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected synchronized Tuple<String, List<String[]>> getTheCompletions(String str) throws CoreException {
        try {
            this.write(str);
    
            return getCompletions();
        } catch (NullPointerException e) {
            //still not started...
            restartShell();
            return getInvalidCompletion();
            
        } catch (Exception e) {
            if(DebugSettings.DEBUG_CODE_COMPLETION){
                PydevPlugin.log(IStatus.ERROR, "ERROR getting completions.", e);
            }
            
            restartShell();
            return getInvalidCompletion();
        }
    }

    /**
     * @throws CoreException
     * 
     */
    public synchronized void restartShell() throws CoreException {
        if(!isInRestart){// we don't want to end up in a loop here...
            isInRestart = true;
            try {
                if (finishedForGood) {
                    throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to restart a new shell.");
                }

                try {
                    this.endIt();
                } catch (Exception e) {
                }
                try {
                    synchronized (this) {
                        this.startIt(shellInterpreter, shellMillis);
                    }
                } catch (Exception e) {
                    PydevPlugin.log(IStatus.ERROR, "ERROR restarting shell.", e);
                }
            } finally {
                isInRestart = false;
            }
        }
    }

    /**
     * @return
     */
    protected synchronized Tuple<String, List<String[]>> getInvalidCompletion() {
        List<String[]> l = new ArrayList<String[]>();
        return new Tuple<String, List<String[]>>(null, l);
    }

    /**
     * @throws IOException
     */
    protected synchronized Tuple<String, List<String[]>> getCompletions() throws IOException {
        ArrayList<String[]> list = new ArrayList<String[]>();
        String read = this.read();
		String string = read.replaceAll("\\(","").replaceAll("\\)","");
        StringTokenizer tokenizer = new StringTokenizer(string, ",");
        
        //the first token is always the file for the module (no matter what)
        String file = "";
        if(tokenizer.hasMoreTokens()){
            file = URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8);
            while(tokenizer.hasMoreTokens()){
                String token = URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8);
                if(!tokenizer.hasMoreTokens()){
                    return new Tuple<String, List<String[]>>(file, list);
                }
                String description = URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8);
                
                String args = "";
                if(tokenizer.hasMoreTokens()){
                    args = URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8);
                }
                
                String type = TYPE_UNKNOWN_STR;
                if(tokenizer.hasMoreTokens()){
                    type = URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8);
                }
      
                //dbg(token);
                //dbg(description);
    
                if(!token.equals("ERROR:")){
                    list.add(new String[]{token, description, args, type});
                }else{
                	if(DebugSettings.DEBUG_CODE_COMPLETION){
                		Log.addLogLevel();
                		try {
							Log.toLogFile("Code completion shell error:", AbstractShell.class);
							Log.toLogFile(token, AbstractShell.class);
							Log.toLogFile(description, AbstractShell.class);
							Log.toLogFile(args, AbstractShell.class);
							Log.toLogFile(type, AbstractShell.class);
						} finally {
							Log.remLogLevel();
						}
                	}
                }
                
            }
        }
        return new Tuple<String, List<String[]>>(file, list);
    }


    /**
     * @param moduleName the name of the module where the token is defined
     * @param token the token we are looking for
     * @return the file where the token was defined, its line and its column (or null if it was not found)
     */
    public synchronized Tuple<String[],int []> getLineCol(String moduleName, String token, List<String> pythonpath) {
        while(isInOperation){
            sleepALittle(25);
        }
        isInOperation = true;
        try {
            String str = moduleName+"."+token;
            internalChangePythonPath(pythonpath);

            try {
                str = URLEncoder.encode(str, ENCODING_UTF_8);
                Tuple<String,List<String[]>> theCompletions = this.getTheCompletions("@@SEARCH" + str + "\nEND@@");
                
                List<String[]> def = theCompletions.o2;
                if(def.size() == 0){
                    return null;
                }

                String[] comps = def.get(0);
                if(comps.length == 0){
                    return null;
                }
                    
                int line = Integer.parseInt(comps[0]);
                int col = Integer.parseInt(comps[1]);
                
                String foundAs = comps[2];
                return new Tuple<String[], int[]>(
                        new String[]{theCompletions.o1, foundAs}, 
                        new int[]{line, col});
                
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            isInOperation = false;
        }
    }


}
