/*
 * Created on Aug 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.python.pydev.plugin.PydevPrefs;

/**
 * @author Fabio Zadrozny
 */
public class PyContentAssistant extends ContentAssistant implements Preferences.IPropertyChangeListener{

    public PyContentAssistant(){
        PydevPrefs.getPreferences().addPropertyChangeListener(this);
//		enableAutoActivation(PyCodeCompletionPreferencesPage.useAutocomplete());
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
//        if(event.getProperty().equals(PyCodeCompletionPreferencesPage.USE_AUTOCOMPLETE)){
//            this.enableAutoActivation( ((Boolean)event.getNewValue()).booleanValue() );
//        }
    }
}
