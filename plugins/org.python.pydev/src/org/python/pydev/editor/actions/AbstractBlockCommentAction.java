package org.python.pydev.editor.actions;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;

public abstract class AbstractBlockCommentAction extends PyAction {
    
    protected boolean alignRight=true;
    protected int defaultCols=80;

    public AbstractBlockCommentAction(){
        //default
    }
    
    /**
     * For tests: assigns the default values
     */
    protected AbstractBlockCommentAction(int defaultCols, boolean alignLeft){
        this.defaultCols = defaultCols;
        this.alignRight = alignLeft;
    }
    
    
    /**
     * Grabs the selection information and performs the action.
     */
    public void run(IAction action) {
        try {
            // Select from text editor
            PySelection ps = new PySelection(getTextEditor());
            // Perform the action
            int toSelect = perform(ps);
            if(toSelect != -1){
                getTextEditor().selectAndReveal(toSelect, 0);
            }else{
                // Put cursor at the first area of the selection
                revealSelEndLine(ps);
            }
        } catch (Exception e) {
            beep(e);
        }
    }

    /**
     * Actually performs the action 
     */
    public abstract int perform(PySelection ps);
    
    
    /**
     * @return the number of columns to be used (and the char too)
     */
    public Tuple<Integer, Character> getColsAndChar(){
        int cols = this.defaultCols;
        char c = '-';
        
        try{
            IPreferenceStore chainedPrefStore = PydevPrefs.getChainedPrefStore();
            cols = chainedPrefStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
            Preferences prefs = PydevPlugin.getDefault().getPluginPreferences();
            c = prefs.getString(getPreferencesNameForChar()).charAt(0);
        }catch(NullPointerException e){
            //ignore... we're in the tests env
        }
        return new Tuple<Integer, Character>(cols, c);
    }

    protected abstract String getPreferencesNameForChar() ;


}
