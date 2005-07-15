/*
 * Created on Aug 16, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
import org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.SocketUtil;
import org.python.pydev.utils.SimplePythonRunner;
import org.python.pydev.utils.ref.REF;

/**
 * @author Fabio Zadrozny
 */
public class PythonShell {

    private static final int DEFAULT_SLEEP_BETWEEN_ATTEMPTS = 1000;
    /**
     * Reference to 'global python shells'
     */
    private static Map shells = new HashMap();
    
    public static final int COMPLETION_SHELL = 1; 
    public static final int OTHERS_SHELL = 2; 

    public synchronized static void stopAllShells(){
        
        for (Iterator iter = shells.values().iterator(); iter.hasNext();) {
            PythonShell element = (PythonShell) iter.next();
            if(element != null){
                element.endIt();
            }
        }
        shells.clear();
    }

    public synchronized static void putServerShell(int id, PythonShell shell) {
        shells.put(new Integer(id), shell);
    }
    
    /**
     * @return
     * @throws CoreException
     * @throws IOException
     * 
     */
    public synchronized static PythonShell getServerShell(int id) throws IOException, Exception {
        PythonShell pytonShell = (PythonShell) shells.get(new Integer(id));
        
        if(pytonShell == null){
            pytonShell = new PythonShell();
            shells.put(new Integer(id), pytonShell);
            pytonShell.startIt();
        }
        return pytonShell;
    }

    
    public static final int BUFFER_SIZE = 1024 ;
    /**
     * Python server process.
     */
    public Process process;
    
    /**
     * We should write in this socket.
     */
    private Socket socketToWrite;
    
    /**
     * We should read this socket.
     */
    private Socket socketToRead;
    
    /**
     * Python file that works as the server.
     */
    private File serverFile;

    /**
     * Server socket (accept connections).
     */
    private ServerSocket serverSocket;


