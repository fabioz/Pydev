/*
 * @author: ptoofani
 * Created: June 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import java.util.HashSet;

import org.eclipse.jface.action.IAction;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.FastStringBuffer;

/**
 * Removes a comment block. Comment blocks are slightly different than regular
 * comments created in that they provide a distinguishing element at the
 * beginning and end as a separator. In this case, it is a string of
 * <code>=======</code> symbols to strongly differentiate this comment block.
 * 
 * This will handle regular comment blocks as well by removing the # token at
 * the head of each line, but will also remove the block separators if they are
 * present.
 * 
 * Changes in 1.2.7: if any line of a block comment is selected, all 'adjacent' lines
 * will have its comment removed (without the need to select the whole lines
 * 
 * @author Parhaum Toofanian
 * @author Fabio Zadrozny
 */
public class PyRemoveBlockComment extends PyAddBlockComment {

    /**
     * Grabs the selection information and performs the action.
     */
    public void run(IAction action) {
        try {
            // Select from text editor
            PySelection ps = new PySelection(getTextEditor());

            // Put cursor at the first area of the selection
            getTextEditor().selectAndReveal(perform(ps), 0);
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
    public int perform(PySelection ps) {
        // What we'll be replacing the selected text with
        FastStringBuffer strbuf = new FastStringBuffer();


        try {
            //discover 1st line that starts the block comment
            int i;
            int startLineIndex = getStartIndex(ps);
            int endLineIndex = getEndIndex(ps);
            if(startLineIndex == -1 || endLineIndex == -1){
                if(startLineIndex == -1 && endLineIndex == -1){
                    return -1;
                }else if(startLineIndex == -1){
                    startLineIndex = endLineIndex;
                }else{
                    endLineIndex = startLineIndex;
                }
            }

            // For each line, uncomment it
            for (i = startLineIndex; i <= endLineIndex; i++) {
                boolean addDelim = true;
                String spacesBefore = "";
                String line = ps.getLine(i);
                for(int j=0;j<line.length();j++){
                    char c = line.charAt(j);
                    if(c != ' '){
                        if(c=='#'){
                            //ok, it starts with # (so, remove the whitespaces before it)
                            if(j > 0){
                                spacesBefore = line.substring(0, j);
                            }
                            line = line.substring(j);
                            break;
                        }
                    }
                }
                if(line.startsWith("#")){
                    line = line.substring(1);
                }
                
                //get the chars used in block-comments
                AbstractBlockCommentAction[] acts = new AbstractBlockCommentAction[]{new PyAddSingleBlockComment(),new PyAddBlockComment()};
                HashSet<Character> chars = new HashSet<Character>();
                for (int j = 0; j < acts.length; j++) {
                    AbstractBlockCommentAction action = acts[j];
                    chars.add(action.getColsAndChar().o2);
                }
                
                if(line.length() > 0){
                    boolean removedChar=false;
                    char lastChar='\0';
                    for(int j=0;j<line.length();j++){
                        lastChar = line.charAt(j);
                        if(!chars.contains(lastChar)){
                            break;
                        }else{
                            removedChar=true;
                            line = line.substring(1);
                            j--;
                        }
                    }
                    if(line.length() == 0 && removedChar){
                        addDelim = false;
                    }
                    if(removedChar && lastChar==' '){
                        line = line.substring(1);
                    }
                }
                
                if(addDelim){
                    strbuf.append(spacesBefore);
                    strbuf.append(line);
                    String lineDelimiter = ps.getDoc().getLineDelimiter(i);
                    if(lineDelimiter != null){
                        strbuf.append(lineDelimiter);
                    }
                }
            }

            // Replace the text with the modified information
            int startLineOffset = ps.getLineOffset(startLineIndex);
            int endLineOffset = ps.getEndLineOffset(endLineIndex);
            String endLineDelimiter = ps.getDoc().getLineDelimiter(endLineIndex);
            if(endLineDelimiter != null){
                endLineOffset += endLineDelimiter.length(); 
            }
            String str = strbuf.toString();
            ps.getDoc().replace(startLineOffset, endLineOffset-startLineOffset, str);
            return startLineOffset+str.length();
        } catch (Exception e) {
            beep(e);
        }

        // In event of problems, return false
        return -1;
    }

    private int getEndIndex(PySelection ps) {
        int endLineIndex = -1;
        int i = ps.getEndLineIndex();
        while(true){
            String line = ps.getLine(i);
            if(PySelection.isCommentLine(line)){
                endLineIndex=i;
            }else{
                break;
            }
            i++;
        }
        return endLineIndex;
    }

    private int getStartIndex(PySelection ps) {
        int startLineIndex = -1;
        int i = ps.getStartLineIndex();
        while(true){
            String line = ps.getLine(i);
            if(PySelection.isCommentLine(line)){
                startLineIndex=i;
            }else{
                break;
            }
            i--;
        }
        return startLineIndex;
    }

}
