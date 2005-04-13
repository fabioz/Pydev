/*
 * Created on Apr 13, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.List;

import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.CompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.ui.ImageCache;


/**
 * @author Fabio Zadrozny
 */
public class AssistCreateMethodInClass extends AbstractAssistCreate {

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
            
            Definition[] defs = module.findDefinition(actTok, line, col, nature.getAstManager());
            if(defs.length >  0){
                actTok = defs[0].value;
            }else{
                return;
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        IToken[] globalTokens = module.getGlobalTokens();
        
        for (int k = 0; k < globalTokens.length; k++) {
            if(globalTokens[k].getRepresentation().endsWith(actTok)){
                AbstractModule m = nature.getAstManager().getModule(globalTokens[k].getCompletePath(), nature);
                if(!(m instanceof SourceModule)){
                    continue;
                }
                final SourceModule s = (SourceModule) m;
                final File modFile = s.getFile();
                if(modFile.exists() && modFile.isFile()){
                    l.add(getProposal(ps, imageCache, offset, m, s));
                }
            }
        }
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.AbstractAssistCreate#getProposal(org.python.pydev.editor.actions.PySelection, org.python.pydev.ui.ImageCache, int, org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule, org.python.pydev.editor.codecompletion.revisited.modules.SourceModule)
     */
    protected CompletionProposal getProposal(PySelection ps, ImageCache imageCache, int offset, AbstractModule currentModule, SourceModule definedModule) {
        return null;
    }

}
