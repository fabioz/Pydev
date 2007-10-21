package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

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
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledToken;
import org.python.pydev.plugin.PydevPlugin;

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

    protected JavaElementToken(String rep, String doc, String args, String parentPackage, int type, IJavaElement javaElement,
            int completionProposalKind, int completionProposalFlags, int completionProposalAdditionalFlags,
            char[] completionPropsoalSignature) {
        super(rep, doc, args, parentPackage, type);
        this.javaElement = javaElement;
        this.completionProposalKind = completionProposalKind;
        this.completionProposalFlags = completionProposalFlags;
        this.completionProposalAdditionalFlags = completionProposalAdditionalFlags;
        this.completionPropsoalSignature = completionPropsoalSignature;
    }
    
    public JavaElementToken(String rep, String doc, String args, String parentPackage, int type, IJavaElement javaElement,
            CompletionProposal completionProposal) {
        this(rep, doc, args, parentPackage, type, javaElement, completionProposal.getKind(), completionProposal.getFlags(), 
                completionProposal.getAdditionalFlags(), completionProposal.getSignature());
    }
    
    

    @Override
    public Image getImage() {
        CompletionProposalLabelProvider provider = new CompletionProposalLabelProvider();
        CompletionProposal generatedProposal = CompletionProposal.create(completionProposalKind, 0);
        generatedProposal.setFlags(completionProposalFlags);
        generatedProposal.setAdditionalFlags(completionProposalAdditionalFlags);
        generatedProposal.setDeclarationSignature(completionPropsoalSignature);

        //uses: kind, flags, signature to create an image. 
        ImageDescriptor descriptor = provider.createImageDescriptor(generatedProposal);
        return descriptor.createImage();
    }

    
    
    @Override
    public String getDocStr() {
        if (javaElement instanceof IMember) {
            IMember member = (IMember) javaElement;
            try {
                return extractJavadoc(member, new NullProgressMonitor());
            } catch (JavaModelException e) {
                //just ignore it in this case (that may happen when no docstring is available)
            } catch (Exception e) {
                PydevPlugin.log("Error getting completion for "+member,e);
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
            if (reader != null)
                return getString(reader);
        }
        return null;
    }

    private Reader getContentReader(IMember member, IProgressMonitor monitor) throws JavaModelException {
        Reader contentReader = JavadocContentAccess.getContentReader(member, true);
        if (contentReader != null)
            return contentReader;

        if (member.getOpenable().getBuffer() == null) { // only if no source available
            String s = member.getAttachedJavadoc(monitor);
            if (s != null)
                return new StringReader(s);
        }
        return null;
    }

    /**
     * Gets the reader content as a String
     */
    private static String getString(Reader reader) {
        StringBuffer buf = new StringBuffer();
        char[] buffer = new char[1024];
        int count;
        try {
            while ((count = reader.read(buffer)) != -1)
                buf.append(buffer, 0, count);
        } catch (IOException e) {
            return null;
        }
        return buf.toString();
    }


}
