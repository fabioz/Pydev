/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.core.structure.FastStringBuffer;

/**
 * This class defines the information used for a code completion request.
 * 
 * @author Fabio Zadrozny
 */
public class CompletionRequest implements ICompletionRequest {

    /**
     * This is used on the AssistOverride: the activationToken is pre-specified
     * for some reason
     */
    public CompletionRequest(File editorFile, IPythonNature nature, IDocument doc, String activationToken, int documentOffset, int qlen,
            IPyCodeCompletion codeCompletion, String qualifier) {

        this.editorFile = editorFile;
        this.nature = nature;
        this.doc = doc;
        this.activationToken = activationToken;
        this.documentOffset = documentOffset;
        this.qlen = qlen;
        this.codeCompletion = codeCompletion;
        this.qualifier = qualifier;

        // the full qualifier is not set here
    }

    /**
     * This is the constructor that should be usually used. It will set the
     * activation token and the qualifier based on the document and its offset
     * 
     * @param editorFile
     * @param nature
     * @param doc
     * @param documentOffset
     * @param codeCompletion
     */
    public CompletionRequest(File editorFile, IPythonNature nature, IDocument doc, int documentOffset, IPyCodeCompletion codeCompletion) {
        //we need those set before requesting a py selection
        this.doc = doc;
        this.documentOffset = documentOffset;

        ActivationTokenAndQual act = getPySelection().getActivationTokenAndQual(false, true);
        this.activationToken = act.activationToken;
        this.qualifier = act.qualifier;
        this.isInCalltip = act.changedForCalltip;
        this.isInMethodKeywordParam = act.isInMethodKeywordParam;
        this.offsetForKeywordParam = act.offsetForKeywordParam;
        this.alreadyHasParams = act.alreadyHasParams;
        this.calltipOffset=  act.calltipOffset;


        int qlen = qualifier.length();

        this.editorFile = editorFile;
        this.nature = nature;
        this.qlen = qlen;
        this.codeCompletion = codeCompletion;

        this.fullQualifier = getPySelection().getActivationTokenAndQual(true)[1];
    }

    
    
    public CompletionRequest createCopyForKeywordParamRequest() {
        CompletionRequest request = new CompletionRequest(editorFile, nature, doc, this.offsetForKeywordParam, codeCompletion);
        request.isInMethodKeywordParam = false; //Just making sure it will not be another request for keyword params
        return request;
    }


    /**
     * This is the file where the request was created. Note that it might be
     * null (especially during the tests). It should be available at runtime and
     * may be used to resolve some path.
     */
    public File editorFile;
    public File getEditorFile(){
        return editorFile;
    }
    
    /**
     * Used when a completion is requested for an editor
     */
    public IPythonNature nature;
    public IPythonNature getNature(){
        return nature;
    }
    
    public IDocument doc;

    /**
     * The activation token of this request.
     * 
     * If it is requested at "m1.m2", the activationToken should be "m1" and the
     * qualifier should be "m2"
     * 
     * If requested at "m3", the activationToken should be empty and the
     * qualifier should be "m3"
     */
    public String activationToken;

    /**
     * The qualifier found to the cursor (will be used to filter the found
     * completions)
     */
    public String qualifier;

    /**
     * The full qualifier found (this is the complete token over where we have
     * the cursor). May be null.
     */
    public String fullQualifier;

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
    public IPyCodeCompletion codeCompletion;

    /**
     * Defines if we're getting the completions for a calltip
     * This happens when we're after a '(' or ',' in a method call. 
     */
    public boolean isInCalltip;
    
    /**
     * Defines if we're in a method call.
     */
    public boolean isInMethodKeywordParam;
    
    /**
     * Only really valid when isInMethodKeywordParam == true. Defines the offset of the method call.
     */
    public int offsetForKeywordParam;
    
    /**
     * Offset of the parens in a calltip.
     */
    public int calltipOffset;

    /**
     * Useful only if we're in a calltip
     */
    public boolean alreadyHasParams;

    /**
     * A selection object (cache) -- initialized on request
     */
    private PySelection ps;

    /**
     * This is a field that is filled in the code-completion engine indicating whether templates should be shown or not.
     */
    public boolean showTemplates = true;

    @Override
    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer();
        buffer.append("CompletionRequest[");
        buffer.append(" editorFile:");
        buffer.appendObject(editorFile);
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

    /**
     * @return a PySelection that represents the current request. If it doesn't exist, create it and cache
     * for other requests.
     */
    public PySelection getPySelection() {
        if(this.ps == null){
            this.ps = new PySelection(this.doc, this.documentOffset);
        }
        return this.ps;
    }

    /**
     * Cache for the module name
     */
    private String initialModule;
    
    /**
     * @return the module name where the completion request took place (may be null if there is no editor file associated)
     * @throws MisconfigurationException 
     */
    public String resolveModule() throws MisconfigurationException {
        if(initialModule == null && editorFile != null){
            initialModule = nature.resolveModule(editorFile);
        }
        return initialModule;
    }

}