/*
 * Created on Sep 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.refactoring;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.editor.codecompletion.PythonShell;

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
     * Reference to a 'global python shell'
     */
    private static PythonShell pytonShell;
    
    /**
     * Instead of making all static, let's use a singleton... it may be useful...
     */
    private static PyRefactoring pyRefactoring;

    
    private PyRefactoring(){
        
    }
    
    public static PyRefactoring getPyRefactoring(){
        if (pyRefactoring == null){
            pyRefactoring = new PyRefactoring();
        }
        return pyRefactoring;
    }

    /**
     * @return
     * @throws CoreException
     * @throws IOException
     * 
     */
    private PythonShell getServerShell() throws IOException, CoreException {
        if(pytonShell == null){
            pytonShell = new PythonShell();
            pytonShell.startIt();
        }
        return pytonShell;
    }
    
    /**
     * This method can be used to write something to the server and get its answer.
     * 
     * @param str
     * @return
     */
    private String makeAction(String str){
        PythonShell pytonShell;
        try {
            pytonShell = getServerShell();
	        try {
	            pytonShell.write(str);
	 
	            return pytonShell.read();
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
     */
    public void extract(File editorFile, int beginLine, int beginCol, int endLine, int endCol, String name) {
        String s = "@@REFACTOR";
        s+=        "extractMethod";
        s+=        " "+editorFile.getAbsolutePath();
        s+=        " "+beginLine;
        s+=        " "+beginCol;
        s+=        " "+endLine;
        s+=        " "+endCol;
        s+=        " "+name;
        s+=        "END@@";
        System.out.println("Extract: "+s);
        String string = makeAction(s);
        System.out.println("REFACTOR RESULT:"+string);
        
    }

    /**
     * @param editorFile
     * @param beginLine
     * @param beginCol
     * @param name
     */
    public void rename(File editorFile, int beginLine, int beginCol, String name) {
        String s = "@@REFACTOR";
        s+=        "renameByCoordinates";
        s+=        " "+editorFile.getAbsolutePath();
        s+=        " "+beginLine;
        s+=        " "+beginCol;
        s+=        " "+name;
        s+=        "END@@";
        System.out.println("Extract: "+s);
        String string = makeAction(s);
        System.out.println("REFACTOR RESULT:"+string);
        
    }

}
