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
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;

public class PyAddSingleBlockComment extends PyAction{
    /* Selection element */
    private PySelection ps;

    public void run(IAction action) {
        try {
            // Select from text editor
            ps = new PySelection(getTextEditor());
            // Perform the action
            perform(ps);

            // Put cursor at the first area of the selection
            getTextEditor().selectAndReveal(ps.getEndLine().getOffset(), 0);
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
                String line = ps.getLine(i).trim();
                String fullCommentLine = getFullCommentLine(line); 
                strbuf.append(fullCommentLine);
                strbuf.append(line );
                strbuf.append(ps.getEndLineDelim());
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
     * @param line 
     * 
     * @return Comment line string, or a default one if Preferences are null
     */
    protected static String getFullCommentLine(String line) {
        try {
            IPreferenceStore chainedPrefStore = PydevPlugin.getChainedPrefStore();
            int cols = chainedPrefStore
                    .getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
            StringBuffer buffer = new StringBuffer(cols);
            Preferences prefs = PydevPlugin.getDefault().getPluginPreferences();
            char c;
            try {
                c = prefs.getString(PydevPrefs.SINGLE_BLOCK_COMMENT_CHAR).charAt(0);
            } catch (Exception e) {
                c = '-';
            }

            buffer.append("#");
            for (int i = 0; i + line.length() < cols-2; i++) {
                buffer.append(c);
            }
            buffer.append(" ");
            return buffer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
