package org.python.pydev.debug.unittest;
///*
// * @author Grig Gheorghiu
// */
//package org.python.pydev.debug.unittest;
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.net.SocketException;
//
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.core.runtime.SubProgressMonitor;
//import org.python.pydev.debug.core.PydevDebugPlugin;
//
//public class PyUnitTestRunner {
//
//	public static class ConsoleListener implements ITestRunListener {
//
//		public void testsStarted(int testCount, String testFile) {
//			System.out.println("STARTING TEST RUN: " + 
//					String.valueOf(testCount) + " tests");
//		}
//
//		public void testsFinished(String summary) {
//			System.out.println(summary);
//
//			//MessageDialog.openInformation(null, "Test Results", messageLong);
//		}
//
//		public void testStarted(String klass, String method) {
//		}
//
//		public void testFailed(String klass, String method, String failureType, String trace) {
//			System.out.print(klass + " " + method + "... ");
//			System.out.println(failureType);
//			System.out.println(trace);
//		}
//
//		public void testOK(String klass, String method) {
//			System.out.print(klass + " " + method + "... ");
//			System.out.println("OK");
//		}
//	}
//
//    public static final int BUFFER_SIZE = 1024 * 4;
//    /**
//     * Python server process.
//     */
//    private Process process;
//    
//    private SubProgressMonitor monitor;
//    private int portToReadFrom;
//    private String testFile;
//    private Socket socketToRead;
//    private ServerSocket serverSocket;
//    
//    private BufferedReader reader;
//
//    public PyUnitTestRunner(IProgressMonitor monitor, int port, String testFile) {
//    	this.monitor = (SubProgressMonitor) monitor;
//    	portToReadFrom = port;
//    	this.testFile = testFile;
//    }
//
//    public void readTestResults() throws IOException{
//		ITestRunListener listener = new ConsoleListener();
//		PydevDebugPlugin.getDefault().addTestListener(listener);
//
//        //sleepALittle(1000);
//        try {
//	        serverSocket = new ServerSocket(portToReadFrom);         //read from this portToRead
//	        try {
//	        	socketToRead = serverSocket.accept();
//	        	try {
//	        		readMessage();
//	        	} finally {
//	        		socketToRead.close();
//	        	}
//	        } finally {
//	        	serverSocket.close();
//	        }
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }  catch (IOException e) {
//            e.printStackTrace();
//            throw e;
//        } finally {
//    		PydevDebugPlugin.getDefault().removeTestListener(listener);
//        }
//    }
//
//    
//    /**
//     * @return s string with the contents read.
//     * @throws IOException
//     */
//    private void readMessage() throws IOException, SocketException {
//    	reader = new BufferedReader(
//    			new InputStreamReader(socketToRead.getInputStream()));
//        try {
//        	String line = null;
//        	while ((line = reader.readLine()) != null) {
//        		System.out.println(line);
//        		parseMessage(line);
//        	}
//		} catch (SocketException e) {
//			//e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		finally {
//        	reader.close();
//        }
//    }
//
//    private void parseMessage(String line) {
//    	int start;
//    	PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
//    	if (line.startsWith("starting tests ")) {
//    		start = "starting tests ".length();
//    		int count = Integer.parseInt(line.substring(start));
//    		monitor.subTask("Starting test run");
//    		plugin.fireTestsStarted(count, testFile);
//    	}
//    	if (line.startsWith("ending tests ")) {
//    		start = "ending tests ".length();
//    		String timeTaken = line.substring(start, line.indexOf(";"));
//    		String result = line.substring(line.indexOf(";") + 1, 
//    				line.length());
//    		String summary = timeTaken +'\n'+ result;
//    		monitor.subTask("Ending test run");
//    		plugin.fireTestsFinished(summary);
//    	}
//    	if (line.startsWith("starting test ")) {
//    		start = "starting test ".length();
//    		String method = line.substring(start, line.indexOf("("));
//    		method = method.trim();
//    		String klass = line.substring(line.indexOf("(") + 1, 
//    				line.indexOf(")"));
//    		klass = klass.trim();
//    		monitor.subTask(line);
//    		plugin.fireTestStarted(klass, method);
//    	}
//    	if (line.startsWith("test OK ")) {
//    		start = "test OK ".length();
//    		String method = line.substring(start, line.indexOf("("));
//    		String klass = line.substring(line.indexOf("(") + 1, 
//    				line.indexOf(")"));
//    		monitor.subTask(line);
//    		plugin.fireTestOK(klass, method);
//    	}
//    	if (line.startsWith("failing test ")) {
//    		start = "failing test ".length();
//    		String method = line.substring(start, line.indexOf("("));
//    		String klass = line.substring(line.indexOf("(") + 1, 
//    				line.indexOf(")"));
//    		StringWriter buffer = new StringWriter();
//    		PrintWriter writer = new PrintWriter(buffer);
//    		String frame = null;
//    		String failureType = "";
//    		try {
//    			while ((frame = reader.readLine()) != null &&
//				(!frame.equals("END TRACE"))) {
//    				//writer.println(frame);
//    				
//    				if (frame.startsWith("TYPE:")) {
//    					int cnt = "TYPE:".length();
//    					failureType = frame.substring(cnt, frame.indexOf("."));
//    				}
//    				else {
//    					writer.println(frame);
//    				}
//    			}
//    				
//    		} catch (IOException e) {
//    			e.printStackTrace();
//    		}
//    		String trace = buffer.getBuffer().toString();
//    		monitor.subTask(line);
//    		plugin.fireTestFailed(klass, method, failureType, trace);
//    	}
//    }
//    /**
//     * @throws IOException
//     */
//    private void closeConn() throws IOException {
//        if(socketToRead != null){
//            socketToRead.close();
//        }
//        socketToRead = null;
//        
//        if(serverSocket != null){
//            serverSocket.close();
//        }
//        serverSocket = null;
//    }
//
//
//    /**
//     * Kill our sub-process.
//     * @throws IOException
//     */
//    void endIt() {
//        
//        try {
//            closeConn();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if (process!= null){
//            try {
//                int i = process.getErrorStream().available();
//                byte b[] = new byte[i];
//                process.getErrorStream().read(b);
//                System.out.println(new String(b));
//                
//                i = process.getInputStream().available();
//                b = new byte[i];
//                process.getErrorStream().read(b);
//                System.out.println(new String(b));
//            } catch (Exception e1) {
//                e1.printStackTrace();
//            }
//                
//            try {
//                process.destroy();
//            } catch (Exception e2) {
//                e2.printStackTrace();
//            }
//
//            try {
//                process.waitFor();
//            } catch (Exception e1) {
//                e1.printStackTrace();
//            }
//            process = null;
//        }
//        
//    }
//}
