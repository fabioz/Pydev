package org.python.pydev.debug.ui;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.editorinput.PySourceLocatorPrefs;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;

/**
 * Preferences for the locations that should be translated -- used when the debugger is not able
 * to find some path aa the client, so, the user is asked for the location and the answer is
 * kept in the preferences in the format:
 * 
 * path asked, new path -- means that a request for the "path asked" should return the "new path"
 * path asked, DONTASK -- means that if some request for that file was asked it should silently ignore it
 */
public class SourceLocatorPrefsPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{


	/**
	 * Initializer sets the preference store
	 */
	public SourceLocatorPrefsPage() {
		super("Source locator", GRID);
		setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
	}

	public void init(IWorkbench workbench) {
	}
	
	
	/**
	 * Creates the editors
	 */
	@SuppressWarnings("unchecked")
    protected void createFieldEditors() {
		Composite p = getFieldEditorParent();
		
        addField(new ListEditor(PydevPrefs.SOURCE_LOCATION_PATHS, "Translation paths to use:", p){

            @Override
            protected String createList(String[] items) {
                return PySourceLocatorPrefs.wordsAsString(items);
            }

            @Override
            protected String getNewInputObject() {
                InputDialog d = new InputDialog(getShell(), "New entry", "Add the entry in the format 'path': 'new path' or 'path' : DONTASK.", "", new IInputValidator(){

                    public String isValid(String newText) {
                        return PySourceLocatorPrefs.isValid(newText);
                    }});

                int retCode = d.open();
                if (retCode == InputDialog.OK) {
                    return d.getValue();
                }
                return null;
            }

            @Override
            protected String[] parseString(String stringList) {
                return PySourceLocatorPrefs.stringAsWords(stringList);
            }
            
            @Override
            protected void doFillIntoGrid(Composite parent, int numColumns) {
                super.doFillIntoGrid(parent, numColumns);
                List listControl = getListControl(parent);
                GridData layoutData = (GridData) listControl.getLayoutData();
                layoutData.heightHint = 300;
            }
        });
	}

	

	/**
	 * Sets default preference values
	 */
	protected void initializeDefaultPreferences(Preferences prefs) {
	}
	

}

