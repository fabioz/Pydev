/*
 * Created on Sep 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.refactoring;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
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
public class PyRefactoring extends AbstractPyRefactoring {
    
    /**
     * Default constructor. Initializes the refactoring shell.
     */
    public PyRefactoring(){
        try {
            AbstractShell.getServerShell(IPythonNature.PYTHON_RELATED, AbstractShell.OTHERS_SHELL); //when we initialize, initialize the server.
        } catch (Exception e) {
            //for the refactoring, we just let it pass...
        }
    }

    /**
     * Restarts the shell if some error happened.
     */
    public void restartShell() {
        try {
            AbstractShell.getServerShell(IPythonNature.PYTHON_RELATED, AbstractShell.OTHERS_SHELL).restartShell();
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }
    
    /**
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#killShell()
     */
    public void killShell() {
        try {
            AbstractShell.getServerShell(IPythonNature.PYTHON_RELATED, AbstractShell.OTHERS_SHELL).endIt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * This method can be used to write something to the server and get its answer.
     * 
     * @param str
     * @param operation
     * @param editor
     * @return
     */
    private String makeAction(String str, RefactoringRequest request){
        PyRefactorAction.checkAvailableForRefactoring(request, this);

        AbstractShell pytonShell;
        try {
            pytonShell = AbstractShell.getServerShell(request.nature, AbstractShell.OTHERS_SHELL);
	        try {
		        pytonShell.changePythonPath(request.nature.getPythonPathNature().getCompleteProjectPythonPath(null)); //default
	            pytonShell.write(str);
	 
	            return URLDecoder.decode(pytonShell.read(request.monitor), "UTF-8");
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
     * Requests an extract in the shell
     */
    public String extract(RefactoringRequest request) {
        File editorFile = request.file;
        String s = "@@BIKE";
        s+=        "extractMethod";
        s+=        "|"+REF.getFileAbsolutePath(editorFile);
        s+=        "|"+request.getBeginLine();
        s+=        "|"+request.getBeginCol();
        s+=        "|"+request.getEndLine();
        s+=        "|"+request.getEndCol();
        s+=        "|"+request.inputName;
        s+=        "END@@";
//        System.out.println("Extract: "+s);
        String string = makeAction(s, request);
//        System.out.println("REFACTOR RESULT:"+string);
        
        communicateRefactorResult(string);
        return string;
    }

    /** Requests a rename operation in the shell
     */
    public String rename(RefactoringRequest request) {
        if(request.inputName == null || request.inputName.equals("")){
            return "";
        }
        File editorFile = request.file;
        String s = "@@BIKE";
        s+=        "renameByCoordinates";
        s+=        "|"+REF.getFileAbsolutePath(editorFile);
        s+=        "|"+request.getBeginLine();
        s+=        "|"+request.getBeginCol();
        s+=        "|"+request.inputName;
        s+=        "END@@";
//        System.out.println("Extract: "+s);
        String string = makeAction(s, request);
        
//        System.out.println("REFACTOR RESULT:"+string);
        communicateRefactorResult(string);
        return string;
        
    }
    public String getRenameInputMessage() {
        return "Please inform the new name.";
    }

    public ItemPointer[] findDefinition(RefactoringRequest request) {
        File editorFile = request.file;
        String s = "@@BIKE";
        s+=        "findDefinition";
        s+=        "|"+REF.getFileAbsolutePath(editorFile);
        s+=        "|"+request.getBeginLine();
        s+=        "|"+request.getBeginCol();
        s+=        "END@@";

        String string = makeAction(s, request);
        
        List<ItemPointer> l = new ArrayList<ItemPointer>();

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

        
        return l.toArray(new ItemPointer[0]);
        
    }
    
    /**
	 * Requests an inline local variable in the shell
     */
    public String inlineLocalVariable(RefactoringRequest request) {
        File editorFile = request.file;
        String s = "@@BIKE";
        s+=        "inlineLocalVariable";
        s+=        "|"+REF.getFileAbsolutePath(editorFile);
        s+=        "|"+request.getBeginLine();
        s+=        "|"+request.getBeginCol();
        s+=        "END@@";
//        System.out.println("Inline: "+s);
        String string = makeAction(s, request);
        
//        System.out.println("REFACTOR RESULT:"+string);
        communicateRefactorResult(string);
        return string;
    }
    
    /**
     * Requests an extract local variable in the shell
     */
    public String extractLocalVariable(RefactoringRequest request) {
        File editorFile = request.file;
        String s = "@@BIKE";
        s+=        "extractLocalVariable";
        s+=        "|"+REF.getFileAbsolutePath(editorFile);
        s+=        "|"+request.getBeginLine();
        s+=        "|"+request.getBeginCol();
        s+=        "|"+request.getEndLine();
        s+=        "|"+request.getEndCol();
        s+=        "|"+request.inputName;
        s+=        "END@@";
//        System.out.println("Extract: "+s);
        String string = makeAction(s, request);
//        System.out.println("REFACTOR RESULT:"+string);
        
        communicateRefactorResult(string);
        return string;
    }
    
    
    /**
     * @param string
     * @return list of strings affected by the refactoring.
     */
    private List<String> refactorResultAsList(String string) {
        List<String> l = new ArrayList<String>();
        
        if (string == null){
            return l;
        }
        
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
     * Sets the last refactor results.
     * 
     * @param string
     */
    private void communicateRefactorResult(String string) {
        List l = refactorResultAsList(string);
        setLastRefactorResults(new Object[]{this, l});
    }

    public boolean canExtract() {
        return true;
    }

    public boolean canRename() {
        return true;
    }

    public boolean canFindDefinition() {
        return true;
    }

    public boolean canInlineLocalVariable() {
        return true;
    }

    public boolean canExtractLocalVariable() {
        return true;
    }

    public boolean useDefaultRefactoringActionCycle() {
        return true;
    }

    public void canRefactorNature(IPythonNature pythonNature) throws RuntimeException {
        try {
            if (!pythonNature.isPython()) {
                throw new RuntimeException("Can only do actions dependent on Bycicle Repair Man in Python projects.");
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }




}
