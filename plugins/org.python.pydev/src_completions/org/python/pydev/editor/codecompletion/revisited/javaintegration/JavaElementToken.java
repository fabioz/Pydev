/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavadocContentAccess;
import org.eclipse.jdt.ui.text.java.CompletionProposalLabelProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledToken;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.utils.Reflection;

/**
 * This is the token that encapsulates a java element.
 *
 * @author Fabio
 */
public class JavaElementToken extends CompiledToken {

    public IJavaElement javaElement;

    private int completionProposalKind;

    private int completionProposalFlags;

    private int completionProposalAdditionalFlags;

    private char[] completionPropsoalSignature;

    /**
     * If not null, this image is returned (otherwise, the image is calculated)
     */
    private Image image;

    /**
     * Used for backward compatibility to eclipse 3.2
     */
    static boolean HAS_ADDITIONAL_FLAGS = true;
    static {
        try {
            Method m = Reflection.findMethod(CompletionProposal.class, "getAdditionalFlags");
            if (m == null) {
                HAS_ADDITIONAL_FLAGS = false;
            }
        } catch (Throwable e) {
            HAS_ADDITIONAL_FLAGS = false;
        }
    }

    protected JavaElementToken(String rep, String doc, String args, String parentPackage, int type,
            IJavaElement javaElement, int completionProposalKind, int completionProposalFlags,
            int completionProposalAdditionalFlags, char[] completionPropsoalSignature) {
        super(rep, doc, args, parentPackage, type);
        this.javaElement = javaElement;
        this.completionProposalKind = completionProposalKind;
        this.completionProposalFlags = completionProposalFlags;
        this.completionProposalAdditionalFlags = completionProposalAdditionalFlags;
        this.completionPropsoalSignature = completionPropsoalSignature;
    }

    public JavaElementToken(String rep, String doc, String args, String parentPackage, int type,
            IJavaElement javaElement, CompletionProposal completionProposal) {
        super(rep, doc, args, parentPackage, type);
        this.javaElement = javaElement;
        this.completionProposalKind = completionProposal.getKind();
        this.completionProposalFlags = completionProposal.getFlags();
        if (HAS_ADDITIONAL_FLAGS) {
            this.completionProposalAdditionalFlags = completionProposal.getAdditionalFlags();
        }
        this.completionPropsoalSignature = completionProposal.getSignature();
    }

    public JavaElementToken(String rep, String doc, String args, String parentPackage, int type,
            IJavaElement javaElement, Image image) {
        super(rep, doc, args, parentPackage, type);
        this.javaElement = javaElement;
        this.image = image;
    }

    @Override
    public Image getImage() {
        if (this.image != null) {
            return this.image;
        }
        CompletionProposalLabelProvider provider = new CompletionProposalLabelProvider();
        CompletionProposal generatedProposal = CompletionProposal.create(completionProposalKind, 0);
        generatedProposal.setFlags(completionProposalFlags);
        if (HAS_ADDITIONAL_FLAGS) {
            generatedProposal.setAdditionalFlags(completionProposalAdditionalFlags);
        }
        generatedProposal.setDeclarationSignature(completionPropsoalSignature);
        generatedProposal.setSignature(completionPropsoalSignature);

        //uses: kind, flags, signature to create an image. 
        ImageDescriptor descriptor = provider.createImageDescriptor(generatedProposal);
        return descriptor.createImage();
    }

    @Override
    public String getDocStr() {
        if (javaElement instanceof IMember) {
            IMember member = (IMember) javaElement;
            try {
                return StringUtils.extractTextFromHTML(extractJavadoc(member, new NullProgressMonitor()));
            } catch (JavaModelException e) {
                //just ignore it in this case (that may happen when no docstring is available)
            } catch (Exception e) {
                Log.log("Error getting completion for " + member, e);
            }
        }
        return null;
    }

    //Helpers to get the docstring (adapted from org.eclipse.jdt.internal.ui.text.java.ProposalInfo)

    /**
     * Extracts the javadoc for the given <code>IMember</code> and returns it
     * as HTML.
     *
     * @param member the member to get the documentation for
     * @param monitor a progress monitor
     * @return the javadoc for <code>member</code> or <code>null</code> if
     *         it is not available
     * @throws JavaModelException if accessing the javadoc fails
     * @throws IOException if reading the javadoc fails
     */
    private String extractJavadoc(IMember member, IProgressMonitor monitor) throws JavaModelException, IOException {
        if (member != null) {
            Reader reader = getContentReader(member, monitor);
            if (reader != null) {
                return getString(reader);
            }
        }
        return null;
    }

    private Reader getContentReader(IMember member, IProgressMonitor monitor) throws JavaModelException {
        Reader contentReader = JavadocContentAccess.getContentReader(member, true);
        if (contentReader != null) {
            return contentReader;
        }

        if (member.getOpenable().getBuffer() == null) { // only if no source available
            String s = member.getAttachedJavadoc(monitor);
            if (s != null) {
                return new StringReader(s);
            }
        }
        return null;
    }

    /**
     * Gets the reader content as a String
     */
    private static String getString(Reader reader) {
        FastStringBuffer buf = new FastStringBuffer();
        char[] buffer = new char[1024];
        int count;
        try {
            while ((count = reader.read(buffer)) != -1) {
                buf.append(buffer, 0, count);
            }
        } catch (IOException e) {
            return null;
        }
        return buf.toString();
    }

}
