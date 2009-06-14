package org.python.pydev.editor.actions;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.DocUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.autoedit.IIndentPrefs;

/**
 * This action was created so that we can make the shift left even if there are less characters in the line than
 * the expected indent (the default shift left won't do the dedent in that case).
 */
public class PyShiftLeft extends PyAction{

    /**
     * Grabs the selection information and performs the action.
     */
    public void run(IAction action) {
        try {
            PyEdit pyEdit = (PyEdit) getTextEditor();
            IIndentPrefs indentPrefs = pyEdit.getIndentPrefs();
            PySelection ps = new PySelection(pyEdit);
            Tuple<Integer, Integer> repRegion = perform(ps, indentPrefs);

            pyEdit.selectAndReveal(repRegion.o1, repRegion.o2);
        } catch (Exception e) {
            beep(e);
        }
    }
    

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps Given PySelection
     * @param indentPrefs 
     * @return the new selection
     * @throws BadLocationException 
     */
    public Tuple<Integer, Integer> perform(PySelection ps, IIndentPrefs indentPrefs) throws BadLocationException {
        int endLineIndex = ps.getEndLineIndex();
        
        //Get the start and len before we change to select the full contents.
        int initialStart = ps.getAbsoluteCursorOffset();
        int initialLen = ps.getSelLength();
        int initialOffsetAtLastLine = (initialStart + initialLen) - ps.getEndLine().getOffset();
        
        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();
        int newStart = ps.getAbsoluteCursorOffset();

        String selectedText = ps.getSelectedText();
        List<String> ret = StringUtils.splitInLines(selectedText);

        
        int tabWidth = indentPrefs.getTabWidth();
        int tabWidthToUse = tabWidth;
        
        //Calculate the tab width we should use
        for(String line: ret){
            String lineIndent = PySelection.getIndentationFromLine(line);
            
            if(lineIndent.length() > 0){
                if(lineIndent.startsWith("\t")){
                    //Tab will be treated by removing the whole tab, so, just go on
                }else{
                    //String with spaces... let's see if we have less spaces than we have the tab width
                    int spaces = 0;
                    for(int i=0;i<lineIndent.length();i++){
                        char c = lineIndent.charAt(i);
                        if(c == ' '){
                            spaces += 1;
                        }else{
                            break; //ok, found all spaces available
                        }
                    }
                    if(spaces > 0){
                        tabWidthToUse = Math.min(spaces, tabWidthToUse);
                    }
                }
            }
        }
        
        
        //Actually do the replacement
        String defaultIndentStr = DocUtils.createSpaceString(tabWidthToUse);
        FastStringBuffer strbuf = new FastStringBuffer(selectedText.length()+ret.size()+2);
        
        //We must also get the number of chars removed at the 1st and last line so that we can calculate the
        //new selection.
        int removedAtFirst = 0;
        int removedAtLast = 0;
        
        for(int i=0;i<ret.size();i++){
            String line = ret.get(i);
            if(line.startsWith("\t")){
                if(i == 0){
                    removedAtFirst = 1;
                }
                if(i == ret.size()-1){
                    removedAtLast = 1;
                }
                line = line.substring(1);
                
            }else if(line.startsWith(defaultIndentStr)){
                if(i == 0){
                    removedAtFirst = defaultIndentStr.length();
                }
                if(i == ret.size()-1){
                    removedAtLast = defaultIndentStr.length();
                }
                line = line.substring(defaultIndentStr.length());
            }
            strbuf.append(line);
        }
        
        ITextSelection txtSel = ps.getTextSelection();
        int start = txtSel.getOffset();
        int len = txtSel.getLength();
        
        String replacement = strbuf.toString();
        // Replace the text with the modified information
        IDocument doc = ps.getDoc();
        doc.replace(start, len, replacement);
        
        
        //Calculate the new cursor position.
        int startSel = initialStart - removedAtFirst;
        if(startSel < newStart){
            startSel = newStart;
        }
        int endSel = initialOffsetAtLastLine - removedAtLast;
        if(endSel < 0){
            endSel = initialOffsetAtLastLine;
        }
        int endLen = doc.getLineOffset(endLineIndex)+endSel - startSel;
        return new Tuple<Integer, Integer>(startSel, endLen);
    }
}
