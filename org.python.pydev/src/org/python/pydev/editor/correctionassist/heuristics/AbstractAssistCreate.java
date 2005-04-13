/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.CompletionProposal;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.ui.ImageCache;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractAssistCreate implements IAssistProps{



    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.editor.actions.PySelection, org.python.pydev.ui.ImageCache, java.io.File, org.python.pydev.plugin.PythonNature, org.python.pydev.editor.model.AbstractNode)
     */
    public List getProps(PySelection ps, ImageCache imageCache, File file, PythonNature nature, AbstractNode root) throws BadLocationException {
        List l = new ArrayList();
        
        //ok, check line to see if it maps to some import
        String lineContentsToCursor = ps.getLineContentsToCursor();
        int i = lineContentsToCursor.indexOf('.')+1;
        int offset = ps.getStartLine().getOffset()+i;
        
        String[] strs = PyCodeCompletion.getActivationTokenAndQual(ps.getDoc(), offset);
        
        String actTok = strs[0];
        if(actTok.endsWith(".")){
            actTok = actTok.substring(0, actTok.length()-1);
        }
        
        //now, check if the actTok is part of some import...
                
        SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc("", file, ps.getDoc(), nature, ps.getEndLineIndex());
        
        getProposalFromModule(ps, imageCache, nature, l, offset, actTok, module);
        
        
        return l;
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
        IToken[] tokenImportedModules = module.getTokenImportedModules();
        for (int k = 0; k < tokenImportedModules.length; k++) {
            if(tokenImportedModules[k].getRepresentation().endsWith(actTok)){
                AbstractModule m = nature.getAstManager().getModule(tokenImportedModules[k].getCompletePath(), nature);
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
     * @param offset that's the offset where the '.' was found
     * @return
     */
    protected abstract CompletionProposal getProposal(PySelection ps, ImageCache imageCache, int offset, AbstractModule currentModule, final SourceModule definedModule);


    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.editor.actions.PySelection, java.lang.String)
     */
    public boolean isValid(PySelection ps, String sel) {
        try {
            String lineToCursor = ps.getLineContentsToCursor();
	        return lineToCursor.indexOf('.') != -1 && lineToCursor.indexOf('(') != -1 &&
	        lineToCursor.indexOf(')') != -1;
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return false;
    }


}
