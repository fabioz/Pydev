/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.preferences;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.editor.preferences.PydevEditorPrefs;

public interface IPydevPreferencesProvider2 {

    /**
     * @return true if it handled the creation of the color options and false otherwise.
     */
    boolean createColorOptions(Composite appearanceComposite, PydevEditorPrefs prefs);

}
