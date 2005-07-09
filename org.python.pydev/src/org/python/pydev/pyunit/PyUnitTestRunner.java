/*
 * Created on Sept. 20, 2004
 *
 * @author Grig Gheorghiu
 */
package org.python.pydev.pyunit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.SocketUtil;
import org.python.pydev.utils.REF;

public class PyUnitTestRunner {

    public static final int BUFFER_SIZE = 1024 * 4;

    /**
     * Python server process.
     */
    public Process process;
    
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
    
    private BufferedReader reader;

    /**
     * Initialize given the file that points to the python server (execute it
     * with python).
     *  
     * @param f file pointing to the python server
     * 
     * @throws IOException
     * @throws CoreException
     */
    public PyUnitTestRunner() throws IOException, CoreException {
        //File testFile = new File(testModuleDir, testModuleName + ".py");
        //if(!testFile.exists()){
        //    throw new RuntimeException("Can't find test file");
        //}
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
     * 
     * @return the script to get the variables.
     * 
     * @throws CoreException
     */
    public static File getScriptWithinPySrc(String targetExec) throws CoreException {

        IPath relative = new Path("PySrc").addTrailingSeparator().append(
                targetExec);

        Bundle bundle = PydevPlugin.getDefault().getBundle();

        URL bundleURL = Platform.find(bundle, relative);
        URL fileURL;
        try {
            fileURL = Platform.asLocalURL(bundleURL);
            File f = new File(fileURL.getPath());

            return f;
        } catch (IOException e) {
            throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR,
                    "Can't find python script", null));
        }
    }

    /**
     * This method creates the python server process and starts the sockets, so that we
     * can talk with the server.
     * 
     * @throws IOException is some error happens creating the sockets - the process is terminated.
     */
    public void runTests(String testModuleDir, String testModuleName, IProject project) throws IOException{
        //int pWrite = SocketUtil.findUnusedLocalPort("127.0.0.1", 50000, 55000);
        int pRead = SocketUtil.findUnusedLocalPort("127.0.0.1", 60001, 65000);

        if(process != null)
            endIt();
		try {
			serverFile = getScriptWithinPySrc("SocketTestRunner.py");
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		String command = "python "+REF.getFileAbsolutePath(serverFile)+" "+pRead+" ";
        command += testModuleDir + " " + testModuleName;
        
        process = Runtime.getRuntime().exec(command);
        
        sleepALittle(1000);
        try {
	        serverSocket = new ServerSocket(pRead);         //read from this port
	        try {
	        	socketToRead = serverSocket.accept();
	        	try {
	        		readMessage();
	        	} finally {
	        		socketToRead.close();
	        	}
	        } finally {
	        	serverSocket.close();
	        }
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
    private void readMessage() throws IOException {
    	reader = new BufferedReader(
    			new InputStreamReader(socketToRead.getInputStream()));
        try {
        	String line = null;
        	while ((line = reader.readLine()) != null) {
        		//System.out.println(line);
        		parseMessage(line);
        	}
        } finally {
        	reader.close();
        }
    }

    private void parseMessage(String line) {
    	PydevPlugin plugin = PydevPlugin.getDefault();
    	if (line.startsWith("starting tests ")) {
    		int start = "starting tests ".length();
    		int count = Integer.parseInt(line.substring(start));
    		plugin.fireTestsStarted(count);
    	}
    	if (line.startsWith("ending tests")) {
    		plugin.fireTestsFinished();
    	}
    	if (line.startsWith("starting test ")) {
    		int start = "starting test ".length();
    		String method = line.substring(start, line.indexOf("("));
    		String klass = line.substring(line.indexOf("(") + 1, 
    				line.indexOf(")"));
    		plugin.fireTestStarted(klass, method);
    	}
    	if (line.startsWith("failing test ")) {
    		int start = "failing test ".length();
    		String method = line.substring(start, line.indexOf("("));
    		String klass = line.substring(line.indexOf("(") + 1, 
    				line.indexOf(")"));
    		StringWriter buffer = new StringWriter();
    		PrintWriter writer = new PrintWriter(buffer);
    		String frame = null;
    		try {
    			while ((frame = reader.readLine()) != null &&
				(!frame.equals("END TRACE")))
    				writer.println(frame);
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		String trace = buffer.getBuffer().toString();
    		plugin.fireTestFailed(klass, method, trace);
    	}
    }
    /**
     * @throws IOException
     */
    private void closeConn() throws IOException {
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
}
