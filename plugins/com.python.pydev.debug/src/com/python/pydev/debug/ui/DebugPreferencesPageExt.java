package com.python.pydev.debug.ui;

import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.debug.ui.DebugPrefsPage;
import org.python.pydev.debug.ui.IDebugPreferencesPageParticipant;

import com.python.pydev.debug.DebugPluginPrefsInitializer;

public class DebugPreferencesPageExt implements IDebugPreferencesPageParticipant {


    public void createFieldEditors(DebugPrefsPage page, Composite parent) {
        page.addField(new IntegerFieldEditor(DebugPluginPrefsInitializer.PYDEV_REMOTE_DEBUGGER_PORT, "Port for remote debugger:", parent, 10));
    }

}
