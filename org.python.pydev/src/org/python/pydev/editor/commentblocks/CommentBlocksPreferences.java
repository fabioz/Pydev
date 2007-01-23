package org.python.pydev.editor.commentblocks;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;

public class CommentBlocksPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {


    public CommentBlocksPreferences() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Comment Block Preferences");
    }

    public static final String MULTI_BLOCK_COMMENT_CHAR = "MULTI_BLOCK_COMMENT_CHAR";
    public static final String DEFAULT_MULTI_BLOCK_COMMENT_CHAR = "=";
    
    public static final String SINGLE_BLOCK_COMMENT_CHAR = "SINGLE_BLOCK_COMMENT_CHAR";
    public static final String DEFAULT_SINGLE_BLOCK_COMMENT_CHAR = "-";
    
    public static final String SINGLE_BLOCK_COMMENT_ALIGN_RIGHT = "SINGLE_BLOCK_COMMENT_ALIGN_RIGHT";
    public static final boolean DEFAULT_SINGLE_BLOCK_COMMENT_ALIGN_RIGHT = true;
    
    
    @Override
    protected void createFieldEditors() {
        final Composite p = getFieldEditorParent();
        addField(new StringFieldEditor(MULTI_BLOCK_COMMENT_CHAR, "Multi-block char (ctrl+4):", 2, p));
        addField(new StringFieldEditor(SINGLE_BLOCK_COMMENT_CHAR, "Single-block char (ctrl+shift+4):", 2, p));
        addField(new BooleanFieldEditor(SINGLE_BLOCK_COMMENT_ALIGN_RIGHT, "Align text in single-block to the right?", p));
    
    }
    public void init(IWorkbench workbench) {
        // pass
        
    }
    
    

}
