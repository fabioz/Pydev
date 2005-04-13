/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.CompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.utils.REF;

/**
 * This class is a proposal that should be applied in a module and not in the original doc.
 * 
 * @author Fabio Zadrozny
 */
public class SourceModuleProposal extends CompletionProposal {

    public final SourceModule module;
    public PyEdit edit;
    public IDocument doc;
    public Definition definition;
    
    public static final int ADD_TO_DEFAULT = -1;
    public static final int ADD_TO_LAST_LINE = 0;
    public static final int ADD_TO_LAST_CLASS_LINE = 1;
    public int addTo = ADD_TO_LAST_LINE;
    
    public SourceModuleProposal(String replacementString, int replacementOffset, 
            int replacementLength, int cursorPosition, Image image, 
            String displayString, IContextInformation contextInformation, 
            String additionalProposalInfo, SourceModule s) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo);
        this.module = s;
    }

    /**
     * @see org.python.pydev.editor.codecompletion.CompletionProposal#apply(org.eclipse.jface.text.IDocument)
     */
    public void apply(IDocument dummy) {
        IPath path = new Path(module.getFile().getAbsolutePath());
        IEditorPart part = PydevPlugin.doOpenEditor(path, true);

        if(part instanceof PyEdit){
            edit = (PyEdit) part;
            doc = edit.getDocumentProvider().getDocument(edit.getEditorInput());
        }else{
            String contents = REF.getFileContents(module.getFile());
            doc = new Document(contents);
        }
        if(addTo == ADD_TO_LAST_LINE){ 
            fReplacementOffset = doc.getLength();
            
        }else if(addTo == ADD_TO_LAST_CLASS_LINE){
            int i = module.findAstEnd(definition.ast)-2;
            
            if(i == -1){
                i = doc.getNumberOfLines();
            }
            try {
                IRegion lineInformation = doc.getLineInformation(i);
                fReplacementOffset = lineInformation.getOffset()+lineInformation.getLength();
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
        super.apply(doc);
    }

    /**
     * @see org.python.pydev.editor.codecompletion.CompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
     */
    public Point getSelection(IDocument dummy) {
        Point sel = super.getSelection(doc);
        edit.setSelection(sel.x, sel.y);
        return null;
    }

    /**
     * @return
     * 
     */
    public String getReplacementStr() {
        return fReplacementString;
    }
    
}