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
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
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
        
        String lineContentsToCursor = ps.getCursorLineContents();
        //ok, check line to see if it maps to some import
        String validText = getValidText(ps);
        
        if(validText.indexOf("=") != -1){
            validText = validText.split("=")[1].trim();
        }
        
        int indexOfPoint = validText.indexOf('.');
        int i = indexOfPoint+1;
        
        int offset = ps.getStartLine().getOffset();
        offset += lineContentsToCursor.indexOf(validText);
        offset += i;
        
        String actTok = getActTok(ps, offset);
        
        //now, check if the actTok is part of some import...
                
        SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc("", file, ps.getDoc(), nature, ps.getEndLineIndex());
        
        getProposalFromModule(ps, imageCache, nature, l, offset, actTok, module);
        
        
        return l;
    }

    
    /**
     * @param ps
     * @param offset
     * @return
     */
    private String getActTok(PySelection ps, int offset) {
        String[] strs = PyCodeCompletion.getActivationTokenAndQual(ps.getDoc(), offset);
        
        String actTok = strs[0];
        if(actTok.endsWith(".")){
            actTok = actTok.substring(0, actTok.length()-1);
        }
        return actTok;
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
        if(actTok.trim().length() == 0){
            l.add(getProposal(ps, imageCache, offset, module)); //same module as doc
            return;
        }
        
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
                    l.add(getProposal(ps, imageCache, offset, s));
                }
            }
        }
    }


    /**
     * @param offset that's the offset where the '.' was found
     * @return
     */
    protected abstract PyCompletionProposal getProposal(PySelection ps, ImageCache imageCache, int offset, final SourceModule definedModule);


    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.editor.actions.PySelection, java.lang.String)
     */
    public boolean isValid(PySelection ps, String sel) {
        try {
            String lineToCursor = getValidText(ps);

            if(lineToCursor.indexOf("class ") != -1 || lineToCursor.indexOf("def ") != -1)
                return false;
            
	        return lineToCursor.indexOf('(') != -1 && lineToCursor.indexOf(')') != -1;
	        
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * @param ps
     * @return
     * @throws BadLocationException
     */
    protected String getValidText(PySelection ps) throws BadLocationException {
        String lineToCursor;
        if(ps.getSelLength() == 0){
            lineToCursor = ps.getLineContentsToCursor();
            
        }else{
            lineToCursor = ps.getSelectedText();
        }
        return lineToCursor;
    }
    


}
