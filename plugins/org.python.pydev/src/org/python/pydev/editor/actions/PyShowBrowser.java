package org.python.pydev.editor.actions;

import org.python.pydev.core.ExtensionHelper;

/**
 * @author Fabio Zadrozny
 */
public class PyShowBrowser extends PyShowOutline{

    @Override
    protected String getExtensionName() {
        return ExtensionHelper.PYDEV_GLOBALS_BROWSER;
    }
}
