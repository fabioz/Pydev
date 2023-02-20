/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 9, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.ast.codecompletion.revisited.AbstractASTManager;
import org.python.pydev.core.ICompletionRequest;
import org.python.pydev.core.IFilterToken;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ITokenCompletionRequest;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQualifier;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * This class defines the information used for a code completion request.
 *
 * @author Fabio Zadrozny
 */
public final class CompletionRequest implements ICompletionRequest, ITokenCompletionRequest {

    public IFilterToken filterToken;

    /**
     * This is used on the AssistOverride: the activationToken is pre-specified
     * for some reason
     */
    public CompletionRequest(File editorFile, IPythonNature nature, IDocument doc, String activationToken,
            int documentOffset, int qlen, IPyCodeCompletion codeCompletion, String qualifier,
            boolean useSubstringMatchInCodeCompletion) {

        this.editorFile = editorFile;
        this.nature = nature;
        this.doc = doc;
        this.activationToken = activationToken;
        this.documentOffset = documentOffset;
        this.qlen = qlen;
        this.codeCompletion = codeCompletion;
        this.qualifier = qualifier;

        // the full qualifier is not set here
        this.fullQualifier = null;

        this.calltipOffset = 0;
        this.alreadyHasParams = false;
        this.offsetForKeywordParam = 0;
        this.useSubstringMatchInCodeCompletion = useSubstringMatchInCodeCompletion;
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
    public CompletionRequest(File editorFile, IPythonNature nature, IDocument doc, int documentOffset,
            IPyCodeCompletion codeCompletion, boolean useSubstringMatchInCodeCompletion) {
        //we need those set before requesting a py selection
        this.doc = doc;
        this.documentOffset = documentOffset;

        ActivationTokenAndQualifier act = getPySelection().getActivationTokenAndQualifier(false, true);
        this.activationToken = act.activationToken;
        this.qualifier = act.qualifier;
        this.isInCalltip = act.changedForCalltip;
        this.isInMethodKeywordParam = act.isInMethodKeywordParam;
        this.offsetForKeywordParam = act.offsetForKeywordParam;
        this.alreadyHasParams = act.alreadyHasParams;
        this.calltipOffset = act.calltipOffset;

        int qlen = qualifier.length();

        this.editorFile = editorFile;
        this.nature = nature;
        this.qlen = qlen;
        this.codeCompletion = codeCompletion;

        this.fullQualifier = getPySelection().getActivationTokenAndQualifier(true)[1];
        this.useSubstringMatchInCodeCompletion = useSubstringMatchInCodeCompletion;
    }

    public CompletionRequest createCopyForKeywordParamRequest() {
        CompletionRequest request = new CompletionRequest(editorFile, nature, doc, this.offsetForKeywordParam,
                codeCompletion, this.useSubstringMatchInCodeCompletion);
        request.isInMethodKeywordParam = false; //Just making sure it will not be another request for keyword params
        return request;
    }

    /**
     * This is the file where the request was created. Note that it might be
     * null (especially during the tests). It should be available at runtime and
     * may be used to resolve some path.
     */
    public File editorFile;

    @Override
    public File getEditorFile() {
        return editorFile;
    }

    /**
     * Used when a completion is requested for an editor
     */
    public IPythonNature nature;

    @Override
    public IPythonNature getNature() {
        return nature;
    }

    public final IDocument doc;

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
    public final String qualifier;

    /**
     * The full qualifier found (this is the complete token over where we have
     * the cursor). May be null.
     */
    public final String fullQualifier;

    /**
     * The offset in the document where this request was asked
     */
    public final int documentOffset;

    /**
     * The lenght of the qualifier (== qualifier.length())
     */
    public final int qlen;

    /**
     * The engine for doing the code-completion
     */
    public final IPyCodeCompletion codeCompletion;

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
    public final int offsetForKeywordParam;

    /**
     * Offset of the parens in a calltip.
     */
    public final int calltipOffset;

    /**
     * Useful only if we're in a calltip
     */
    public final boolean alreadyHasParams;

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
        if (this.ps == null) {
            this.ps = new PySelection(this.doc, this.documentOffset);
        }
        return this.ps;
    }

    /**
     * Cache for the module name
     */
    private String initialModule;

    /**
     * Cache for the source module created.
     */
    private IModule module;

    public final boolean useSubstringMatchInCodeCompletion;

    private IProgressMonitor cancelMonitor;

    /**
     * @return the module name where the completion request took place (may be null if there is no editor file associated)
     * @throws MisconfigurationException
     */
    public String resolveModule() throws MisconfigurationException {
        if (initialModule == null && editorFile != null) {
            initialModule = nature.resolveModule(editorFile);
        }
        return initialModule;
    }

    /**
     * @param state
     * @param astManager
     * @return
     * @throws MisconfigurationException
     */
    @Override
    public IModule getModule() throws MisconfigurationException {
        if (module == null) {
            module = AbstractASTManager.createModule(this.editorFile, this.doc, this.nature);
        }
        return module;
    }

    @Override
    public String getActivationToken() {
        return this.activationToken;
    }

    @Override
    public void setActivationToken(String activationToken) {
        this.activationToken = activationToken;
    }

    @Override
    public String getQualifier() {
        return qualifier;
    }

    @Override
    public int getLine() throws BadLocationException {
        return doc.getLineOfOffset(documentOffset);
    }

    @Override
    public int getCol() throws BadLocationException {
        IRegion region = doc.getLineInformationOfOffset(documentOffset);
        int col = documentOffset - region.getOffset();
        return col;
    }

    public IProgressMonitor getCancelMonitor() {
        return this.cancelMonitor;
    }

    public void setCancelMonitor(IProgressMonitor monitor) {
        this.cancelMonitor = monitor;
    }

}