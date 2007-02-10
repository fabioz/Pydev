/*
 * @author: fabioz
 * Created: January 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.core.runtime.Preferences;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.commentblocks.CommentBlocksPreferences;
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
    public int perform(PySelection ps) {
        // What we'll be replacing the selected text with
        StringBuffer strbuf = new StringBuffer();

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        try {
            String fullCommentLine;
            String endLineDelim = ps.getEndLineDelim();
            
            int startLineIndex = ps.getStartLineIndex();
            int endLineIndex = ps.getEndLineIndex();
            
            boolean classBehaviour = false;
            if(startLineIndex == endLineIndex && getUseClassNameBehaviour()){
                if(ps.isInClassLine()){
                    //just get the class name
                    classBehaviour = true;
                }
            }
            
            // Start of block

            if(classBehaviour){
                String line = ps.getLine(startLineIndex);
                int classIndex = line.indexOf("class ");
                fullCommentLine = getFullCommentLine(classIndex);
                String spacesBefore;
                if(classIndex > 0){
                    spacesBefore = line.substring(0, classIndex);
                }else{
                    spacesBefore = "";
                }
                
                strbuf.append(spacesBefore+"#" + fullCommentLine + endLineDelim);
                String initialLine = line;
                line = line.substring(classIndex+6);
                StringBuffer className = new StringBuffer();
                for(int i=0;i<line.length();i++){
                    char cN = line.charAt(i);
                    if(Character.isJavaIdentifierPart(cN)){
                        className.append(cN);
                    }else{
                        break;
                    }
                }
                
                strbuf.append(spacesBefore);
                strbuf.append("# ");
                strbuf.append(className);
                strbuf.append(endLineDelim);
                
                strbuf.append(spacesBefore);
                strbuf.append("#" + fullCommentLine);
                strbuf.append(endLineDelim);
                strbuf.append(initialLine);
                
                
            }else{
                fullCommentLine = getFullCommentLine(0);
                strbuf.append("#" + fullCommentLine + endLineDelim);
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

    @Override
    protected String getPreferencesNameForChar() {
        return CommentBlocksPreferences.MULTI_BLOCK_COMMENT_CHAR;
    }
    
    /**
     * Currently returns a string with the comment block.  
     * 
     * @return Comment line string, or a default one if Preferences are null
     */
    protected String getFullCommentLine(int subtract) {
        Tuple<Integer,Character> colsAndChar = getColsAndChar();
        int cols = colsAndChar.o1-subtract;
        char c = colsAndChar.o2;

        StringBuffer buffer = new StringBuffer(cols);
        for (int i = 0; i < cols-1; i++) {
            buffer.append(c);
        }
        return buffer.toString();
    }
}
