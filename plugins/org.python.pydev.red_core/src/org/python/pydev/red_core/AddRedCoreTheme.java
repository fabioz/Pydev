/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.red_core;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.IPyEditListener4;
import org.python.pydev.editor.PyEdit;

/**
 * Adds the features related to Aptana red core, such as the find bar and theming.
 */
public class AddRedCoreTheme implements IPyEditListener, IPyEditListener4{

	private AddRedCoreThemeImpl addRedCoreThemeImpl;

	public void onEditorCreated(final PyEdit edit) {
		if(!AddRedCoreThemeAvailable.isRedCoreAvailable()){
			return;
		}
		try {
			addRedCoreThemeImpl = new AddRedCoreThemeImpl();
			addRedCoreThemeImpl.installRedCoreTheme(edit);
		} catch (Throwable e) {
			//If we have some problem here, there's some versioning problem, let's log it and
			//signal it's not available.
			Log.log(e);
			AddRedCoreThemeAvailable.setRedCoreAvailable(false);
		}
	}

	public void onSave(PyEdit edit, IProgressMonitor monitor) {
		
		
	}

	public void onCreateActions(ListResourceBundle resources, PyEdit edit,
			IProgressMonitor monitor) {
		
		
	}

	public void onDispose(PyEdit edit, IProgressMonitor monitor) {
		
		
	}

	public void onSetDocument(IDocument document, PyEdit edit,
			IProgressMonitor monitor) {
		
		
	}

}
