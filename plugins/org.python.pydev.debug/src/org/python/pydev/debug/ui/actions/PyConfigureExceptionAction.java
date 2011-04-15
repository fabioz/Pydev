package org.python.pydev.debug.ui.actions;

import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.editor.actions.PyShowOutline;

public class PyConfigureExceptionAction extends PyShowOutline {

	protected String getExtensionName() {
		return ExtensionHelper.PYDEV_CONFIGURE_EXCEPTION;
	}
}
