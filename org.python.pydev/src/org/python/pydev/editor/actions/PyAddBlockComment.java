/*
 * @author: fabioz
 * Created: January 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.core.runtime.Preferences;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.commentblocks.CommentBlocksPreferences;
import org.python.pydev.editor.correctionassist.docstrings.AssistDocString;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Creates a comment block.  Comment blocks are slightly different than regular comments 
 * created in that they provide a distinguishing element at the beginning and end as a 
 * separator.  In this case, it is a string of <code>=======</code> symbols to strongly
 * differentiate this comment block.
 * 
 * @author Fabio Zadrozny
 * @author Parhaum Toofanian
 */
public class PyAddBlockComment extends AbstractBlockCommentAction {


    private boolean defaultClassNameBehaviour;

    public PyAddBlockComment(){
        //default
    }
    
    /**
     * For tests: assigns the default values
     */
    PyAddBlockComment(int defaultCols, boolean alignLeft, boolean classNameBehaviour){
        super(defaultCols, alignLeft);
        this.defaultClassNameBehaviour = classNameBehaviour;
    }

    @Override
    protected void revealSelEndLine(PySelection ps) {
        getTextEditor().selectAndReveal(ps.getEndLine().getOffset(), 0);
    }


    protected boolean getUseClassNameBehaviour(){
        try{
            Preferences prefs = PydevPlugin.getDefault().getPluginPreferences();
            return prefs.getBoolean(CommentBlocksPreferences.MULTI_BLOCK_COMMENT_SHOW_ONLY_CLASS_NAME);
        }catch(NullPointerException e){ //tests
            return defaultClassNameBehaviour;
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

        try {
            String fullCommentLine = getFullCommentLine();
            String endLineDelim = ps.getEndLineDelim();
            
            int startLineIndex = ps.getStartLineIndex();
            int endLineIndex = ps.getEndLineIndex();
            
            boolean classBehaviour = false;
            if(startLineIndex == endLineIndex && getUseClassNameBehaviour()){
                String line = ps.getLine(startLineIndex);
                if(AssistDocString.ClassPattern.matcher(line).matches()){
                    //just get the class name
                    classBehaviour = true;
                }
            }
            
            // Start of block
            strbuf.append("#" + fullCommentLine + endLineDelim);

            if(classBehaviour){
                String line = ps.getLine(startLineIndex);
                String initialLine = line;
                line = line.substring(line.indexOf("class ")+6);
                StringBuffer className = new StringBuffer();
                for(int i=0;i<line.length();i++){
                    char cN = line.charAt(i);
                    if(Character.isJavaIdentifierPart(cN)){
                        className.append(cN);
                    }else{
                        break;
                    }
                }
                
                strbuf.append("# ");
                strbuf.append(className);
                strbuf.append(endLineDelim);
                strbuf.append("#" + fullCommentLine);
                strbuf.append(endLineDelim);
                strbuf.append(initialLine);
                
                
            }else{
                // For each line, comment them out
                for (int i = startLineIndex; i <= endLineIndex; i++) {
                    strbuf.append("#");
                    String line = ps.getLine(i);
                    if(!line.startsWith("\t") && !line.startsWith(" ")){
                        strbuf.append(" ");
                    }
                    strbuf.append(line);
                    strbuf.append(endLineDelim);
                }
                // End of block
                strbuf.append("#" + fullCommentLine);
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

    @Override
    protected String getPreferencesNameForChar() {
        return CommentBlocksPreferences.MULTI_BLOCK_COMMENT_CHAR;
    }
    
    /**
     * Currently returns a string with the comment block.  
     * 
     * @return Comment line string, or a default one if Preferences are null
     */
    protected String getFullCommentLine() {
        Tuple<Integer,Character> colsAndChar = getColsAndChar();
        int cols = colsAndChar.o1;
        char c = colsAndChar.o2;

        StringBuffer buffer = new StringBuffer(cols);
        for (int i = 0; i < cols-1; i++) {
            buffer.append(c);
        }
        return buffer.toString();
    }
}
