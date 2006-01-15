/*
 * Created on Mar 9, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.ICompletionRequest;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.nature.PythonNature;


/**
 * This class defines the information used for a code completion request.
 * @author Fabio Zadrozny
 */
public class CompletionRequest implements ICompletionRequest{

    public CompletionRequest(PyEdit edit, IDocument doc,
            String activationToken, int documentOffset, int qlen,
            PyCodeCompletion codeCompletion, 
            String qualifier){
        this.editorFile = edit.getEditorFile();
        this.nature = (PythonNature) edit.getPythonNature();
        this.doc = doc;
        this.activationToken  = activationToken;
        this.documentOffset = documentOffset;
        this.qlen = qlen;
        this.codeCompletion = codeCompletion;
        this.qualifier = qualifier;
    }

    public CompletionRequest(File editorFile, PythonNature nature, IDocument doc,
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

    public CompletionRequest(File editorFile, PythonNature nature, IDocument doc,
            int documentOffset,
            PyCodeCompletion codeCompletion){

        String[] strs = PyCodeCompletion.getActivationTokenAndQual(doc, documentOffset); 
        this.activationToken = strs[0];
        this.qualifier = strs[1];
        int qlen = qualifier.length();
        
        
        this.editorFile = editorFile;
        this.nature = nature;
        this.doc = doc;
        this.documentOffset = documentOffset;
        this.qlen = qlen;
        this.codeCompletion = codeCompletion;
        
    }

    public File editorFile;
    public PythonNature nature;
    public IDocument doc;
    public String activationToken; 
    public String qualifier; 
    public int documentOffset; 
    public int qlen;
    public PyCodeCompletion codeCompletion;
}