package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * The code-completion engine that should be used inside strings
 * 
 * @author fabioz
 */
public class PyStringCodeCompletion extends AbstractPyCodeCompletion{

    /**
     * Epydoc fields (after @)
     */
    public static String[] EPYDOC_FIELDS = new String[]{
        "param",
        "type",
        "return",
        "rtype",
        "keyword",
        "raise",
        "ivar",
        "cvar",
        "var",
        "type",
        "type",
        "group",
        "sort",
        "see",
        "note",
        "attention",
        "bug",
        "warning",
        "version",
        "todo",
        "deprecated",
        "since",
        "status",
        "change",
        "requires",
        "precondition",
        "postcondition",
        "invariant",
        "author",
        "organization",
        "copyright",
        "license",
        "contact",
        "summary",
    };
    
    public List<ICompletionProposal> getCodeCompletionProposals(ITextViewer viewer, CompletionRequest request) throws CoreException, BadLocationException {
        ArrayList<ICompletionProposal> ret = new ArrayList<ICompletionProposal>();
        try{
            char c = request.doc.getChar(request.documentOffset - request.qualifier.length() -1);
            if(c == '@'){
                //ok, looking for epydoc filters
                for(String f:EPYDOC_FIELDS){
                    if(f.startsWith(request.qualifier)){
                        ret.add(new PyLinkedModeCompletionProposal(f, request.documentOffset - request.qlen, request.qlen, f.length(), 
                                PyCodeCompletionImages.getImageForType(IPyCodeCompletion.TYPE_EPYDOC), null, null, "", 0, PyCompletionProposal.ON_APPLY_DEFAULT, ""));
                    }
                }
            }
        }catch (BadLocationException e) {
            //just ignore it
        }
        return ret;
    }
	
	

}
