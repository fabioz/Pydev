/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcNoSuchHandlerException;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.webserver.WebServer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.debug.newconsole.prefs.InteractiveConsolePrefs;
import org.python.pydev.dltk.console.IScriptConsoleCommunication;
import org.python.pydev.dltk.console.InterpreterResponse;
import org.python.pydev.editor.codecompletion.AbstractPyCodeCompletion;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.PyCalltipsContextInformation;
import org.python.pydev.editor.codecompletion.PyCodeCompletionImages;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.codecompletion.PyLinkedModeCompletionProposal;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.ThreadStreamReader;

/**
 * Communication with Xml-rpc with the client.
 *
 * @author Fabio
 */
public class PydevConsoleCommunication implements IScriptConsoleCommunication, XmlRpcHandler{

    /**
     * XML-RPC client for sending messages to the server.
     */
    private IPydevXmlRpcClient client;
    
    /**
     * Responsible for getting the stdout of the process.
     */
    private final ThreadStreamReader stdOutReader;
    
    /**
     * Responsible for getting the stderr of the process.
     */
    private final ThreadStreamReader stdErrReader;

    /**
     * This is the server responsible for giving input to a raw_input() requested.
     */
    private WebServer webServer;
    
    /**
     * Initializes the xml-rpc communication.
     * 
     * @param port the port where the communication should happen.
     * @param process this is the process that was spawned (server for the XML-RPC)
     * 
     * @throws MalformedURLException
     */
    public PydevConsoleCommunication(int port, Process process, int clientPort) throws Exception {
        stdOutReader = new ThreadStreamReader(process.getInputStream());
        stdErrReader = new ThreadStreamReader(process.getErrorStream());
        stdOutReader.start();
        stdErrReader.start();
        
        //start the server that'll handle input requests
        this.webServer = new WebServer(clientPort);
        XmlRpcServer serverToHandleRawInput = this.webServer.getXmlRpcServer();
        serverToHandleRawInput.setHandlerMapping(new XmlRpcHandlerMapping(){

            public XmlRpcHandler getHandler(String handlerName) throws XmlRpcNoSuchHandlerException, XmlRpcException {
                return PydevConsoleCommunication.this;
            }
        });
        
        this.webServer.start();

        IPydevXmlRpcClient client = new PydevXmlRpcClient(process, stdErrReader, stdOutReader);
        client.setPort(port);
        
        this.client = client;
    }

