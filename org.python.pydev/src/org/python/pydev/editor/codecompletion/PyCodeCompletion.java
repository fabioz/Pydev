/*
 * Created on Aug 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Dmoore
 * @author Fabio Zadrozny
 */
public class PyCodeCompletion {
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer,
     *      int)
     */
    int docBoundary = -1; // the document prior to the activation token

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
     * 
     * @param theCode
     * @param theActivationToken
     * @return
     */
    public List autoComplete(java.lang.String theCode,
            java.lang.String theActivationToken) {
        List theList = new ArrayList();
        String s = new String();
        File tmp = null;

        try {
            // get the inspect.py file from the package:
            s = getAutoCompleteScript();

        } catch (CoreException e) {

            e.printStackTrace();
        }

        try {
            //We actually write a file with all of our code to a temporary location,
            //so that we can get its code completion from the typper.py script.
            //
            //TODO: there must be a faster strategy...
            //
            tmp = bufferContent(theCode);

            if (tmp == null) {
                System.out
                        .println("DBG:bufferContent() null. No tip for you!!");
                return theList;
            }
            
            String ss = new String("python " + s + " " + tmp.getAbsolutePath());

            Process p = Runtime.getRuntime().exec(ss);
            BufferedReader in = new BufferedReader(new InputStreamReader(p
                    .getInputStream()));
            String str;
            while ((str = in.readLine()) != null) {
                
                if (!str.startsWith("tip: ")){
                    continue;
                }
                
                str = str.substring(5);

                theList.add(str);
            }
            in.close();
            InputStream eInput = p.getErrorStream();
            BufferedReader eIn = new BufferedReader(new InputStreamReader(
                    eInput));

            while ((str = eIn.readLine()) != null) {
                //System.out.println("error output: " + str);
            }
            p.waitFor();
        } catch (IOException e) {

            e.printStackTrace();
        } catch (InterruptedException e) {

            e.printStackTrace();
        }
        return theList;
    }

    /**
     * We actually write a file with all of our code to a temporary location,
     * so that we can get its code completion from the typper.py script.     
     * 
     * @param theCode code to be written to file.
     */
    private File bufferContent(java.lang.String theCode) {
        //
        try {
            // Create temp file.
            File temp = File.createTempFile("PyDev", ".tmp");

            // Delete temp file when program exits.
            temp.deleteOnExit();

            // Write to temp file
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            out.write(theCode);
            out.close();
            return temp;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 
     * @return the script to get the variables.
     * 
     * @throws CoreException
     */
    public static String getAutoCompleteScript() throws CoreException {
        String targetExec = "tipper.py";

        IPath relative = new Path("PySrc").addTrailingSeparator().append(
                targetExec);

        Bundle bundle = PydevPlugin.getDefault().getBundle();

        URL bundleURL = Platform.find(bundle, relative);
        URL fileURL;
        try {
            fileURL = Platform.asLocalURL(bundleURL);
            String filePath = new File(fileURL.getPath()).getAbsolutePath();

            return filePath;
        } catch (IOException e) {
            throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR,
                    "Can't find python debug script", null));
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
        return theDoc.substring(this.docBoundary + 1, documentOffset);
    }

}
