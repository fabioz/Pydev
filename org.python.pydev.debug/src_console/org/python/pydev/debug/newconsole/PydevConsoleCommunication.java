package org.python.pydev.debug.newconsole;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.dltk.console.IScriptConsoleCommunication;
import org.python.pydev.dltk.console.InterpreterResponse;
import org.python.pydev.editor.codecompletion.AbstractPyCodeCompletion;
import org.python.pydev.editor.codecompletion.IPyCodeCompletion;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.PyCodeCompletionImages;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;

/**
 * Communication with Xml-rpc with the client.
 *
 * @author Fabio
 */
public class PydevConsoleCommunication implements IScriptConsoleCommunication{

    private XmlRpcClient client;

    /**
     * Initializes the xml-rpc communication.
     * 
     * @param port the port where the communication should happen.
     * 
     * @throws MalformedURLException
     */
    public PydevConsoleCommunication(int port) throws MalformedURLException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL("http://127.0.0.1:"+port));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        this.client = client;
    }
    
    /**
     * Stops the communication with the client (passes message for it to quit).
     */
    public void close() throws Exception {
        this.client.execute("close", new Object[0]);
    }

    /**
     * Executes a given line in the interpreter.
     * 
     * @param command the command to be executed in the client
     */
    public InterpreterResponse execInterpreter(String command) throws Exception {
        Object[] execute = (Object[]) this.client.execute("addExec", new Object[]{command});
        
        boolean more = extractBool(execute[2]);
        boolean needInput = extractBool(execute[3]);
        
        return new InterpreterResponse((String)execute[0], (String)execute[1], 
                more, 
                needInput);
    }

    /**
     * Gets an object as a boolean. 
     * 
     * @param objToGetAsBool
     * @return a boolean with the object
     */
    private boolean extractBool(Object objToGetAsBool) {
        if(objToGetAsBool instanceof Boolean){
            return (Boolean)objToGetAsBool;
        }
        return Boolean.parseBoolean(objToGetAsBool.toString());
    }

    /**
     * @return completions from the client
     */
    @SuppressWarnings("unchecked")
    public ICompletionProposal[] getCompletions(String text, int offset) throws Exception {
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
                    name += args;

                    int priority = IPyCompletionProposal.PRIORITY_DEFAULT;
                    if(type == IToken.TYPE_PARAM){
                        priority = IPyCompletionProposal.PRIORITY_LOCALS;
                    }
                    
                    ret.add(new PyCompletionProposal(name,
                    		offset-length, length, 0, 
                    		PyCodeCompletionImages.getImageForType(type), name, null, docStr, priority));
                    
                }
            }
        }
        ICompletionProposal[] proposals = ret.toArray(new ICompletionProposal[ret.size()]);
        
        Arrays.sort(proposals, IPyCodeCompletion.PROPOSAL_COMPARATOR);
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
        return client.execute("getDescription", new Object[]{text}).toString();
    }

}
