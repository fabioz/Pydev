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
        File tipperFile=null;

        try {
            tipperFile = getAutoCompleteScript();

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
            
            //now we have it in the modules... import and call tipper with the code...
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
                    continue;
                }
                
                str = str.substring(5);

                theList.add(str);
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
     * @return the script to get the variables.
     * 
     * @throws CoreException
     */
    public static File getAutoCompleteScript() throws CoreException {
        String targetExec = "tipper.py";

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
