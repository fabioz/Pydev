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
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
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
    protected static final int DEFAULT_SLEEP_BETWEEN_ATTEMPTS = 1000;
    protected static final int DEBUG_SHELL = 2;
    
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
    
    /**
     * Lock to know if there is someone already using this shell for some operation
     */
    private boolean isInOperation = false;

    private void dbg(Object string, int priority) {
        if(priority <= DEBUG_SHELL){
            System.out.println(string);
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
     * we have a 'related to' id as the first step, according to the IPythonNature constants
     * 
     * @see org.python.pydev.core.IPythonNature#PYTHON_RELATED
     * @see org.python.pydev.core.IPythonNature#JYTHON_RELATED
     * 
     * and then we have the id with the shell type that points to the actual shell
     * 
     * @see #COMPLETION_SHELL
     * @see #OTHERS_SHELL
     */
    protected static Map<Integer,Map<Integer,AbstractShell>> shells = new HashMap<Integer,Map<Integer,AbstractShell>>();
    
    /**
     * if we are already finished for good, we may not start new shells (this is a static, because this 
     * should be set only at shutdown).
     */
    private static boolean finishedForGood = false;
    
    /**
     * simple stop of a shell (it may be later restarted)
     */
    public synchronized static void stopServerShell(int relatedId, int id) {
        Map<Integer, AbstractShell> typeToShell = getTypeToShellFromId(relatedId);
        AbstractShell pythonShell = (AbstractShell) typeToShell.get(new Integer(id));
        
        if(pythonShell != null){
            try {
                pythonShell.endIt();
            } catch (Exception e) {
                // ignore... we are ending it anyway...
            }
        }
    }

    
    /**
     * stops all registered shells 
     *
     */
    public synchronized static void shutdownAllShells(){
    	synchronized(shells){
	        for (Iterator<Map<Integer, AbstractShell>> iter = shells.values().iterator(); iter.hasNext();) {
	            finishedForGood = true;  //we may no longer restart shells
	            
	            Map<Integer,AbstractShell> rel = (Map<Integer, AbstractShell>) iter.next();
	            if(rel != null){
	                for (Iterator iter2 = rel.values().iterator(); iter.hasNext();) {
	                    AbstractShell element = (AbstractShell) iter2.next();
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
     * @param relatedId the id that is related to the structure we want to get
     * @return a map with the type of the shell mapping to the shell itself
     */
    private synchronized static Map<Integer, AbstractShell> getTypeToShellFromId(int relatedId) {
    	synchronized(shells){
	        Map<Integer, AbstractShell> typeToShell = shells.get(relatedId);
	        
	        if (typeToShell == null) {
	            typeToShell = new HashMap<Integer, AbstractShell>();
	            shells.put(relatedId, typeToShell);
	        }
	        return typeToShell;
    	}
    }

    /**
     * register a shell and give it an id
     * @param id the shell id
     * @param shell the shell to register
     * 
     * @see org.python.pydev.core.IPythonNature#PYTHON_RELATED
     * @see org.python.pydev.core.IPythonNature#JYTHON_RELATED
     * 
     * @see #COMPLETION_SHELL
     * @see #OTHERS_SHELL
     */
    public synchronized static void putServerShell(IPythonNature nature, int id, AbstractShell shell) {
        try {
            Map<Integer, AbstractShell> typeToShell = getTypeToShellFromId(nature.getRelatedId());
            typeToShell.put(new Integer(id), shell);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public synchronized static AbstractShell getServerShell(IPythonNature nature, int id) throws IOException, Exception {
        return getServerShell(nature.getRelatedId(), id);
    }
    
    /**
     * @return the shell with the given id related to some nature
     * 
     * @see org.python.pydev.core.IPythonNature#PYTHON_RELATED
     * @see org.python.pydev.core.IPythonNature#JYTHON_RELATED
     * 
     * @see #COMPLETION_SHELL
     * @see #OTHERS_SHELL
     * 
     * @throws CoreException
     * @throws IOException
     */
    public synchronized static AbstractShell getServerShell(int relatedId, int id) throws IOException, Exception {
    	synchronized(shells){
	        Map<Integer, AbstractShell> typeToShell = getTypeToShellFromId(relatedId);
	        AbstractShell pythonShell = (AbstractShell) typeToShell.get(new Integer(id));
	        
	        if(pythonShell == null){
	            if(relatedId == IPythonNature.PYTHON_RELATED){
	                pythonShell = new PythonShell();
	            }else if(relatedId == IPythonNature.JYTHON_RELATED){
	                pythonShell = new JythonShell();
	            }else{
	                throw new RuntimeException("unknown related id");
	            }
	            synchronized(pythonShell){
	            	pythonShell.startIt(); //first start it
	            }
	            
	            //then make it accessible
	            typeToShell.put(new Integer(id), pythonShell);
	        }
            
            //if the shell is still starting, we will not return it.
            while(pythonShell.inStart){
                pythonShell.sleepALittle(200);
            }
	        return pythonShell;
    	}
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
     */
    public synchronized void startIt() throws IOException, Exception {
    	synchronized(this){
    		this.startIt(AbstractShell.DEFAULT_SLEEP_BETWEEN_ATTEMPTS);
    	}
    }


    /**
     * This method creates the python server process and starts the sockets, so that we
     * can talk with the server.
     * 
     * @param milisSleep: time to wait after creating the process.
     * @throws IOException is some error happens creating the sockets - the process is terminated.
     * @throws CoreException
     */
    protected synchronized void startIt(int milisSleep) throws IOException, Exception {
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

				int pWrite = SocketUtil.findUnusedLocalPort("127.0.0.1", 50000, 55000);
				int pRead = SocketUtil.findUnusedLocalPort("127.0.0.1", 55001, 60000);

				if (process != null) {
					endIt(); //end the current process
				}

				String execMsg = createServerProcess(pWrite, pRead);
				dbg("executing " + execMsg,1);

				sleepALittle(200);
				String osName = System.getProperty("os.name");
				if (process == null) {
					String msg = "Error creating python process - got null process(" + execMsg + ") - os:" + osName;
                    PydevPlugin.log(msg);
					throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, msg, new Exception(msg)));
				}
				try {
					int exitVal = process.exitValue(); //should throw exception saying that it still is not terminated...
					String msg = "Error creating python process - exited before creating sockets - exitValue = ("
							+ exitVal + ")(" + execMsg + ") - os:" + osName;
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
				serverSocket = new ServerSocket(pRead); //read in this port
				int maxAttempts = PyCodeCompletionPreferencesPage.getNumberOfConnectionAttempts();
				while (!connected && attempts < maxAttempts && !finishedForGood) {
					attempts += 1;
					try {
						if (socketToWrite == null || socketToWrite.isConnected() == false) {
							socketToWrite = new Socket("127.0.0.1", pWrite); //we should write in this port
						}

						if (socketToWrite != null || socketToWrite.isConnected()) {
							serverSocket.setSoTimeout(milisSleep * 2); //let's give it a higher timeout, as we're already half - connected
							try {
								socketToRead = serverSocket.accept();
								connected = true;
								dbg("connected! ",1);
							} catch (SocketTimeoutException e) {
								//that's ok, timeout for waiting connection expired, let's check it again in the next loop
							}
						}
					} catch (IOException e1) {
						if (socketToWrite != null && socketToWrite.isConnected() == true) {
							PydevPlugin.log(IStatus.ERROR, "Attempt: " + attempts + " of " + maxAttempts
									+ " failed, trying again...(socketToWrite already binded)", e1);
						}
						if (socketToWrite != null && !socketToWrite.isConnected() == true) {
						    PydevPlugin.log(IStatus.ERROR, "Attempt: " + attempts + " of " + maxAttempts
						            + " failed, trying again...(socketToWrite still not binded)", e1);
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
					Exception exception = new Exception("Error connecting to python process (" + execMsg + ") "
							+ isAlive + " the output of the process is: " + output);
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

        stdReader.start();
        errReader.start();
    }


    /**
     * @return the current output of the process
     */
    protected synchronized String getProcessOutput(){
        try {
            String output = "";
            output += "Std output:\n" + stdReader.contents.toString();
            output += "\n\nErr output:\n" + errReader.contents.toString();
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
     */
    protected abstract String createServerProcess(int pWrite, int pRead) throws IOException;

    protected synchronized void communicateWork(String desc, Operation operation) {
        if(operation != null){
            operation.monitor.setTaskName(desc);
            operation.monitor.worked(1);
        }
    }

    public synchronized void clearSocket() throws IOException {
    	while(true){ //clear until we get no message...
	        byte[] b = new byte[AbstractShell.BUFFER_SIZE];
	        this.socketToRead.getInputStream().read(b);
	
	        String s = new String(b);
	        s = s.replaceAll((char)0+"",""); //python sends this char as payload.
	        if(s.length() == 0){
	        	return;
	        }
    	}        
    }

    /**
     * @param operation
     * @return
     * @throws IOException
     */
    public synchronized String read(Operation operation) throws IOException {
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
            String tempStr = "";
            int j = 0;
            while (j != 100) {
                byte[] b = new byte[AbstractShell.BUFFER_SIZE];

                this.socketToRead.getInputStream().read(b);

                String s = new String(b);

                //processing without any status to present to the user
                if (s.indexOf("@@PROCESSING_END@@") != -1) { //each time we get a processing message, reset j to 0.
                    s = s.replaceAll("@@PROCESSING_END@@", "");
                    j = 0;
                    communicateWork("Processing...", operation);
                }

                //processing with some kind of status
                if (s.indexOf("@@PROCESSING:") != -1) { //each time we get a processing message, reset j to 0.
                    s = s.replaceAll("@@PROCESSING:", "");
                    s = s.replaceAll("END@@", "");
                    j = 0;
                    s = URLDecoder.decode(s, ENCODING_UTF_8);
                    if (s.trim().equals("") == false) {
                        communicateWork("Processing: " + s, operation);
                    } else {
                        communicateWork("Processing...", operation);
                    }
                    s = "";
                }

                s = s.replaceAll((char) 0 + "", ""); //python sends this char as payload.
                str.append(s);

                if (str.indexOf("END@@") != -1) {
                    break;
                } else {

                    if (tempStr.equals(str) == true) { //only raise if nothing was received.
                        j++;
                    } else {
                        j = 0; //we are receiving, even though that may take a long time if the namespace is really polluted...
                    }
                    sleepALittle(10);
                    tempStr = str.toString();
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
                e.printStackTrace();
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
        //dbg("RETURNING:"+URLDecoder.decode(URLDecoder.decode(r,ENCODING_UTF_8),ENCODING_UTF_8));
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

    public synchronized void sendGoToDirMsg(File file) {
        while(isInOperation){
            sleepALittle(100);
        }
        isInOperation = true;
        try {
            if (finishedForGood) {
                throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to change the shell dir.");
            }
            checkShell();

            try {
                if (file.isDirectory() == false) {
                    file = file.getParentFile();
                }

                String str = REF.getFileAbsolutePath(file);
                str = URLEncoder.encode(str, ENCODING_UTF_8);
                this.write("@@CHANGE_DIR:" + str + "END@@");
                this.read(); //this should be the ok message...

            } catch (IOException e) {
                try {
                    restartShell();
                } catch (Exception e1) {

                }
                PydevPlugin.log(IStatus.ERROR, "ERROR sending go to dir msg.", e);
            }
        } finally {
            isInOperation = false;
        }
    }

    private synchronized void checkShell() {
        try {
            if (this.socketToWrite == null || !this.socketToWrite.isBound() || 
                this.socketToRead == null  || !this.socketToRead.isBound()  ||
                this.serverSocket == null  || !this.serverSocket.isBound()) {
                restartShell();
            }
        } catch (Exception e) {
            // ok
        }
    }


    /**
     * @return list with tuples: new String[]{token, description}
     * @throws CoreException
     */
    public synchronized Tuple<String, List<String[]>> getImportCompletions(String str, List pythonpath) throws CoreException {
        while(isInOperation){
            sleepALittle(100);
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
    public synchronized void changePythonPath(List pythonpath) throws CoreException {
        while(isInOperation){
            sleepALittle(100);
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
    private void internalChangePythonPath(List pythonpath) {
        if(finishedForGood){
            throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to change its dir.");
        }
        StringBuffer buffer = new StringBuffer();
        for (Iterator iter = pythonpath.iterator(); iter.hasNext();) {
            String path = (String) iter.next();
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
            PydevPlugin.log(IStatus.ERROR, "ERROR getting completions.", e);
    
            restartShell();
            return getInvalidCompletion();
        }
    }

    /**
     * @throws CoreException
     * 
     */
    public synchronized void restartShell() throws CoreException {
        if(finishedForGood){
            throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to restart a new shell.");
        }

        try {
            this.endIt();
        } catch (Exception e) {
        }
        try {
        	synchronized(this){
        		this.startIt();
        	}
        } catch (Exception e) {
            PydevPlugin.log(IStatus.ERROR, "ERROR restarting shell.", e);
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
        String string = this.read().replaceAll("\\(","").replaceAll("\\)","");
        StringTokenizer tokenizer = new StringTokenizer(string, ",");
        
        //the first token is always the file for the module (no matter what)
        String file = URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8);
        while(tokenizer.hasMoreTokens()){
            String token       = URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8);
            if(!tokenizer.hasMoreTokens()){
                return new Tuple<String, List<String[]>>(file, list);
            }
            String description = URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8);
            
            String args = "";
            if(tokenizer.hasMoreTokens())
                args = URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8);
            
            String type =""+PyCodeCompletion.TYPE_UNKNOWN;
            if(tokenizer.hasMoreTokens())
                type = URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8);
  
            //dbg(token);
            //dbg(description);

            if(!token.equals("ERROR:")){
                list.add(new String[]{token, description, args, type});
            }
        }
        return new Tuple<String, List<String[]>>(file, list);
    }


    /**
     * @param moduleName the name of the module where the token is defined
     * @param token the token we are looking for
     * @return the file where the token was defined, its line and its column.
     */
    public Tuple<String[],int []> getLineCol(String moduleName, String token, List pythonpath) {
        while(isInOperation){
            sleepALittle(100);
        }
        isInOperation = true;
        try {
            String str = moduleName+"."+token;
            internalChangePythonPath(pythonpath);

            try {
                str = URLEncoder.encode(str, ENCODING_UTF_8);
                Tuple<String,List<String[]>> theCompletions = this.getTheCompletions("@@SEARCH" + str + "\nEND@@");
                int line = Integer.parseInt(theCompletions.o2.get(0)[0]);
                int col = Integer.parseInt(theCompletions.o2.get(0)[1]);
                String foundAs = theCompletions.o2.get(0)[2];
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
