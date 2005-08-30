/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.shell;

import java.io.File;
import java.io.IOException;
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
import org.eclipse.core.runtime.Status;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.SocketUtil;
import org.python.pydev.runners.ThreadStreamReader;

public abstract class AbstractShell {

    public static final int BUFFER_SIZE = 1024 ;
    public static final int OTHERS_SHELL = 2;
    public static final int COMPLETION_SHELL = 1;
    protected static final int DEFAULT_SLEEP_BETWEEN_ATTEMPTS = 500;
    protected static final boolean DEBUG_SHELL = true;


    private void dbg(Object string) {
        if(DEBUG_SHELL){
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
    public static void stopServerShell(int relatedId, int id) {
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

    /**
     * @param relatedId the id that is related to the structure we want to get
     * @return a map with the type of the shell mapping to the shell itself
     */
    private static Map<Integer, AbstractShell> getTypeToShellFromId(int relatedId) {
        Map<Integer, AbstractShell> typeToShell = shells.get(relatedId);
        
        if (typeToShell == null) {
            typeToShell = new HashMap<Integer, AbstractShell>();
            shells.put(relatedId, typeToShell);
        }
        return typeToShell;
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
            typeToShell.put(new Integer(id), pythonShell);
            pythonShell.startIt();
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
    protected void sleepALittle(int t) {
        try {
            synchronized(this){
                wait(t); //millis
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method creates the python server process and starts the sockets, so that we
     * can talk with the server.
     * @throws IOException
     * @throws CoreException
     */
    public void startIt() throws IOException, Exception {
        this.startIt(AbstractShell.DEFAULT_SLEEP_BETWEEN_ATTEMPTS);
    }


    /**
     * This method creates the python server process and starts the sockets, so that we
     * can talk with the server.
     * 
     * @param milisSleep: time to wait after creating the process.
     * @throws IOException is some error happens creating the sockets - the process is terminated.
     * @throws CoreException
     */
    protected void startIt(int milisSleep) throws IOException, Exception {
        if(finishedForGood){
            throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to restart it.");
        }

        try {
    
            int pWrite = SocketUtil.findUnusedLocalPort("127.0.0.1", 50000, 55000);
            int pRead = SocketUtil.findUnusedLocalPort("127.0.0.1", 55001, 60000);
    
            if(process != null){
                endIt(); //end the current process
            }
            
            String execMsg = createServerProcess(pWrite, pRead);
            dbg("executing "+execMsg);
            
            sleepALittle(200);
            String osName = System.getProperty("os.name");
            if(process == null){
                String msg = "Error creating python process - got null process("+execMsg+") - os:"+osName;
                throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, msg, new Exception(msg)));
            }
            try {
                int exitVal = process.exitValue(); //should throw exception saying that it still is not terminated...
                String msg = "Error creating python process - exited before creating sockets - exitValue = ("+exitVal+")("+execMsg+") - os:"+osName;
                throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, msg, new Exception(msg)));
            } catch (IllegalThreadStateException e2) { //this is ok
            }
            
            dbg("afterCreateProcess ");
            //ok, process validated, so, let's get its output and store it for further use.
            afterCreateProcess();
            
            boolean connected = false;
            int attempts = 0;
            
            dbg("connecting ");
            sleepALittle(milisSleep);
            socketToWrite = null;
            serverSocket = new ServerSocket(pRead); //read in this port
            int maxAttempts = PyCodeCompletionPreferencesPage.getNumberOfConnectionAttempts();
            while(!connected && attempts < maxAttempts && !finishedForGood){
                attempts += 1;
                try {
                    if(socketToWrite == null || socketToWrite.isConnected() == false){
                        socketToWrite = new Socket("127.0.0.1",pWrite); //we should write in this port
                    }
                    
                    if(socketToWrite != null || socketToWrite.isConnected()){
                        serverSocket.setSoTimeout(milisSleep*2); //let's give it a higher timeout, as we're already half - connected
                        try {
                            socketToRead = serverSocket.accept();
                            connected = true;
                        } catch (SocketTimeoutException e) {
                            //that's ok, timeout for waiting connection expired, let's check it again in the next loop
                        }
                    }
                } catch (IOException e1) {
                    if(socketToWrite != null && socketToWrite.isConnected() == true){
                        PydevPlugin.log(IStatus.ERROR, "Attempt: "+attempts+" of "+maxAttempts+" failed, trying again...(socketToWrite already binded)", e1);
                    }
                }
                
                //if not connected, let's sleep a little for another attempt
                if(!connected){
                    sleepALittle(milisSleep);
                }
            }
            
            if(!connected && !finishedForGood ){
                dbg("NOT connected ");

                //what, after all this trouble we are still not connected????!?!?!?!
                //let's communicate this to the user...
                String isAlive;
                try {
                    int exitVal = process.exitValue(); //should throw exception saying that it still is not terminated...
                    isAlive = " - the process in NOT ALIVE anymore (output="+exitVal+") - ";
                } catch (IllegalThreadStateException e2) { //this is ok
                    isAlive = " - the process in still alive - ";
                }
                

                String output = getProcessOutput();
                Exception exception = new Exception("Error connecting to python process (" + execMsg + ") "+isAlive+" the output of the process is: "+output);
                try {
                    Status status = PydevPlugin.makeStatus(IStatus.ERROR, "Error connecting to python process (" + execMsg + ") "+isAlive+" the output of the process is: "+output, exception);
                    throw new CoreException(status);
                } catch (Exception e) {
                    throw exception;
                }
            }
            
        } catch (IOException e) {
            
            if(process!=null){
                process.destroy();
                process = null;
            }
            throw e;
        }
    }



    private void afterCreateProcess() {
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
    protected String getProcessOutput(){
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

    protected void communicateWork(String desc, Operation operation) {
        if(operation != null){
            operation.monitor.setTaskName(desc);
            operation.monitor.worked(1);
        }
    }

    /**
     * @param operation
     * @return
     * @throws IOException
     */
    public String read(Operation operation) throws IOException {
        if(finishedForGood){
            throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to read from it.");
        }

        StringBuffer str = new StringBuffer();
        String tempStr = "";
        int j = 0;
        while(j != 100){
            byte[] b = new byte[AbstractShell.BUFFER_SIZE];
    
            this.socketToRead.getInputStream().read(b);
    
            String s = new String(b);
            
            if(s.indexOf("@@PROCESSING_END@@") != -1){ //each time we get a processing message, reset j to 0.
                s = s.replaceAll("@@PROCESSING_END@@", "");
                j = 0;
                communicateWork("Processing...", operation);
            }
            
            
            if(s.indexOf("@@PROCESSING:") != -1){ //each time we get a processing message, reset j to 0.
                s = s.replaceAll("@@PROCESSING:", "");
                s = s.replaceAll("END@@", "");
                j = 0;
                s = URLDecoder.decode(s, ENCODING_UTF_8);
                if(s.trim().equals("") == false){
                    communicateWork("Processing: "+s, operation);
                }else{
                    communicateWork("Processing...", operation);
                }
                s = "";
            }
    
            
            s = s.replaceAll((char)0+"",""); //python sends this char as payload.
            str.append(s);
            
            if(str.indexOf("END@@") != -1){
                break;
            }else{
                
                if(tempStr.equals(str) == true){ //only raise if nothing was received.
                    j++;
                }else{
                    j = 0; //we are receiving, even though that may take a long time if the namespace is really polluted...
                }
                sleepALittle(10);
                tempStr = str.toString();
            }
            
        }
        
        String ret = str.toString().replaceFirst("@@COMPLETIONS","");
        //remove END@@
        try {
            if(ret.indexOf("END@@")!= -1){
                ret = ret.substring(0, ret.indexOf("END@@"));
                return ret;
            }else{
                throw new RuntimeException("Couldn't find END@@ on received string.");
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            if(ret.length() > 500){
                ret = ret.substring(0, 499)+"...(continued)...";//if the string gets too big, it can crash Eclipse...
            }
            PydevPlugin.log(IStatus.ERROR, "ERROR WITH STRING:"+ret, e);
            return "";
        }
    }

    /**
     * @return s string with the contents read.
     * @throws IOException
     */
    protected String read() throws IOException {
        String r = read(null);
        //dbg("RETURNING:"+URLDecoder.decode(URLDecoder.decode(r,ENCODING_UTF_8),ENCODING_UTF_8));
        return r;
    }

    /**
     * @param str
     * @throws IOException
     */
    public void write(String str) throws IOException {
        if(finishedForGood){
            throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to write to it.");
        }
        //dbg("WRITING:"+str);
        this.socketToWrite.getOutputStream().write(str.getBytes());
    }

    /**
     * @throws IOException
     */
    private void closeConn() throws IOException {
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
    public void shutdown() {
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
    public void endIt() {
        
        try {
            closeConn();
        } catch (Exception e) {
            //that's ok...
        }
        if (process!= null){
            process.destroy();
            process = null;
        }
        
    }

    public void sendGoToDirMsg(File file) {
        if(finishedForGood){
            throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to change the shell dir.");
        }
        checkShell();
        
        try {
            if(file.isDirectory() == false){
                file = file.getParentFile();
            }
            
            String str = REF.getFileAbsolutePath(file);
            str = URLEncoder.encode(str, ENCODING_UTF_8);
            this.write("@@CHANGE_DIR:"+str+"END@@");
            this.read(); //this should be the ok message...
            
        } catch (IOException e) {
            try {
                restartShell();
            } catch (Exception e1) {
                
            }
            PydevPlugin.log(IStatus.ERROR, "ERROR sending go to dir msg.", e);
        }
    }

    private void checkShell() {
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
    public List getImportCompletions(String str, List pythonpath) throws CoreException {
        changePythonPath(pythonpath);
        
        try {
            str = URLEncoder.encode(str, ENCODING_UTF_8);
            return this.getTheCompletions("@@IMPORTS:" + str + "\nEND@@");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param pythonpath
     * @throws CoreException
     */
    public void changePythonPath(List pythonpath) throws CoreException {
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

    protected List getTheCompletions(String str) throws CoreException {
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
    public void restartShell() throws CoreException {
        if(finishedForGood){
            throw new RuntimeException("Shells are already finished for good, so, it is an invalid state to try to restart a new shell.");
        }

        try {
            this.endIt();
        } catch (Exception e) {
        }
        try {
            this.startIt();
        } catch (Exception e) {
            PydevPlugin.log(IStatus.ERROR, "ERROR restarting shell.", e);
        }
    }

    /**
     * @return
     */
    protected List getInvalidCompletion() {
        List<String[]> l = new ArrayList<String[]>();
        return l;
    }

    /**
     * @throws IOException
     */
    protected List getCompletions() throws IOException {
        ArrayList<String[]> list = new ArrayList<String[]>();
        String string = this.read().replaceAll("\\(","").replaceAll("\\)","");
        StringTokenizer tokenizer = new StringTokenizer(string, ",");
        
        while(tokenizer.hasMoreTokens()){
            String token       = URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8);
            if(!tokenizer.hasMoreTokens()){
                return list;
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
        return list;
    }


}
