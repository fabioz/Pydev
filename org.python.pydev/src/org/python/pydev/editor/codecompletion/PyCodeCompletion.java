/*
 * Created on Aug 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.osgi.framework.Bundle;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.Location;
import org.python.pydev.editor.model.ModelUtils;
import org.python.pydev.editor.model.Scope;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Dmoore
 * @author Fabio Zadrozny
 */
public class PyCodeCompletion {
    
    
    int docBoundary = -1; // the document prior to the activation token
    private PythonShell pytonShell;

    /**
     * @param theDoc: the whole document as a string.
     * @param documentOffset: the cursor position
     */
    String partialDocument(String theDoc, int documentOffset) {
        if (this.docBoundary < 0) {
            calcDocBoundary(theDoc, documentOffset);
        }
        if(this.docBoundary != -1){
	        String before = theDoc.substring(0, this.docBoundary);
	        return before;
        }
        return "";

    }

    /**
     * Returns a list with the tokens to use for autocompletion.
     * @param edit
     * @param doc
     * @param documentOffset
     * 
     * @param theCode
     * @param theActivationToken
     * @return
     */
    public List autoComplete(PyEdit edit, IDocument doc, int documentOffset, java.lang.String theCode,
            java.lang.String theActivationToken) {
        if(PyCodeCompletionPreferencesPage.useServerTipEnviroment()){
            return serverCompletion(theActivationToken, edit, doc, documentOffset, theCode);
        }else{
            return consoleCompletion(theCode);
        }
    }


    /**
     * @param edit
     * @param doc
     * @param documentOffset
     * @param theCode
     * @param theCode
     */
    private List serverCompletion(String theActivationToken, PyEdit edit, IDocument doc, int documentOffset, String theCode) {
        List theList = new ArrayList();
        PythonShell serverShell = null;
        try {
            serverShell = getServerShell();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        
        String docToParse = getDocToParse(doc, documentOffset);

        String trimmed = theActivationToken.replace('.',' ').trim();
        if (trimmed.equals("") == false){
            
            List completions;
            if (trimmed.equals("self")){
                Location loc = Location.offsetToLocation(doc, documentOffset);
                AbstractNode closest = ModelUtils.getLessOrEqualNode(edit.getPythonModel(),loc);

                Scope scope = closest.getScope().findContainingClass();
                String token = scope.getStartNode().getName();
                completions = serverShell.getClassCompletions(token, docToParse);
            }else{
                completions = serverShell.getTokenCompletions(trimmed, docToParse);
            }
            theList.addAll(completions);
            
        }
        else{ //go to globals
//            System.out.println("simpleCompletion - ''");
            List completions = serverShell.getGlobalCompletions(docToParse);
            theList.addAll(completions);
            
        }
        return theList;
        
    }

    /**
     * @param doc
     * @param documentOffset
     * @return
     */
    private String getDocToParse(IDocument doc, int documentOffset) {
        String wholeDoc = doc.get();
        String newDoc = "";
        try {
            int lineOfOffset = doc.getLineOfOffset(documentOffset);
            IRegion lineInformation = doc.getLineInformation(lineOfOffset);
            
            int docLength = doc.getLength();
            String src = doc.get(lineInformation.getOffset(), documentOffset-lineInformation.getOffset());
            
            String spaces="";
            for (int i = 0; i < src.length(); i++) {
                if(src.charAt(i) != ' '){
                    break;
                }
                spaces += ' ';
            }
            
            newDoc = wholeDoc.substring(0, lineInformation.getOffset());
            newDoc += spaces+"pass\n";
            newDoc += wholeDoc.substring(lineInformation.getOffset() + lineInformation.getLength(), docLength);
            
//            System.out.println("DOC:"+newDoc);
            
        } catch (BadLocationException e1) {
            e1.printStackTrace();
        }
        return newDoc;
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
     * @param theCode
     */
    private List consoleCompletion(java.lang.String theCode) {
        List theList = new ArrayList();
        File tipperFile=null;
        try {
            
        tipperFile = getAutoCompleteScript(false);

        } catch (CoreException e) {

            e.printStackTrace();
        }

        try {
            Process p = Runtime.getRuntime().exec(new String[]{"python"});
            
            //we have the process...
            OutputStreamWriter writer = new OutputStreamWriter(p.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(p
                    .getInputStream()));
            BufferedReader eIn = new BufferedReader(new InputStreamReader(
                    p.getErrorStream()));
            
            //we have to put the tipper in sys.path
            writer.write("import sys\n");
            writer.write("sys.path.insert(0,r'"+tipperFile.getParent()+"')\n");
            
            writer.write("from tipper import GenerateTip\n");
            
            theCode = theCode.replaceAll("\r","");
            theCode = theCode.replaceAll("\n","\\\\n");

            theCode = theCode.replaceAll("'","@l@l@*"); //TODO: that's a really bad way to do it...

            writer.write("s = '"+theCode+"'\n");
            writer.write("s = s.replace('@l@l@*', '\\'')\n");

            writer.write("GenerateTip(s)\n\n\n");
            writer.flush();
            writer.close();
            
            String str;
            while ((str = in.readLine()) != null) {
                if (!str.startsWith("tip: ")){
//                    System.out.println("std output: " + str);
                    continue;
                }
                
                str = str.substring(5);

                theList.add(new String[]{str,""});
            }
            in.close();

            while ((str = eIn.readLine()) != null) {
//                System.out.println("error output: " + str);
            }
            eIn.close();
            p.waitFor();
        } catch (IOException e) {

            e.printStackTrace();
        } catch (InterruptedException e) {

            e.printStackTrace();
        }
        return theList;
    }

    /**
     * 
     * @param useSimpleTipper
     * @return the script to get the variables.
     * 
     * @throws CoreException
     */
    public static File getAutoCompleteScript(boolean useSimpleTipper) throws CoreException {
        if(useSimpleTipper){
            return getScriptWithinPySrc("simpleTipper.py");
        }else{
            return getScriptWithinPySrc("tipper.py");
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
                    "Can't find python debug script", null));
        }
    }

    public static File getImageWithinIcons(String icon) throws CoreException {

        IPath relative = new Path("icons").addTrailingSeparator().append(
                icon);

        Bundle bundle = PydevPlugin.getDefault().getBundle();

        URL bundleURL = Platform.find(bundle, relative);
        URL fileURL;
        try {
            fileURL = Platform.asLocalURL(bundleURL);
            File f = new File(fileURL.getPath());

            return f;
        } catch (IOException e) {
            throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR,
                    "Can't find image", null));
        }
    }

    /**
     * The docBoundary should get until the last line before the one
     * we are editing.
     * 
     * @param qualifier
     * @param documentOffset
     * @param proposals
     */
    public void calcDocBoundary(String theDoc, int documentOffset) {
        this.docBoundary = theDoc.substring(0, documentOffset)
                .lastIndexOf('\n');
    }
    
    /**
     * Returns the activation token.
     * 
     * @param theDoc
     * @param documentOffset
     * @return
     */
    public String getActivationToken(String theDoc, int documentOffset) {
        if (this.docBoundary < 0) {
            calcDocBoundary(theDoc, documentOffset);
        }
        String str = theDoc.substring(this.docBoundary + 1, documentOffset);
        if (str.endsWith(" ")){
            str = " ";
        }
        return str;
    }

}