    /**
     * Stops the communication with the client (passes message for it to quit).
     */
    public void close() throws Exception {
        if(this.client != null){
            Job job = new Job("Close console communication"){

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        PydevConsoleCommunication.this.client.execute("close", new Object[0]);
                    } catch (Exception e) {
                        //Ok, we can ignore this one on close.
                    }
                    PydevConsoleCommunication.this.client = null;
                    return Status.OK_STATUS;
                }
            };
            job.schedule(); //finish it
        }
        
        if(this.webServer != null){
            this.webServer.shutdown();
            this.webServer = null;
        }
    }
    
    /**
     * Variables that control when we're expecting to give some input to the server or when we're
     * adding some line to be executed 
     */
    
    /**
     * Signals that the next command added should be sent as an input to the server.
     */
    private volatile boolean waitingForInput;
    
    /**
     * Input that should be sent to the server (waiting for raw_input)
     */
    private volatile String inputReceived;
    
    /**
     * Response that should be sent back to the shell.
     */
    private volatile InterpreterResponse nextResponse;
    
    /**
     * Helper to keep on busy loop.
     */
    private volatile Object lock = new Object();
    
    /**
     * Helper to keep on busy loop.
     */
    private volatile Object lock2 = new Object();
    
    /**
     * Keeps a flag indicating that we were able to communicate successfully with the shell at least once
     * (if we haven't we may retry more than once the first time, as jython can take a while to initialize
     * the communication)
     */
    private volatile boolean firstCommWorked = false;

    
    /**
     * Called when the server is requesting some input from this class.
     */
    public Object execute(XmlRpcRequest request) throws XmlRpcException {
        waitingForInput = true;
        inputReceived = null;
        boolean needInput = true;
        
        //let the busy loop from execInterpreter free and enter a busy loop
        //in this function until execInterpreter gives us an input
        nextResponse = new InterpreterResponse(stdOutReader.getAndClearContents(), 
                stdErrReader.getAndClearContents(), false, needInput);
        
        //busy loop until we have an input
        while(inputReceived == null){
            synchronized(lock){
                try {
                    lock.wait(10);
                } catch (InterruptedException e) {
                    PydevPlugin.log(e);
                }
            }
        }
        return inputReceived;
    }

    /**
     * Executes a given line in the interpreter.
     * 
     * @param command the command to be executed in the client
     */
    public void execInterpreter(final String command, final ICallback<Object, InterpreterResponse> onResponseReceived){
        nextResponse = null;
        if(waitingForInput){
            inputReceived = command;
            waitingForInput = false;
            //the thread that we started in the last exec is still alive if we were waiting for an input.
        }else{
            //create a thread that'll keep locked until an answer is received from the server.
            Job job = new Job("PyDev Console Communication"){

                /**
                 * Executes the needed command 
                 * 
                 * @return a tuple with (null, more) or (error, false)
                 * 
                 * @throws XmlRpcException
                 */
                private Tuple<String, Boolean> exec() throws XmlRpcException{
                
                    Object[] execute = (Object[]) client.execute("addExec", new Object[]{command});
                    
                    Object object = execute[0];
                    boolean more;
                    
                    String errorContents = null;
                    if(object instanceof Boolean){
                        more = (Boolean)object;
                        
                    }else{
                        String str = object.toString();
                        
                        String lower = str.toLowerCase();
                        if(lower.equals("true") || lower.equals("1")){
                            more = true;
                        }else if(lower.equals("false")|| lower.equals("0")){
                            more = false;
                        }else{
                            more = false;
                            errorContents = str;
                        }
                    }
                    return new Tuple<String, Boolean>(errorContents, more);
                }
                
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    boolean needInput = false;
                    try{
                        
                        Tuple<String, Boolean> executed = null;
                        
                        
                        //the 1st time we'll do a connection attempt, we can try to connect n times (until the 1st time the connection
                        //is accepted) -- that's mostly because the server may take a while to get started.
                        int commAttempts = 0;
                        int maximumAttempts = InteractiveConsolePrefs.getMaximumAttempts();
                        //System.out.println(maximumAttempts);
                        
                        while(true){
                            if(monitor.isCanceled()){
                                return Status.CANCEL_STATUS;
                            }
                            executed = exec();
                            
                            //executed.o1 is not null only if we had an error
                            
                            String refusedConnPattern = "Failed to read servers response"; // Was "refused", but it didn't 
                                                                                           // work on non English system 
                                                                                           // (in Spanish localized systems
                                                                                           // it is "rechazada") 
                                                                                           // This string always works, 
                                                                                           // because it is hard-coded in
                                                                                           // the XML-RPC library)
                            if(executed.o1 != null && executed.o1.indexOf(refusedConnPattern) != -1){
                                if(firstCommWorked){
                                    break;
                                }else{
                                    if(commAttempts < maximumAttempts){
                                        commAttempts += 1;
                                        Thread.sleep(250);
                                        executed.o1 = stdErrReader.getAndClearContents();
                                        continue;
                                    }else{
                                        break;
                                    }
                                }
                                
                            }else{
                                break;
                            }
                            
                            //unreachable code!! -- commented because eclipse will complain about it
                            //throw new RuntimeException("Can never get here!");
                        }
                        
                        firstCommWorked = true;
                        
                        String errorContents = executed.o1;
                        boolean more = executed.o2;
                        
                        if(errorContents == null){
                            errorContents = stdErrReader.getAndClearContents();
                        }else{
                            errorContents += "\n"+stdErrReader.getAndClearContents();
                        }
                        nextResponse = new InterpreterResponse(stdOutReader.getAndClearContents(), 
                                errorContents, more, needInput);
                        
                    }catch(Exception e){
                        nextResponse = new InterpreterResponse("", "Exception while pushing line to console:"+e.getMessage(), 
                                false, needInput);
                    }
                    return Status.OK_STATUS;
                }
            };
            
            job.schedule();
            
        }
        
        //busy loop until we have a response
        while(nextResponse == null){
            synchronized(lock2){
                try {
                    lock2.wait(20);
                } catch (InterruptedException e) {
                    PydevPlugin.log(e);
                }
            }
        }
        onResponseReceived.call(nextResponse);
    }

    /**
     * @return completions from the client
     */
    public ICompletionProposal[] getCompletions(String text, int offset) throws Exception {
        if(waitingForInput){
            return new ICompletionProposal[0];
        }
        Object fromServer = client.execute("getCompletions", new Object[]{text});
        List<ICompletionProposal> ret = new ArrayList<ICompletionProposal>();
        
        
        convertToICompletions(text, offset, fromServer, ret);
        ICompletionProposal[] proposals = ret.toArray(new ICompletionProposal[ret.size()]);
        return proposals;
    }

    public static void convertToICompletions(String text, int offset, Object fromServer, List<ICompletionProposal> ret) {
        if(fromServer instanceof Object[]){
            Object[] objects = (Object[]) fromServer;
            fromServer = Arrays.asList(objects);
        }
        if(fromServer instanceof List){
            int length = text.lastIndexOf('.');
            if(length == -1){
                length = text.length();
            }else{
                length = text.length()-length-1;
            }
            
            List comps = (List) fromServer;
            for(Object o:comps){
                if(o instanceof Object[]){
                    //name, doc, args, type
                    Object[] comp = (Object[]) o;
                    
                    String name = (String) comp[0];
                    String docStr = (String) comp[1];
                    int type = extractInt(comp[3]);
                    String args = AbstractPyCodeCompletion.getArgs((String) comp[2], type,
                            ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE) ;
                    String nameAndArgs = name+args;

                    int priority = IPyCompletionProposal.PRIORITY_DEFAULT;
                    if(type == IToken.TYPE_PARAM){
                        priority = IPyCompletionProposal.PRIORITY_LOCALS;
                    }
                    
//                    ret.add(new PyCompletionProposal(name,
//                            offset-length, length, name.length(), 
//                            PyCodeCompletionImages.getImageForType(type), name, null, docStr, priority));
                    
                    int cursorPos = name.length();
                    if(args.length() > 1){
                        cursorPos += 1;
                    }
                    
                    int replacementOffset = offset-length;
                    PyCalltipsContextInformation pyContextInformation = null;
                    if(args.length() > 2){
                        pyContextInformation = new PyCalltipsContextInformation(args, replacementOffset+name.length()+1); //just after the parenthesis
                    }
                    
                    ret.add(new PyLinkedModeCompletionProposal(nameAndArgs,
                            replacementOffset, length, cursorPos, 
                            PyCodeCompletionImages.getImageForType(type), nameAndArgs, pyContextInformation, docStr, priority, 
                            PyCompletionProposal.ON_APPLY_DEFAULT, args, false));
                    
                }
            }
        }
    }

    /**
     * Extracts an int from an object
     * 
     * @param objToGetInt the object that should be gotten as an int
     * @return int with the int the object represents
     */
    private static int extractInt(Object objToGetInt) {
        if(objToGetInt instanceof Integer){
            return (Integer)objToGetInt;
        }
        return Integer.parseInt(objToGetInt.toString());
    }
    
    
    /**
     * @return the description of the given attribute in the shell
     */
    public String getDescription(String text) throws Exception {
        if(waitingForInput){
            return "Unable to get description: waiting for input.";
        }
        return client.execute("getDescription", new Object[]{text}).toString();
    }


}
