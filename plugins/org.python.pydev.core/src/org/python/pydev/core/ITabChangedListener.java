/**
 * Copyright (c) 2015 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

public interface ITabChangedListener {

    public void onTabSettingsChanged(IIndentPrefs prefs);
}
