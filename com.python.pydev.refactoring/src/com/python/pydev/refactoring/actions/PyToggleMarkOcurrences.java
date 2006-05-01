/*
 * Created on Apr 29, 2006
 */
package com.python.pydev.refactoring.actions;

import java.util.ResourceBundle;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * References: 
 * 
 * - org.eclipse.jdt.internal.ui.javaeditor.ToggleMarkOccurrencesAction
 * - org.eclipse.jdt.internal.ui.javaeditor.JavaEditor #markOccurrencesOfType, #updateOccurrenceAnnotations
 * 
 * This class only makes the 'toggle' for the ocurrences.
 * 
 * @author Fabio
 */
public class PyToggleMarkOcurrences extends TextEditorAction implements IPropertyChangeListener {
    public PyToggleMarkOcurrences(ResourceBundle resourceBundle) {
        super(resourceBundle, "PyToggleMarkOccurrencesAction.", null, IAction.AS_CHECK_BOX); //$NON-NLS-1$
        update();
    }

    public void propertyChange(PropertyChangeEvent event) {
    }

}
