package org.python.pydev.debug.ui;

import org.eclipse.swt.widgets.Composite;

public interface IDebugPreferencesPageParticipant {

    void createFieldEditors(DebugPrefsPage page, Composite parent);

    
}
