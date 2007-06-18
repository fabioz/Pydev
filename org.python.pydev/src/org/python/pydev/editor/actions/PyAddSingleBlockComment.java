/*
 * License: Common Public License v1.0
 * Created on 01/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.commentblocks.CommentBlocksPreferences;
import org.python.pydev.plugin.PydevPlugin;

public class PyAddSingleBlockComment extends AbstractBlockCommentAction {
    
    public PyAddSingleBlockComment(){
        //default
    }
    
    /**
     * For tests: assigns the default values
     */
    PyAddSingleBlockComment(int defaultCols, boolean alignLeft){
        super(defaultCols, alignLeft);
    }

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps Given PySelection
     * @return boolean The success or failure of the action
     */
    public int perform(PySelection ps) {
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
                    buffer.append("# ");
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

            int startOffset = ps.getStartLine().getOffset();
            String str = strbuf.toString();
            // Replace the text with the modified information
            ps.getDoc().replace(startOffset, ps.getSelLength(), str);
            return startOffset+str.length();
        } catch (Exception e) {
            beep(e);
        }

        // In event of problems, return false
        return -1;
    }

    private boolean getAlignRight() {
        PydevPlugin plugin = PydevPlugin.getDefault();
        if(plugin != null){
            return plugin.getPluginPreferences().getBoolean(CommentBlocksPreferences.SINGLE_BLOCK_COMMENT_ALIGN_RIGHT);
            
        }else{ //tests env
            return this.alignRight;
        }
    }

    @Override
    protected String getPreferencesNameForChar() {
        return CommentBlocksPreferences.SINGLE_BLOCK_COMMENT_CHAR;
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