    /**
     * Initialize given the file that points to the python server (execute it
     * with python).
     *  
     * @param f file pointing to the python server
     * 
     * @throws IOException
     * @throws CoreException
     */
    public PythonShell(File f) throws IOException, CoreException {
        serverFile = f;
        if(!serverFile.exists()){
            throw new RuntimeException("Can't find python server file");
        }
    }
    
    
    /**
     * Initialize with the default python server file.
     * 
     * @throws IOException
     * @throws CoreException
     */
    public PythonShell() throws IOException, CoreException {
        this(PydevPlugin.getScriptWithinPySrc("pycompletionserver.py"));
    }

    
    /**
     * Just wait a little...
     */
    private void sleepALittle(int t) {
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
     * 
     * @param milisSleep: time to wait after creating the process.
     * @throws IOException is some error happens creating the sockets - the process is terminated.
     * @throws CoreException
     */
    public void startIt(int milisSleep) throws IOException, Exception{
        try {

            int pWrite = SocketUtil.findUnusedLocalPort("127.0.0.1", 50000, 55000);
            int pRead = SocketUtil.findUnusedLocalPort("127.0.0.1", 55001, 60000);

            if(process != null)
                endIt();
            String interpreter = PydevPlugin.getInterpreterManager().getDefaultInterpreter();
            String osName = System.getProperty("os.name");
            
            String execMsg;
            if(osName.toLowerCase().indexOf("win") != -1){ //in windows, we have to put python "path_to_file.py"
                execMsg = interpreter+" \""+REF.getFileAbsolutePath(serverFile)+"\" "+pWrite+" "+pRead;
            }else{ //however in mac, this gives an error...
                execMsg = interpreter+" "+REF.getFileAbsolutePath(serverFile)+" "+pWrite+" "+pRead;
            }

            //System.out.println(execMsg);
            process = SimplePythonRunner.createProcess(execMsg, serverFile.getParentFile());
            
            sleepALittle(200);
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
            
            boolean connected = false;
            int attempts = 0;
            
            sleepALittle(300);
            socketToWrite = null;
            serverSocket = new ServerSocket(pRead); //read in this port
            int maxAttempts = PyCodeCompletionPreferencesPage.getNumberOfConnectionAttempts();
            while(!connected && attempts < maxAttempts){
                attempts += 1;
	            try {
	                if(socketToWrite == null || socketToWrite.isConnected() == false){
	                    socketToWrite = new Socket("127.0.0.1",pWrite); //we should write in this port
	                }
                    socketToRead = serverSocket.accept();
                    connected = true;
                } catch (IOException e1) {
	                if(socketToWrite != null && socketToWrite.isConnected() == true){
	                    PydevPlugin.log(IStatus.ERROR, "Attempt: "+attempts+" of "+maxAttempts+" failed, trying again...(socketToWrite already binded)", e1);
	                }
//	                no need for showing the error below...
//	                else{
//	                    PydevPlugin.log(IStatus.ERROR, "Attempt: "+attempts+" of "+maxAttempts+" failed, trying again...", e1);
//	                }
	                
                }
                
                //if not connected, let's sleep a little for another attempt
                if(!connected){
                    sleepALittle(milisSleep);
                }
            }
            
            if(!connected){
                //what, after all this trouble we are still not connected????!?!?!?!
                //let's communicate this to the user...
                Exception exception = new Exception("Error connecting to python process.");
                try {
                    Status status = PydevPlugin.makeStatus(IStatus.ERROR, "Error connecting to python process (" + execMsg + ")", exception);
	                throw new CoreException(status);
                } catch (Exception e) {
                    throw exception;
                }
            }
            
        } catch (IOException e) {
            
            if(process!=null){
                process.destroy();
            }
            process = null;
            throw e;
        }
    }
    
    /**
     * This method creates the python server process and starts the sockets, so that we
     * can talk with the server.
     * @throws IOException
     * @throws CoreException
     */
    public void startIt() throws IOException, Exception{
        this.startIt(DEFAULT_SLEEP_BETWEEN_ATTEMPTS);
    }

    
    private void communicateWork(String desc, Operation operation){
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
        StringBuffer str = new StringBuffer();
        String tempStr = "";
        int j = 0;
        while(j != 100){
	        byte[] b = new byte[PythonShell.BUFFER_SIZE];

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
                s = URLDecoder.decode(s, "UTF-8");
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
    public String read() throws IOException {
        String r = read(null);
//        System.out.println("RETURNING:"+URLDecoder.decode(URLDecoder.decode(r)));
        return r;
    }
    

    /**
     * @param str
     * @throws IOException
     */
    public void write(String str) throws IOException {
//        System.out.println("WRITING:"+str);
        this.socketToWrite.getOutputStream().write(str.getBytes());
    }

    
    /**
     * @throws IOException
     */
    private void closeConn() throws IOException {
        write("@@KILL_SERVER_END@@");
        if(socketToWrite != null){
	        socketToWrite.close();
        }
        socketToWrite = null;
        
        if(socketToRead != null){
            socketToRead.close();
        }
        socketToRead = null;
        
        if(serverSocket != null){
            serverSocket.close();
        }
        serverSocket = null;
    }


    /**
     * Kill our sub-process.
     * @throws IOException
     */
    public void endIt() {
        
        try {
            closeConn();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (process!= null){
            try {
                process.getOutputStream().close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            try {
                process.getErrorStream().close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            try {
                process.getInputStream().close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
                
            try {
                process.destroy();
            } catch (Exception e2) {
                e2.printStackTrace();
            }

            process = null;
        }
        
    }

    public void sendGoToDirMsg(File file){
        try {
            if(file.isDirectory() == false){
                file = file.getParentFile();
            }
            
            String str = REF.getFileAbsolutePath(file);
            str = URLEncoder.encode(str, "UTF-8");
            this.write("@@CHANGE_DIR:"+str+"END@@");
//            String ok = this.read(); //this should be the ok message...
            this.read(); //this should be the ok message...
            
        } catch (IOException e) {
            PydevPlugin.log(IStatus.ERROR, "ERROR sending go to dir msg.", e);
        }
    }
    

    /**
     * 
     * @param importsTipper
     * @return list with tuples: new String[]{token, description}
     * @throws CoreException
     */
    public List getImportCompletions(String str, List pythonpath) throws CoreException {
        changePythonPath(pythonpath);
        
        try {
            str = URLEncoder.encode(str, "UTF-8");
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
        StringBuffer buffer = new StringBuffer();
        for (Iterator iter = pythonpath.iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            buffer.append(path);
            buffer.append("|");
        }
        try {
            getTheCompletions("@@CHANGE_PYTHONPATH:" + URLEncoder.encode(buffer.toString(), "UTF-8") + "\nEND@@");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } 
    }

    /**
     * 
     * @return List with String[] (we are interested only in the String[0])
     * @throws CoreException
     */
    public List getPythonPath() throws CoreException{
        return this.getTheCompletions("@@PYTHONPATH_END@@");
    }
    
    private List getTheCompletions(String str) throws CoreException{
        try {
            this.write(str);
 
            return getCompletions();
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
    private List getInvalidCompletion() {
        List l = new ArrayList();
        l.add(new String[]{"SERVER_ERROR","please try again."});
        return l;
    }


    /**
     * @throws IOException
     */
    private List getCompletions() throws IOException {
        ArrayList list = new ArrayList();
        String string = this.read().replaceAll("\\(","").replaceAll("\\)","");
        StringTokenizer tokenizer = new StringTokenizer(string, ",");
        
        while(tokenizer.hasMoreTokens()){
            String token       = URLDecoder.decode(tokenizer.nextToken(), "UTF-8");
            if(!tokenizer.hasMoreTokens()){
                return list;
            }
            String description = URLDecoder.decode(tokenizer.nextToken(), "UTF-8");
            
            String args = "";
            if(tokenizer.hasMoreTokens())
                args = URLDecoder.decode(tokenizer.nextToken(), "UTF-8");
            
            String type =""+PyCodeCompletion.TYPE_UNKNOWN;
            if(tokenizer.hasMoreTokens())
                type = URLDecoder.decode(tokenizer.nextToken(), "UTF-8");
            
//            System.out.println(token);
//            System.out.println(description);

            if(!token.equals("ERROR:")){
                list.add(new String[]{token, description, args, type});
            }
        }
        return list;
    }






}