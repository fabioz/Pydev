/*
 * Created on Aug 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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
     * @param theDoc:
     *            the whole document as a string.
     * @param documentOffset:
     *            the cursor position
     */
    String partialDocument(String theDoc, int documentOffset) {
        if (this.docBoundary < 0) {
            calcDocBoundary(theDoc, documentOffset);
        }
        if (this.docBoundary != -1) {
            String before = theDoc.substring(0, this.docBoundary);
            return before;
        }
        return "";

    }

    /**
     * Returns a list with the tokens to use for autocompletion.
     * 
     * @param edit
     * @param doc
     * @param documentOffset
     * 
     * @param theActivationToken
     * @return
     */
    public List autoComplete(PyEdit edit, IDocument doc, int documentOffset,
            java.lang.String theActivationToken) {
        List theList = new ArrayList();
        PythonShell serverShell = null;
        try {
            serverShell = PythonShell.getServerShell();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        String docToParse = getDocToParse(doc, documentOffset);
        
        String trimmed = theActivationToken.replace('.', ' ').trim();
        
        String importsTipper = getImportsTipperStr(theActivationToken, edit, doc, documentOffset);
        if (importsTipper.length()!=0) { //may be space.
        
            List completions = serverShell.getImportCompletions(importsTipper);
            theList.addAll(completions);
        
        } else if (trimmed.equals("") == false
                && theActivationToken.indexOf('.') != -1) {
        
            List completions;
            if (trimmed.equals("self")) {
                Location loc = Location.offsetToLocation(doc, documentOffset);
                AbstractNode closest = ModelUtils.getLessOrEqualNode(edit
                        .getPythonModel(), loc);
        
                if(closest == null){
                    completions = serverShell.getTokenCompletions(trimmed,
                            docToParse);
                }else{
                    Scope scope = closest.getScope().findContainingClass();
                    String token = scope.getStartNode().getName();
                    completions = serverShell
                            .getClassCompletions(token, docToParse);
                }
            } else {
                completions = serverShell.getTokenCompletions(trimmed,
                        docToParse);
            }
            theList.addAll(completions);
        
        } else { //go to globals
            List completions = serverShell.getGlobalCompletions(docToParse);
            theList.addAll(completions);
        
        }
        return theList;
    }


    /**
     * @param theActivationToken
     * @param edit
     * @param doc
     * @param documentOffset
     * @return
     */
    public String getImportsTipperStr(String theActivationToken, PyEdit edit,
            IDocument doc, int documentOffset) {
        String importMsg = "";
        try {
            
            IRegion region = doc.getLineInformationOfOffset(documentOffset);
            String string = doc.get(region.getOffset(), documentOffset-region.getOffset());
            int fromIndex = string.indexOf("from");
            int importIndex = string.indexOf("import");

            if(fromIndex  != -1 || importIndex != -1){
                string = string.replaceAll("#.*", ""); //remove comments 
                String[] strings = string.split(" ");
                
                for (int i = 0; i < strings.length; i++) {
                    if(strings[i].equals("from")==false && strings[i].equals("import")==false){
                        if(importMsg.length() != 0){
                            importMsg += '.';
                        }
                        importMsg += strings[i];
                    }
                }
                
                if(fromIndex  != -1 && importIndex != -1){
                    if(strings.length == 3){
                        importMsg += '.';
                    }
                }
            }else{
                return "";
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        if (importMsg.indexOf(".") == -1){
            return " ";
        }
        if (importMsg.length() > 0 && importMsg.endsWith(".") == false ){
            importMsg = importMsg.substring(0, importMsg.lastIndexOf('.'));
        }
        
        return importMsg;
    }

    /**
     * @param doc
     * @param documentOffset
     * @return
     */
    public String getDocToParse(IDocument doc, int documentOffset) {
        String wholeDoc = doc.get();
        String newDoc = "";
        try {
            int lineOfOffset = doc.getLineOfOffset(documentOffset);
            IRegion lineInformation = doc.getLineInformation(lineOfOffset);

            int docLength = doc.getLength();
            String src = doc.get(lineInformation.getOffset(), documentOffset
                    - lineInformation.getOffset());

            String spaces = "";
            for (int i = 0; i < src.length(); i++) {
                if (src.charAt(i) != ' ') {
                    break;
                }
                spaces += ' ';
            }

            newDoc = wholeDoc.substring(0, lineInformation.getOffset());
            newDoc += spaces + "pass\n";
            newDoc += wholeDoc.substring(lineInformation.getOffset()
                    + lineInformation.getLength(), docLength);

        } catch (BadLocationException e1) {
            e1.printStackTrace();
        }
        return "\n"+newDoc;
    }

    /**
     * 
     * @param useSimpleTipper
     * @return the script to get the variables.
     * 
     * @throws CoreException
     */
    public static File getAutoCompleteScript() throws CoreException {
        return getScriptWithinPySrc("simpleTipper.py");
    }

    /**
     * 
     * @return the script to get the variables.
     * 
     * @throws CoreException
     */
    public static File getScriptWithinPySrc(String targetExec)
            throws CoreException {

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

        IPath relative = new Path("icons").addTrailingSeparator().append(icon);

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
     * The docBoundary should get until the last line before the one we are
     * editing.
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
        if (str.endsWith(" ")) {
            return " ";
        }
        
        int lastSpaceIndex = str.lastIndexOf(' ');
        int lastParIndex = str.lastIndexOf('(');
        
        if(lastParIndex != -1 || lastSpaceIndex != -1){
            int lastIndex = lastSpaceIndex > lastParIndex ? lastSpaceIndex : lastParIndex;
            return str.substring(lastIndex+1, str.length());
        }
        return str;
    }

}