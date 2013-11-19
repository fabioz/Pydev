/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.python.pydev.core.ExtensionHelper;

/**
 * @author Fabio Zadrozny
 */
public class PyShowBrowser extends PyShowOutline {

    @Override
    protected String getExtensionName() {
        return ExtensionHelper.PYDEV_GLOBALS_BROWSER;
    }
}
