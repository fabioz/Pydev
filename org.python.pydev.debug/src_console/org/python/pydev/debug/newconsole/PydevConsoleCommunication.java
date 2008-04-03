package org.python.pydev.debug.newconsole;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcNoSuchHandlerException;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.webserver.WebServer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.dltk.console.IScriptConsoleCommunication;
import org.python.pydev.dltk.console.InterpreterResponse;
import org.python.pydev.editor.codecompletion.AbstractPyCodeCompletion;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
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
    private ThreadStreamReader stdOutReader;
    
    /**
     * Responsible for getting the stderr of the process.
     */
    private ThreadStreamReader stdErrReader;

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

        IPydevXmlRpcClient client = new PydevXmlRpcClient(process, stdErrReader);
        client.setPort(port);
        
        this.client = client;
    }

    /**
     * Stops the communication with the client (passes message for it to quit).
     */
    public void close() throws Exception {
        if(this.client != null){
            this.client.execute("close", new Object[0]);
            this.client = null;
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
    public InterpreterResponse execInterpreter(final String command) throws Exception {
        nextResponse = null;
        if(waitingForInput){
            inputReceived = command;
            waitingForInput = false;
            //the thread that we started in the last exec is still alive if we were waiting for an input.
        }else{
            //create a thread that'll keep locked until an answer is received from the server.
            Thread thread = new Thread(){
                @Override
                public void run() {
                    boolean needInput = false;
                    try{
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
                        
                        if(errorContents == null){
                            errorContents = stdErrReader.getAndClearContents();
                        }
                        nextResponse = new InterpreterResponse(stdOutReader.getAndClearContents(), 
                                errorContents, more, needInput);
                        
                    }catch(Exception e){
                        nextResponse = new InterpreterResponse("", "Exception while pushing line to console:"+e.getMessage(), 
                                false, needInput);
                    }
                }
            };
            thread.start();
        }
        
        //busy loop until we have a response
        while(nextResponse == null){
            synchronized(lock2){
                try {
                    lock2.wait(10);
                } catch (InterruptedException e) {
                    PydevPlugin.log(e);
                }
            }
        }
        return nextResponse;
    }

    /**
     * @return completions from the client
     */
    @SuppressWarnings("unchecked")
    public ICompletionProposal[] getCompletions(String text, int offset) throws Exception {
        if(waitingForInput){
            return new ICompletionProposal[0];
        }
        Object fromServer = client.execute("getCompletions", new Object[]{text});
        List<ICompletionProposal> ret = new ArrayList<ICompletionProposal>();
        
        
        int length = text.lastIndexOf('.');
        if(length == -1){
            length = text.length();
        }else{
            length = text.length()-length-1;
        }
        
        if(fromServer instanceof Object[]){
            Object[] comps = (Object[]) fromServer;
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
//                    		offset-length, length, name.length(), 
//                    		PyCodeCompletionImages.getImageForType(type), name, null, docStr, priority));
                    
                    int cursorPos = name.length();
                    if(args.length() > 1){
                        cursorPos += 1;
                    }
                    ret.add(new PyLinkedModeCompletionProposal(nameAndArgs,
                            offset-length, length, cursorPos, 
                            PyCodeCompletionImages.getImageForType(type), nameAndArgs, null, docStr, priority, 
                            PyCompletionProposal.ON_APPLY_DEFAULT, args, false));
                    
                }
            }
        }
        ICompletionProposal[] proposals = ret.toArray(new ICompletionProposal[ret.size()]);
        return proposals;
    }

    /**
     * Extracts an int from an object
     * 
     * @param objToGetInt the object that should be gotten as an int
     * @return int with the int the object represents
     */
    private int extractInt(Object objToGetInt) {
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
