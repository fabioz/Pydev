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
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;

public class PyAddSingleBlockComment extends PyAction {
    /* Selection element */

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
    public static boolean perform(PySelection ps) {
        // What we'll be replacing the selected text with
        StringBuffer strbuf = new StringBuffer();

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        int i;
        try {
            // For each line, comment them out
            for (i = ps.getStartLineIndex(); i <= ps.getEndLineIndex(); i++) {
                String line = StringUtils.rightTrim(ps.getLine(i));
                String fullCommentLine = getFullCommentLine(line);
                strbuf.append(fullCommentLine);
                strbuf.append(line.trim());
                if(i != ps.getEndLineIndex()){
                    strbuf.append(ps.getEndLineDelim());
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

    /**
     * Currently returns a string with the comment block.
     * 
     * @param line
     * 
     * @return Comment line string, or a default one if Preferences are null
     */
    protected static String getFullCommentLine(String line) {
        try {
            IPreferenceStore chainedPrefStore = null; 
            int cols = 10;
            char c = '-';
            
            try{
                chainedPrefStore = PydevPlugin.getChainedPrefStore();
                cols = chainedPrefStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
                Preferences prefs = PydevPlugin.getDefault().getPluginPreferences();
                c = prefs.getString(PydevPrefs.SINGLE_BLOCK_COMMENT_CHAR).charAt(0);
            }catch(NullPointerException e){
                //ignore... we're in the tests env
            }
            StringBuffer buffer = new StringBuffer(cols);
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if(ch == '\t' || ch == ' '){
                    buffer.append(ch);
                }else{
                    break;
                }
            }            
            
            
            buffer.append("#");
            for (int i = 0; i + line.length() < cols - 2; i++) {
                buffer.append(c);
            }
            buffer.append(" ");
            return buffer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
