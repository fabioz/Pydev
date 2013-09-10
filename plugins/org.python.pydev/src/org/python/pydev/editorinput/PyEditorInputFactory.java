/******************************************************************************
* Copyright (C) 2011-2012  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.editorinput;

import java.io.File;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class PyEditorInputFactory implements IElementFactory {

    public static final String FACTORY_ID = "org.python.pydev.editorinput.pyEditorInputFactory";

    public IAdaptable createElement(IMemento memento) {
        String file = memento.getString(TAG_FILE);
        if (file == null || file.length() == 0) {
            return null;
        }

        String zipPath = memento.getString(TAG_ZIP_PATH);
        if (zipPath == null || zipPath.length() == 0) {
            return PydevFileEditorInput.create(new File(file), false);
        }

        return new PydevZipFileEditorInput(new PydevZipFileStorage(new File(file), zipPath));
    }

    private static final String TAG_FILE = "file"; //$NON-NLS-1$

    private static final String TAG_ZIP_PATH = "zip_path"; //$NON-NLS-1$

    public static void saveState(IMemento memento, PydevZipFileEditorInput pydevZipFileEditorInput) {
        memento.putString(TAG_FILE, pydevZipFileEditorInput.getFile().toString());
        memento.putString(TAG_ZIP_PATH, pydevZipFileEditorInput.getZipPath());
    }

    public static void saveState(IMemento memento, PydevFileEditorInput pydevFileEditorInput) {
        memento.putString(TAG_FILE, pydevFileEditorInput.getFile().toString());
    }

}
