/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui;

import org.eclipse.swt.widgets.Composite;

public interface IDebugPreferencesPageParticipant {

    void createFieldEditors(DebugPrefsPage page, Composite parent);

}
