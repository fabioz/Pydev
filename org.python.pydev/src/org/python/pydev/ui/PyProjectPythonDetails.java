/*
 * Created on Jan 25, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;

/**
 * @author Fabio Zadrozny
 */
public class PyProjectPythonDetails extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {

    public static final String PYTHON_VERSION = "PYTHON_VERSION";

    public static final String DEFAULT_PYTHON_VERSION = "2.3";
    public static final String PYTHON_VERSION_2_4 = "2.4";

    /**
     */
    public PyProjectPythonDetails() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        RadioGroupFieldEditor editor= new RadioGroupFieldEditor(
            PYTHON_VERSION, 
			"Python Version", 
			2,
			new String[][] {
				{"Version 2.3", DEFAULT_PYTHON_VERSION},
				{"Version 2.4", PYTHON_VERSION_2_4}
			},
          p, 
          true);	
        addField(editor);
    }
    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }
    
    public static String getPythonVersion() {
        return PydevPrefs.getPreferences().getString(PYTHON_VERSION);
    }

	/**
	 * The element.
	 */
	private IAdaptable element;

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPropertyPage#getElement()
	 */
	public IAdaptable getElement() {
		return element;
	}
	/**
	 * Sets the element that owns properties shown on this page.
	 * 
	 * @param element
	 *            the element
	 */
	public void setElement(IAdaptable element) {
		this.element = element;
	}


}