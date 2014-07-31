/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *

 *******************************************************************************/
package org.python.pydev.shared_interactive_console.console.ui;

import java.io.File;

/**
 * Constants for the console UI.
 */
public class ScriptConsoleUIConstants {

    public static final String TERMINATE_ICON = "terminate.gif"; //$NON-NLS-1$

    public static final String INTERRUPT_ICON = "interrupt.gif"; //$NON-NLS-1$

    public static final String SAVE_SESSION_ICON = "save.gif"; //$NON-NLS-1$

    public static final String LINK_WITH_DEBUGGER = "sync_ed.gif"; //$NON-NLS-1$

    public static final String ICONS_PATH = File.separator + "icons";

    public static final String DEBUG_CONSOLE_TYPE = "PydevDebugConsole";

    public static final String INTERACTIVE_CONSOLE_PERSISTENT_HISTORY_MAXIMUM_ENTRIES = "INTERACTIVE_CONSOLE_PERSISTENT_HISTORY_MAXIMUM_ENTRIES";

    public static final int DEFAULT_INTERACTIVE_CONSOLE_PERSISTENT_HISTORY_MAXIMUM_ENTRIES = 200;
}
