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
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.plugin.SocketUtil;

/**
 * @author Fabio Zadrozny
 */
public class PythonShell {

    /**
     * Reference to a 'global python shell'
     */
    private static PythonShell pytonShell;


    /**
     * @return
     * @throws CoreException
     * @throws IOException
     * 
     */
    public synchronized static PythonShell getServerShell() throws IOException, CoreException {
        if(pytonShell == null){
            pytonShell = new PythonShell();
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
    private void sleepALittle() {
        sleepALittle(25); //25 millis
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
    public void startIt(int milisSleep) throws IOException, CoreException{
        try {

            int pWrite = SocketUtil.findUnusedLocalPort("127.0.0.1", 50000, 55000);
            int pRead = SocketUtil.findUnusedLocalPort("127.0.0.1", 55001, 60000);

            if(process != null)
                endIt();
            String interpreter = getDefaultInterpreter();
            String execMsg = interpreter+" \""+serverFile.getAbsolutePath()+"\" "+pWrite+" "+pRead;
            process = Runtime.getRuntime().exec(execMsg);
            
            boolean connected = false;
            int attempts = 0;
            
            sleepALittle(200);
            while(!connected && attempts < 20){
                attempts += 1;
	            try {
                    socketToWrite = new Socket("127.0.0.1",pWrite); //we should write in this port  
                    serverSocket = new ServerSocket(pRead);         //and read in this port 
                    socketToRead = serverSocket.accept();
                    connected = true;
                } catch (IOException e1) {
                    PydevPlugin.log(IStatus.ERROR, "Attempt: "+attempts+" of 20 failed, trying again...", e1);
                }
	            sleepALittle(milisSleep);            
            }
            
            if(!connected){
                //what, after all this trouble we are still not connected????!?!?!?!
                //let's communicate this to the user...
                throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, "Error creating python process ("+execMsg+")", new Exception("Error creating python process.")));
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
     * @return
     */
    protected String getDefaultInterpreter() {
        try {
            return PydevPrefs.getDefaultInterpreter();
        } catch (RuntimeException e) {
            return "python";
        }
    }


    /**
     * This method creates the python server process and starts the sockets, so that we
     * can talk with the server.
     * @throws IOException
     * @throws CoreException
     */
    public void startIt() throws IOException, CoreException{
        this.startIt(25);
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
        String str = "";
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
                s = URLDecoder.decode(s);
                if(s.trim().equals("") == false){
                    communicateWork("Processing: "+s, operation);
                }else{
                    communicateWork("Processing...", operation);
                }
                s = "";
            }

            
            s = s.replaceAll((char)0+"",""); //python sends this char as payload.
            str += s;
            
            if(str.indexOf("END@@") != -1){
                break;
            }else{
                
                if(tempStr.equals(str) == true){ //only raise if nothing was received.
                    j++;
                }else{
                    j = 0; //we are receiving, even though that may take a long time if the namespace is really polluted...
                }
                sleepALittle(10);
                tempStr = str;
            }
            
        }
        
        //remove @@COMPLETIONS
        str = str.replaceFirst("@@COMPLETIONS","");
        //remove END@@
        try {
            if(str.indexOf("END@@")!= -1){
	            str = str.substring(0, str.indexOf("END@@"));
	            return str;
            }else{
                throw new RuntimeException("Couldn't find END@@ on received string.");
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            if(str.length() > 1000){
                str = str.substring(0, 999)+"...(continued)...";//if the string gets too big, it can crash Eclipse...
            }
            PydevPlugin.log(IStatus.ERROR, "ERROR WITH STRING:"+str, e);
            return "";
        }
    }


    /**
     * @return s string with the contents read.
     * @throws IOException
     */
    public String read() throws IOException {
        String r = read(null);
//        System.out.println("RETURNING:"+r);
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
                int i = process.getErrorStream().available();
                byte b[] = new byte[i];
                process.getErrorStream().read(b);
                System.out.println(new String(b));
                
                i = process.getInputStream().available();
                b = new byte[i];
                process.getInputStream().read(b);
                System.out.println(new String(b));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
                
            try {
                process.destroy();
            } catch (Exception e2) {
                e2.printStackTrace();
            }

            try {
                process.waitFor();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            process = null;
        }
        
    }

    public void sendGoToDirMsg(File file){
        try {
            if(file.isDirectory() == false){
                file = file.getParentFile();
            }
            
            String str = file.getAbsolutePath();
            str = URLEncoder.encode(str);
            this.write("@@CHANGE_DIR:"+str+"END@@");
            String ok = this.read(); //this should be the ok message...
            
        } catch (IOException e) {
            PydevPlugin.log(IStatus.ERROR, "ERROR sending go to dir msg.", e);
        }
    }
    
    public void sendReloadModulesMsg(){
        try {
            this.write("@@RELOAD_MODULES_END@@");
            String ok = this.read(); //this should be the ok message...
            
        } catch (IOException e) {
            PydevPlugin.log(IStatus.ERROR, "ERROR sending reload modules msg.", e);
        }
    }

    /**
     * @param str
     * @throws CoreException
     * @throws IOException
     */
    public List getGlobalCompletions(String str) throws CoreException {
        str = URLEncoder.encode(str);
        return this.getTheCompletions("@@GLOBALS:"+str+"\nEND@@");
    }

    /**
     * @param str
     * @throws CoreException
     * @throws IOException
     */
    public List getTokenCompletions(String token, String str) throws CoreException  {
        token = URLEncoder.encode(token);
        str = URLEncoder.encode(str);
        String s = "@@TOKEN_GLOBALS("+token+"):"+str+"\nEND@@";
        return this.getTheCompletions(s);
    }

    /**
     * @param token
     * @param docToParse
     * @return
     * @throws CoreException
     */
    public List getClassCompletions(String token, String str) throws CoreException {
        token = URLEncoder.encode(token);
        str = URLEncoder.encode(str);
        String s = "@@CLASS_GLOBALS("+token+"):"+str+"\nEND@@";
        return this.getTheCompletions(s);
    }

    /**
     * @param importsTipper
     * @return
     * @throws CoreException
     */
    public List getImportCompletions(String str) throws CoreException {
        str = URLEncoder.encode(str);
        return this.getTheCompletions("@@IMPORTS:"+str+"\nEND@@");
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
        } catch (IOException e) {
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
            String token       = URLDecoder.decode(tokenizer.nextToken());
            String description = URLDecoder.decode(tokenizer.nextToken());
            

            list.add(new String[]{token, description});
        }
        return list;
    }






}