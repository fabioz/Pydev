/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.ui;

import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.debug.ui.DebugPrefsPage;
import org.python.pydev.debug.ui.IDebugPreferencesPageParticipant;

import com.python.pydev.debug.DebugPluginPrefsInitializer;

public class DebugPreferencesPageExt implements IDebugPreferencesPageParticipant {

    public void createFieldEditors(DebugPrefsPage page, Composite parent) {
        page.addField(new IntegerFieldEditor(DebugPluginPrefsInitializer.PYDEV_REMOTE_DEBUGGER_PORT,
                "Port for remote debugger:", parent, 10));
    }

}
