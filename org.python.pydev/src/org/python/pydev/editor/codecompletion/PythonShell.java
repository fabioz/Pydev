/*
 * Created on Aug 16, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import org.eclipse.core.runtime.CoreException;

/**
 * 
 * TODO: THIS IS STILL ONLY A TEST!!
 * 
 * @author Fabio Zadrozny
 */
public class PythonShell {

    public Process p;
    private Socket socket;
    public static final String END_MSG = "@END@";


    public PythonShell() throws IOException, CoreException {
        startIt();
    }

    /**
     * @throws CoreException
     */
    public void startIt() throws IOException, CoreException {

        File serverFile = new File("D:\\dev_programs\\eclipse_3\\eclipse\\workspace\\org.python.pydev\\PySrc\\pycompletionserver.py");
        //PyCodeCompletion.getScriptWithinPySrc("pycompletionserver.py");

        p = Runtime.getRuntime().exec("python "+serverFile.getAbsolutePath());
        
        sleepALittle();
        
        socket = new Socket("127.0.0.1",50007);
        
        write("TESTE"+END_MSG);
        String b = read();
        System.out.println(b);
    }

    
    /**
     * 
     */
    private void sleepALittle() {
        try {
            synchronized(this){
                wait(100);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @throws IOException
     */
    private void closeConn() throws IOException {
        if(socket != null){
	        socket.getOutputStream().write("".getBytes());
	        socket.close();
        }
        socket = null;
    }

    /**
     * @return
     * @throws IOException
     */
    public String read() throws IOException {
        String str = "";
        while(str.endsWith(END_MSG)){
	        BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream(), 3);
	        int size = inputStream.available();
	        byte b[] = new byte[size];
	        inputStream.read(b);
	        str += new String(b);
        }
        return str;
    }

    /**
     * @param str
     * @throws IOException
     */
    public void write(String str) throws IOException {
        socket.getOutputStream().write(str.getBytes());
    }

    /**
     * Kill our sub-process.
     */
    private void endIt() {
        try {
            closeConn();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (p!= null){
            p.destroy();
            p = null;
        }
        
    }
    
    public static void main(String[] args) throws IOException, CoreException{
        PythonShell shell = new PythonShell();
        shell.endIt();
    }

}