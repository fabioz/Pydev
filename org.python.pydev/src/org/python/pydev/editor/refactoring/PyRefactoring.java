/*
 * Created on Sep 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.refactoring;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IPropertyListener;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation;
import org.python.pydev.editor.codecompletion.PythonShell;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;

/**
 * This class is used to make the refactorings.
 * 
 * The design is basically: handle the actions and pass them to the 
 * python server (that should be using bicycle repair man).
 * 
 * Later, this might be changed as long as the interface provided is
 * the same.
 * 
 * @author Fabio Zadrozny
 */
public class PyRefactoring {
    

    /**
     * Instead of making all static, let's use a singleton... it may be useful...
     */
    private static PyRefactoring pyRefactoring;

    private List propChangeListeners = new ArrayList();

    public static final int REFACTOR_RESULT = 1;

    private Object lastRefactorResults;

    
    public synchronized static PyRefactoring getPyRefactoring(){
        if (pyRefactoring == null){
            pyRefactoring = new PyRefactoring();
        }
        return pyRefactoring;
    }

    private PyRefactoring(){
        try {
            PythonShell.getServerShell(PythonShell.OTHERS_SHELL); //when we initialize, initialize the server.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    public void addPropertyListener(IPropertyListener l) {
    	propChangeListeners.add(l);
    }

    /**
     * This method can be used to write something to the server and get its answer.
     * 
     * @param str
     * @param operation
     * @return
     */
    private String makeAction(String str, Operation operation){
        PythonShell pytonShell;
        try {
            pytonShell = PythonShell.getServerShell(PythonShell.OTHERS_SHELL);
	        try {
	            pytonShell.write(str);
	 
	            return URLDecoder.decode(pytonShell.read(operation));
	        } catch (Exception e) {
	            e.printStackTrace();
	            
	            pytonShell.restartShell();
	        }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }


    /**
     * @param editorFile
     * @param beginLine
     * @param beginCol
     * @param endLine
     * @param endCol
     * @param name
     * @param operation
     */
    public String extract(File editorFile, int beginLine, int beginCol, int endLine, int endCol, String name, Operation operation) {
        String s = "@@BIKE";
        s+=        "extractMethod";
        s+=        "|"+editorFile.getAbsolutePath();
        s+=        "|"+beginLine;
        s+=        "|"+beginCol;
        s+=        "|"+endLine;
        s+=        "|"+endCol;
        s+=        "|"+name;
        s+=        "END@@";
//        System.out.println("Extract: "+s);
        String string = makeAction(s, operation);
//        System.out.println("REFACTOR RESULT:"+string);
        
        communicateRefactorResult(string);
        return string;
    }

    /**
     * @param editorFile
     * @param beginLine
     * @param beginCol
     * @param name
     * @param operation
     */
    public String rename(File editorFile, int beginLine, int beginCol, String name, Operation operation) {
        String s = "@@BIKE";
        s+=        "renameByCoordinates";
        s+=        "|"+editorFile.getAbsolutePath();
        s+=        "|"+beginLine;
        s+=        "|"+beginCol;
        s+=        "|"+name;
        s+=        "END@@";
//        System.out.println("Extract: "+s);
        String string = makeAction(s, operation);
        
//        System.out.println("REFACTOR RESULT:"+string);
        communicateRefactorResult(string);
        return string;
        
    }

    public List findDefinition(File editorFile, int beginLine, int beginCol, Operation operation) {
        String s = "@@BIKE";
        s+=        "findDefinition";
        s+=        "|"+editorFile.getAbsolutePath();
        s+=        "|"+beginLine;
        s+=        "|"+beginCol;
        s+=        "END@@";

        System.out.println("Find: "+s);
        String string = makeAction(s, operation);
        
        System.out.println("REFACTOR RESULT:"+string);
        List l = new ArrayList();

        
        if (string.startsWith("BIKE_OK:")){
            string = string.replaceFirst("BIKE_OK:", "").replaceAll("\\[","").replaceAll("'","");
	        string = string.substring(0, string.lastIndexOf(']'));    
	        
	        //now we should have something like:
	        //(file,line,col,confidence)(file,line,col,confidence)...
	        
	        string = string.replaceAll("\\(","");
	        StringTokenizer tokenizer = new StringTokenizer(string, ")");
	        while(tokenizer.hasMoreTokens()){
	            String tok = tokenizer.nextToken();
	            
	            String[] toks = tok.split(",");
	            if(toks.length == 4){ //4th position is the confidence
	                Location location = new Location(Integer.parseInt(toks[1])-1, Integer.parseInt(toks[2]));
	                l.add(new ItemPointer(new File(toks[0]), location, location));
	            }
	        }
        }

        
        return l;
        
    }
    
    /**
     * @param editorFile
     * @param beginLine
     * @param beginCol
     * @param operation
     * @return
     */
    public String inlineLocalVariable(File editorFile, int beginLine, int beginCol, Operation operation) {
        String s = "@@BIKE";
        s+=        "inlineLocalVariable";
        s+=        "|"+editorFile.getAbsolutePath();
        s+=        "|"+beginLine;
        s+=        "|"+beginCol;
        s+=        "END@@";
//        System.out.println("Inline: "+s);
        String string = makeAction(s, operation);
        
//        System.out.println("REFACTOR RESULT:"+string);
        communicateRefactorResult(string);
        return string;
    }
    
    /**
     * @param editorFile
     * @param beginLine
     * @param beginCol
     * @param endLine
     * @param endCol
     * @param name
     * @param operation
     * @return
     */
    public String extractLocalVariable(File editorFile, int beginLine, int beginCol, int endLine, int endCol, String name, Operation operation) {
        String s = "@@BIKE";
        s+=        "extractLocalVariable";
        s+=        "|"+editorFile.getAbsolutePath();
        s+=        "|"+beginLine;
        s+=        "|"+beginCol;
        s+=        "|"+endLine;
        s+=        "|"+endCol;
        s+=        "|"+name;
        s+=        "END@@";
//        System.out.println("Extract: "+s);
        String string = makeAction(s, operation);
//        System.out.println("REFACTOR RESULT:"+string);
        
        communicateRefactorResult(string);
        return string;
    }
    
    /**
     * @param string
     */
    private void communicateRefactorResult(String string) {
   
        List l = refactorResultAsList(string);
        
        Object o=null;
        
        for (Iterator iter = this.propChangeListeners.iterator(); iter.hasNext();) {
            IPropertyListener element = (IPropertyListener) iter.next();
            o = new Object[]{this, l};
            element.propertyChanged(o, REFACTOR_RESULT);
        }
        
        this.lastRefactorResults = o;
    }
    
    
    



    /**
     * @param string
     * @return
     */
    public List refactorResultAsList(String string) {
        List l = new ArrayList();
        
        if (string.startsWith("BIKE_OK:")){
	        string = string.replaceFirst("BIKE_OK:", "").replaceAll("\\[","").replaceAll("'","");
	        string = string.substring(0, string.lastIndexOf(']'));
	        StringTokenizer tokenizer = new StringTokenizer(string, ", ");
	        
	        while(tokenizer.hasMoreTokens()){
	            l.add(tokenizer.nextToken());
	        }
        }
        return l;
    }

    /**
     * 
     */
    public void restartShell() {
        try {
            PythonShell.getServerShell(PythonShell.OTHERS_SHELL).restartShell();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param lastRefactorResults The lastRefactorResults to set.
     */
    public void setLastRefactorResults(Object lastRefactorResults) {
        this.lastRefactorResults = lastRefactorResults;
    }

    /**
     * @return Returns the lastRefactorResults.
     */
    public Object getLastRefactorResults() {
        return lastRefactorResults;
    }



}
