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
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class AssistAssign implements IAssistProps {

    private Image getImage(ImageCache imageCache, String c){
        if(imageCache != null)
            return imageCache.get(c);
        return null;
    }
    
    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.core.docutils.PySelection, org.python.pydev.core.bundle.ImageCache, java.io.File, org.python.pydev.plugin.PythonNature)
     */
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, PyEdit edit, int offset) throws BadLocationException {
        List<ICompletionProposal> l = new ArrayList<ICompletionProposal>();
        String sel = PyAction.getLineWithoutComments(ps);
        if (sel.trim().length() == 0) {
            return l;
        }


        //go on and make the suggestion.
        //
        //if we have a method call, eg.:
        //  e.methodCall()| would result in the following suggestions:
        //
        //                   methodCall = e.methodCall()
        //					 self.methodCall = e.methodCall()
        //
        // NewClass()| would result in
        //
        //                   newClass = NewClass()
        //					 self.newClass = NewClass()
        //
        //now, if we don't have a method call, eg.:
        // 1+1| would result in
        //
        //					 |result| = 1+1
        //					 self.|result| = 1+1

        String callName = getTokToAssign(ps);

        if(callName.length() > 0){
            //all that just to change first char to lower case.
            callName = PyAction.lowerChar(callName, 0);
            if (callName.startsWith("get") && callName.length() > 3){
                callName = callName.substring(3);
                callName = PyAction.lowerChar(callName, 0);
            }
        }else{
            callName = "result";
        }
        
        String tok = callName;

        int firstCharPosition = PySelection.getFirstCharPosition(ps.getDoc(), ps.getAbsoluteCursorOffset());
        callName += " = ";
        l.add(new PyCompletionProposal(callName, firstCharPosition, 0, 0, getImage(imageCache, UIConstants.ASSIST_ASSIGN_TO_LOCAL),
                "Assign to local ("+tok+")", null, null, IPyCompletionProposal.PRIORITY_DEFAULT));
        
        l.add(new PyCompletionProposal("self." + callName, firstCharPosition, 0, 5, getImage(imageCache,UIConstants.ASSIST_ASSIGN_TO_CLASS),
                "Assign to field (self."+tok+")", null, null, IPyCompletionProposal.PRIORITY_DEFAULT));
        return l;
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.core.docutils.PySelection, java.lang.String)
     */
    public boolean isValid(PySelection ps, String sel, PyEdit edit, int offset) {
        try {
            if(! (ps.getTextSelection().getLength() == 0))
                return false;

            String lineToCursor = ps.getLineContentsToCursor();

            if( ! ( sel.indexOf("class ") == -1 && sel.indexOf("def ") == -1 && sel.indexOf("import ") == -1))
                return false;

            String eqReplaced = sel.replaceAll("==", "");
            if (eqReplaced.indexOf("=") != -1){
                //ok, make analysis taking into account the first parentesis
                if(eqReplaced.indexOf('(') == -1){
                    return false;
                }
                int i = eqReplaced.indexOf('(');
                if(eqReplaced.substring(0, i).indexOf('=') != -1){
                    return false;
                }
            }
            
            if( lineToCursor.trim().endsWith(")"))
                return true;
            
            if( lineToCursor.indexOf('.') != -1)
                return true;
            
            
        } catch (BadLocationException e) {
        }
        return false;
    }

    /**
     * @param ps
     * @return
     */ 
    private String getTokToAssign(PySelection ps) {
        String beforeParentesisTok = PyAction.getBeforeParentesisTok(ps);
        if(beforeParentesisTok.length() > 0){
            return beforeParentesisTok;
        }
        //otherwise, try to find . (ignore code after #)
        String string = PyAction.getLineWithoutComments(ps);
        String callName = "";
        //get parentesis position and go backwards
    
        int i;
        if ((i = string.lastIndexOf(".")) != -1) {
            callName = "";
    
            for (int j = i+1; j < string.length() && PyAction.stillInTok(string, j); j++) {
                callName += string.charAt(j);
            }
        }
        return callName;
    }

}
