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
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.codingstd.ICodingStd;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.plugin.preferences.PyCodeStylePreferencesPage;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class AssistAssign implements IAssistProps {

    private ICodingStd std;

    public AssistAssign() {
        this(new ICodingStd(){

            public boolean localsAndAttrsCamelcase() {
                return PyCodeStylePreferencesPage.useLocalsAndAttrsCamelCase();
            }
            
        });
    }
    
    public AssistAssign(ICodingStd std) {
        this.std = std;
    }

    private Image getImage(ImageCache imageCache, String c){
        if(imageCache != null)
            return imageCache.get(c);
        return null;
    }
    
    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps
     */
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature, 
            PyEdit edit, int offset) throws BadLocationException {
        PySourceViewer viewer = null;
        if(edit != null){ //only in tests it's actually null
            viewer = edit.getPySourceViewer();
        }
        
        return this.getProps(ps, imageCache, viewer, offset, PyAction.getLineWithoutComments(ps), 
                PySelection.getFirstCharPosition(ps.getDoc(), ps.getAbsoluteCursorOffset()));
    }        
    
    /**
     * Actual implementation (receiving a source viewer and only the actually used parameters).
     * 
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps
     * 
     * @param lineWithoutComments the line that should be checked (without any comments)
     */
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, 
            ISourceViewer sourceViewer, int offset, String lineWithoutComments, int firstCharAbsolutePosition) 
            throws BadLocationException {
            
        List<ICompletionProposal> l = new ArrayList<ICompletionProposal>();
        if (lineWithoutComments.trim().length() == 0) {
            return l;
        }


        //go on and make the suggestion.
        //
        //if we have a method call, eg.:
        //  e.methodCall()| would result in the following suggestions:
        //
        //                   methodCall = e.methodCall()
        //                     self.methodCall = e.methodCall()
        //
        // NewClass()| would result in
        //
        //                   newClass = NewClass()
        //                     self.newClass = NewClass()
        //
        //now, if we don't have a method call, eg.:
        // 1+1| would result in
        //
        //                     |result| = 1+1
        //                     self.|result| = 1+1

        String callName = getTokToAssign(ps, lineWithoutComments);
        callName = changeToLowerUppercaseConstant(callName);

        if(callName.length() > 0){
            //all that just to change first char to lower case.
            if (callName.toLowerCase().startsWith("get") && callName.length() > 3){
                callName = callName.substring(3);
            }
            
            callName = changeToCodingStd(callName);
            
            for(int i=0;i<callName.length();i++){
                char c = callName.charAt(i);
                if(c != '_'){
                    callName = PyAction.lowerChar(callName, i);
                    break;
                }
            }
        }else{
            callName = "result";
        }
        
        String tok = callName;

        callName += " = ";
        l.add(new AssistAssignCompletionProposal(callName, firstCharAbsolutePosition, 0, 0, getImage(imageCache, UIConstants.ASSIST_ASSIGN_TO_LOCAL),
                "Assign to local ("+tok+")", null, null, IPyCompletionProposal.PRIORITY_DEFAULT, sourceViewer));
        
        l.add(new AssistAssignCompletionProposal("self." + callName, firstCharAbsolutePosition, 0, 5, getImage(imageCache, UIConstants.ASSIST_ASSIGN_TO_CLASS),
                "Assign to field (self."+tok+")", null, null, IPyCompletionProposal.PRIORITY_DEFAULT, sourceViewer));
        return l;
    }

    private String changeToLowerUppercaseConstant(String callName) {
        char[] cs = callName.toCharArray();
        for(char c: cs){
            if(Character.isLetterOrDigit(c) && !Character.isUpperCase(c)){
                return callName;
            }
        }
        return callName.toLowerCase();
    }

    private String changeToCodingStd(String callName) {
        if(!this.std.localsAndAttrsCamelcase()){
            FastStringBuffer ret = new FastStringBuffer();
            char[] cs = callName.toCharArray();
            
            boolean foundLower = false;
            
            for(char c: cs){
                if(Character.isUpperCase(c)){
                    if(foundLower){
                        ret.append('_');
                    }
                    foundLower = false;
                    ret.append(Character.toLowerCase(c));
                }else{
                    if(Character.isLowerCase(c)){
                        foundLower = true;
                    }
                    ret.append(c);
                }
            }
            return ret.toString();
        }else{
            return callName;
        }
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid
     */
    public boolean isValid(PySelection ps, String sel, PyEdit edit, int offset) {
        return isValid(ps.getTextSelection().getLength(), sel, offset);
    }
    
    /**
     * @param selectionLength the length of the currently selected text 
     * @param lineContents the contents of the line
     * @param offset the offset of the cursor
     * @return true if an assign is available and false otherwise
     */
    public boolean isValid(int selectionLength, String lineContents, int offset) {
        if(! (selectionLength == 0)){
            return false;
        }

        if( ! ( lineContents.indexOf("class ") == -1 &&
                lineContents.indexOf("def ") == -1 && 
                lineContents.indexOf("import ") == -1)){
            
            return false;
        }

        String eqReplaced = lineContents.replaceAll("==", "");
        if (eqReplaced.indexOf("=") != -1){ //we have some equal
            //ok, make analysis taking into account the first parentesis
            if(eqReplaced.indexOf('(') == -1){
                return false;
            }
            int i = eqReplaced.indexOf('(');
            if(eqReplaced.substring(0, i).indexOf('=') != -1){
                return false;
            }
        }
        return true;
    }

    /**
     * @param ps
     * @return
     */ 
    private String getTokToAssign(PySelection ps, String sel) {
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
        if(callName.length() == 0){
            if(StringUtils.isSingleWord(sel.trim())){
                return sel.trim();
            }
        }
        return callName;
    }

}
