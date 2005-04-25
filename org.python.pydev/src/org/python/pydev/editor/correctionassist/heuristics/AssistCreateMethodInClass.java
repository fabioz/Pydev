/*
 * Created on Apr 13, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.SourceModuleProposal;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;


/**
 * @author Fabio Zadrozny
 */
public class AssistCreateMethodInClass extends AbstractAssistCreate {
    private String getDeclToCreate(PySelection ps, int offset, String met){
        List toks = PyAction.getInsideParentesisToks(met);
        met = met.substring(0, met.indexOf('('));
        
        String delim = PyAction.getDelimiter(ps.getDoc());
        String indent = PyAction.getStaticIndentationString();
        
        String ret = delim;
        ret += indent+"def "+met+"(self";
        
        
        for (Iterator iter = toks.iterator(); iter.hasNext();) {
            ret += ", ";
            String element = (String) iter.next();
            ret += element;
        }
        ret += "):"+delim;
        
        ret += indent+indent+"'''"+delim;
        for (Iterator iter = toks.iterator(); iter.hasNext();) {
            String element = (String) iter.next();
            ret += indent+indent+"@param "+element+":"+delim;
        }
        ret += indent+indent+"'''"+delim;
        ret += indent+indent;
        
        
        return ret;
        
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.AbstractAssistCreate#getProposal(org.python.pydev.editor.actions.PySelection, org.python.pydev.ui.ImageCache, int, org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule, org.python.pydev.editor.codecompletion.revisited.modules.SourceModule)
     */
    protected PyCompletionProposal getProposal(PySelection ps, ImageCache imageCache, int offset, AbstractModule m, SourceModule s, Definition d) {
        try {
            Image img = null;
            if (imageCache != null)
                img = imageCache.get(UIConstants.ASSIST_NEW_CLASS);
            int len = ps.getStartLine().getOffset() + ps.getStartLine().getLength() - offset;
            String met = ps.getDoc().get(offset, len).trim();
            String methodToCreate = getDeclToCreate(ps, offset, met);
            SourceModuleProposal proposal = new SourceModuleProposal(methodToCreate, 0, 0, methodToCreate.length(), img,
                    "Create method "+met+" in class " + m.getName() + "." + d.value, null, null, s, IPyCompletionProposal.PRIORITY_DEFAULT);
            proposal.definition = d;
            proposal.addTo = SourceModuleProposal.ADD_TO_LAST_CLASS_LINE;
            return proposal;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     * This function might be overriden. It is called before getProposal() 
     * 
     * @param ps
     * @param imageCache
     * @param nature
     * @param l
     * @param offset
     * @param actTok
     * @param module
     */
    protected void getProposalFromModule(PySelection ps, ImageCache imageCache, PythonNature nature, List l, int offset, String actTok, SourceModule module) {
        try {
            int line = ps.getStartLineIndex();
            int col = offset - ps.getStartLine().getOffset();
            
            Definition[] defs = module.findDefinition(actTok, line, col, nature);
            while(defs.length >  0){
                if(defs[0].ast instanceof FunctionDef || defs[0].ast instanceof ClassDef){
                    AbstractModule m = defs[0].module;
                    if(!(m instanceof SourceModule)){
                        return;
                    }
                    final SourceModule s = (SourceModule) m;
                    final File modFile = s.getFile();
                    if(modFile.exists() && modFile.isFile()){
                        l.add(getProposal(ps, imageCache, offset, m, s, defs[0]));
                    }

                    return;
                    
                } else {
                    defs = module.findDefinition(defs[0].value, defs[0].line, defs[0].col, nature);
                }
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.AbstractAssistCreate#getProposal(org.python.pydev.editor.actions.PySelection, org.python.pydev.ui.ImageCache, int, org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule, org.python.pydev.editor.codecompletion.revisited.modules.SourceModule)
     */
    protected PyCompletionProposal getProposal(PySelection ps, ImageCache imageCache, int offset, SourceModule definedModule) {
        throw new RuntimeException("Should not be called.");
    }

}
