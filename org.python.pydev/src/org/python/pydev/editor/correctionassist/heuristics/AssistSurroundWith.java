/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.AbstractTemplateCodeCompletion;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class AssistSurroundWith extends AbstractTemplateCodeCompletion implements IAssistProps {

    /**
     * @throws BadLocationException
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.core.docutils.PySelection, org.python.pydev.core.bundle.ImageCache)
     */
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature, PyEdit edit, int offset) throws BadLocationException {
        
        ArrayList<ICompletionProposal> l = new ArrayList<ICompletionProposal>();
        String indentation = PyAction.getStaticIndentationString(edit);
        
        ps.selectCompleteLine();
        
        int start = ps.getStartLine().getOffset();
        int end = ps.getEndLine().getOffset()+ps.getEndLine().getLength();

        //delimiter to use
        String delimiter = PyAction.getDelimiter(ps.getDoc());
        
        //get the 1st char (determines indent)
        int firstCharPosition = PySelection.getFirstCharRelativePosition(ps.getDoc(), start);
        FastStringBuffer startIndentBuffer = new FastStringBuffer(firstCharPosition+1);
        int i = 0;
        while(i < firstCharPosition){
            startIndentBuffer.append(" ");
            i++;
        }
        final String startIndent = startIndentBuffer.toString();
        
        //code to be surrounded
        String surroundedCode = ps.getDoc().get(start, end-start);
        surroundedCode = indentation+surroundedCode.replaceAll(delimiter, delimiter+indentation);
        
        //region
        IRegion region = ps.getRegion();
        TemplateContext context = createContext(edit.getPySourceViewer(), region, ps.getDoc());

        //not static because we need the actual code.
        String[] replace0to3 = new String[]{startIndent, delimiter, surroundedCode, delimiter, startIndent, delimiter, startIndent, indentation, indentation};
        String[] replace4toEnd = new String[]{startIndent, delimiter, surroundedCode, delimiter, startIndent, indentation};
        
        //actually create the template
        for (int iComp = 0, iRep=0; iComp < SURROUND_WITH_COMPLETIONS.length; iComp+= 2,iRep++) {
            String comp = SURROUND_WITH_COMPLETIONS[iComp];
            if(iRep < 4){
                comp = StringUtils.format(comp, (Object [])replace0to3);
            }else{
                comp = StringUtils.format(comp, (Object [])replace4toEnd);
            }
            
            Template t = new Template("Surround with", SURROUND_WITH_COMPLETIONS[iComp+1], "", comp, false);
            l.add(new TemplateProposal(t, context, region, imageCache.get(UIConstants.COMPLETION_TEMPLATE), 5){
                @Override
                public String getAdditionalProposalInfo() {
                    return startIndent+super.getAdditionalProposalInfo();
                }
            });
        }

        return l;
    }

    /**
     * Template completions available for surround with... They %s will be replaced later for the actual code/indentation.
     * 
     * Could be refactored so that we don't have to put the actual indent here (creating a subclass of PyDocumentTemplateContext)
     * Also, if that refactoring was done, we could give an interface for the user to configure those templates better.
     * 
     * Another nice thing may be analyzing the current context for local variables so that
     * for item in collection could have 'good' choices for the collection variable based on the local variables.
     */
    public static final String[] SURROUND_WITH_COMPLETIONS = new String[]{
        "%stry:%s%s%s%sexcept${cursor}:%s%s%sraise", "try..except",
        "%stry:%s%s%s%sexcept (${RuntimeError}, ), e:%s%s%s${raise}${cursor}", "try..except (RuntimeError, ), e",
        "%stry:%s%s%s%sfinally:%s%s%s${pass}", "try..finally",
        "%sif ${True}:%s%s%s%selse:%s%s%s${pass}", "if..else",
        
        
        "%swhile ${True}:%s%s%s%s%s", "while",
        "%sfor ${item} in ${collection}:%s%s%s%s%s${cursor}", "for",
        "%sif ${True}:%s%s%s%s%s${cursor}", "if",
        "%swith ${var}:%s%s%s%s%s${cursor}", "with",
    };

    
    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.core.docutils.PySelection)
     */
    public boolean isValid(PySelection ps, String sel, PyEdit edit, int offset) {
        return ps.getTextSelection().getLength() > 0;
    }

    public List<Object> getCodeCompletionProposals(ITextViewer viewer, CompletionRequest request) throws CoreException,
            BadLocationException {
        throw new RuntimeException("Not implemented: completions should be gotten from the IAssistProps interface.");
    }
    
    

}
