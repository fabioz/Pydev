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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
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
    public static PythonShell getServerShell() throws IOException, CoreException {
        if(pytonShell == null){
            pytonShell = new PythonShell();
            pytonShell.startIt();
        }
        return pytonShell;
        
    }

    
    public static final int BUFFER_SIZE = 1024 * 4;
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
        this(PyCodeCompletion.getScriptWithinPySrc("pycompletionserver.py"));
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
     * @throws IOException is some error happens creating the sockets - the process is terminated.
     */
    public void startIt() throws IOException{
        try {

            int pWrite = SocketUtil.findUnusedLocalPort("127.0.0.1", 50000, 55000);
            int pRead = SocketUtil.findUnusedLocalPort("127.0.0.1", 55001, 60000);

            if(process != null)
                endIt();
            process = Runtime.getRuntime().exec("python "+serverFile.getAbsolutePath()+" "+pWrite+" "+pRead);
            
            sleepALittle();
            
            socketToWrite = new Socket("127.0.0.1",pWrite); //we should write in this port  
            serverSocket = new ServerSocket(pRead);         //and read in this port 
            socketToRead = serverSocket.accept();
        } catch (IOException e) {
            
            if(process!=null){
                process.destroy();
            }
            e.printStackTrace();
            throw e;
        }
    }

    
    /**
     * @return s string with the contents read.
     * @throws IOException
     */
    public String read() throws IOException {
        String str = "";

        int j = 0;
        while(str.indexOf("END@@") == -1 && j != 100){
	        byte[] b = new byte[PythonShell.BUFFER_SIZE];

            this.socketToRead.getInputStream().read(b);
//            System.out.println("READ:"+new String(b));

            String s = new String(b);
            
            str += s;
            j++;
        }
        
        //remove @@COMPLETIONS
        str = str.replaceFirst("@@COMPLETIONS","");
        //remove END@@
        return str.substring(0, str.lastIndexOf("END@@"));
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
    void endIt() {
        
        try {
            closeConn();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (process!= null){
            try {
                int i = process.getErrorStream().available();
                byte b[] = new byte[i];
                process.getErrorStream().read(b);
                System.out.println(new String(b));
                
                i = process.getInputStream().available();
                b = new byte[i];
                process.getErrorStream().read(b);
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
            System.out.println("changing dir:"+file.getAbsolutePath());
            this.write("@@CHANGE_DIR:"+file.getAbsolutePath()+"END@@");
            String ok = this.read(); //this should be the ok message...
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void sendReloadModulesMsg(){
        try {
            this.write("@@RELOAD_MODULES_END@@");
            String ok = this.read(); //this should be the ok message...
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param str
     * @throws IOException
     */
    public List getGlobalCompletions(String str) {
        return this.getTheCompletions("@@GLOBALS:"+str+"\nEND@@");
    }

    /**
     * @param str
     * @throws IOException
     */
    public List getTokenCompletions(String token, String str)  {
        String s = "@@TOKEN_GLOBALS("+token+"):"+str+"\nEND@@";
        return this.getTheCompletions(s);
    }

    /**
     * @param token
     * @param docToParse
     * @return
     */
    public List getClassCompletions(String token, String str) {
        String s = "@@CLASS_GLOBALS("+token+"):"+str+"\nEND@@";
        return this.getTheCompletions(s);
    }

    private List getTheCompletions(String str){
        try {
            this.write(str);
 
            return getCompletions();
        } catch (Exception e) {
            e.printStackTrace();

            this.endIt();
            try {
                this.startIt();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            return getInvalidCompletion();
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
            String token = tokenizer.nextToken();
            String description = tokenizer.nextToken();
            list.add(new String[]{token, description});
        }
        return list;
    }



}