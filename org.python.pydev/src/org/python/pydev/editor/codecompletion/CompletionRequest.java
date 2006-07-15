/*
 * Created on Mar 9, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.ICompletionRequest;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;


/**
 * This class defines the information used for a code completion request.
 * @author Fabio Zadrozny
 */
public class CompletionRequest implements ICompletionRequest{


    /**
     * This is used on the AssistOverride: the activationToken is pre-specified for some reason
     */
    public CompletionRequest(File editorFile, IPythonNature nature, IDocument doc,
            String activationToken, int documentOffset, int qlen,
            PyCodeCompletion codeCompletion, 
            String qualifier){

        this.editorFile = editorFile;
        this.nature = nature;
        this.doc = doc;
        this.activationToken  = activationToken;
        this.documentOffset = documentOffset;
        this.qlen = qlen;
        this.codeCompletion = codeCompletion;
        this.qualifier = qualifier;
        
    }

    /**
     * This is the constructor that should be usually used. It will set the activation token
     * and the qualifier based on the document and its offset
     * 
     * @param editorFile
     * @param nature
     * @param doc
     * @param documentOffset
     * @param codeCompletion
     */
    public CompletionRequest(File editorFile, IPythonNature nature, IDocument doc,
            int documentOffset, PyCodeCompletion codeCompletion){

        ActivationTokenAndQual act = PySelection.getActivationTokenAndQual(doc, documentOffset, false, true); 
        this.activationToken = act.activationToken;
        this.qualifier = act.qualifier;
        this.isInCalltip = act.changedForCalltip;
        this.alreadyHasParams = act.alreadyHasParams;
        
        int qlen = qualifier.length();
        
        
        this.editorFile = editorFile;
        this.nature = nature;
        this.doc = doc;
        this.documentOffset = documentOffset;
        this.qlen = qlen;
        this.codeCompletion = codeCompletion;
        
    }

    /**
     * This is the file where the request was created. Note that it might be null (especially during the tests). 
     * It should be available at runtime and may be used to resolve some path.
     */
    public File editorFile;
    public IPythonNature nature;
    public IDocument doc;
    
    /**
     * The activation token of this request.
     * 
     * If it is requested at "m1.m2", the activationToken should be "m1" and the qualifier should be "m2"
     * 
     * If requested at "m3", the activationToken should be empty and the qualifier should be "m3"
     */
    public String activationToken; 
    public String qualifier; 
    
    /**
     * The offset in the document where this request was asked
     */
    public int documentOffset; 
    
    /**
     * The lenght of the qualifier (== qualifier.length())
     */
    public int qlen;
    
    /**
     * The engine for doing the code-completion
     */
    public PyCodeCompletion codeCompletion;
    
    /**
     * Defines if we're getting the completions for a calltip
     */
    public boolean isInCalltip;
    
    /**
     * Useful only if we're in a calltip
     */
    public boolean alreadyHasParams;
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("CompletionRequest[");
        buffer.append(" editorFile:");
        buffer.append(editorFile);
        buffer.append(" activationToken:");
        buffer.append(activationToken);
        buffer.append(" qualifier:");
        buffer.append(qualifier);
        buffer.append(" isInCalltip:");
        buffer.append(isInCalltip);
        buffer.append(" alreadyHasParams:");
        buffer.append(alreadyHasParams);
        buffer.append("]");
        return buffer.toString();
    }
}