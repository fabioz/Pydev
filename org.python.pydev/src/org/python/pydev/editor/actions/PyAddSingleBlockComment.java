/*
 * License: Common Public License v1.0
 * Created on 01/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.commentblocks.CommentBlocksPreferences;
import org.python.pydev.plugin.PydevPlugin;

public class PyAddSingleBlockComment extends PyAction {
    private boolean alignRight=true;
    private int defaultCols=80;

    public PyAddSingleBlockComment(){
        //default
    }
    
    /**
     * For tests: assigns the default values
     */
    PyAddSingleBlockComment(int defaultCols, boolean alignLeft){
        this.defaultCols = defaultCols;
        this.alignRight = alignLeft;
        
    }

    public void run(IAction action) {
        try {
            PySelection ps = new PySelection(getTextEditor());
            // Perform the action
            perform(ps);

            // Put cursor at the first area of the selection
            revealSelEndLine(ps);
        } catch (Exception e) {
            beep(e);
        }
    }

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps Given PySelection
     * @return boolean The success or failure of the action
     */
    public boolean perform(PySelection ps) {
        // What we'll be replacing the selected text with
        StringBuffer strbuf = new StringBuffer();

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        int i;
        try {
            // For each line, comment them out
            for (i = ps.getStartLineIndex(); i <= ps.getEndLineIndex(); i++) {
                String line = StringUtils.rightTrim(ps.getLine(i));
                if(getAlignRight()){
                    strbuf.append(getRightAlignedFullCommentLine(line));
                    strbuf.append(line.trim());
                    if(i != ps.getEndLineIndex()){
                        strbuf.append(ps.getEndLineDelim());
                    }
                }else{
                    Tuple<Integer,Character> colsAndChar = getColsAndChar();
                    int cols = colsAndChar.o1;
                    char c = colsAndChar.o2;
                    
                    StringBuffer buffer = makeBufferToIndent(line, cols);            
                    buffer.append('#');
                    buffer.append(line.trim());
                    buffer.append(' ');
                    while(buffer.length() < cols){
                        buffer.append(c);
                    }
                    strbuf.append(buffer);
                    if(i != ps.getEndLineIndex()){
                        strbuf.append(ps.getEndLineDelim());
                    }
                }
            }

            // Replace the text with the modified information
            ps.getDoc().replace(ps.getStartLine().getOffset(), ps.getSelLength(), strbuf.toString());
            return true;
        } catch (Exception e) {
            beep(e);
        }

        // In event of problems, return false
        return false;
    }

    private boolean getAlignRight() {
        try{
            return PydevPlugin.getDefault().getPluginPreferences().getBoolean(CommentBlocksPreferences.SINGLE_BLOCK_COMMENT_ALIGN_RIGHT);
        }catch(NullPointerException e){
            //ignore... we're in the tests env
        }
        return this.alignRight;
    }
    
    protected Tuple<Integer, Character> getColsAndChar(){
        int cols = this.defaultCols;
        char c = '-';
        
        try{
            IPreferenceStore chainedPrefStore = PydevPlugin.getChainedPrefStore();
            cols = chainedPrefStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
            Preferences prefs = PydevPlugin.getDefault().getPluginPreferences();
            c = prefs.getString(CommentBlocksPreferences.SINGLE_BLOCK_COMMENT_CHAR).charAt(0);
        }catch(NullPointerException e){
            //ignore... we're in the tests env
        }
        return new Tuple<Integer, Character>(cols, c);
    }

    /**
     * Currently returns a string with the comment block.
     * 
     * @param line
     * 
     * @return Comment line string, or a default one if Preferences are null
     */
    protected String getRightAlignedFullCommentLine(String line) {
        Tuple<Integer,Character> colsAndChar = getColsAndChar();
        int cols = colsAndChar.o1;
        char c = colsAndChar.o2;
        
        StringBuffer buffer = makeBufferToIndent(line, cols);            
        
        buffer.append("#");
        for (int i = 0; i + line.length() < cols - 2; i++) {
            buffer.append(c);
        }
        buffer.append(" ");
        return buffer.toString();
    }

    private StringBuffer makeBufferToIndent(String line, int cols) {
        StringBuffer buffer = new StringBuffer(cols);
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if(ch == '\t' || ch == ' '){
                buffer.append(ch);
            }else{
                break;
            }
        }
        return buffer;
    }

}
