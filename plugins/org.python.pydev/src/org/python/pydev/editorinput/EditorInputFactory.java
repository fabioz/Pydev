/******************************************************************************
* Copyright (C) 2015  Brainwy Software Ltda.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial API and implementation
******************************************************************************/
package org.python.pydev.editorinput;

import java.io.File;
import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_ui.editor_input.PydevFileEditorInput;
import org.python.pydev.shared_ui.editor_input.PydevZipFileEditorInput;
import org.python.pydev.shared_ui.editor_input.PydevZipFileStorage;

public class EditorInputFactory {

    /**
     * Creates an editor input for the passed file.
     *
     * If forceExternalFile is True, it won't even try to create a FileEditorInput, otherwise,
     * it will try to create it with the most suitable type it can
     * (i.e.: FileEditorInput, FileStoreEditorInput, PydevFileEditorInput, ...)
     */
    public static IEditorInput create(File file, boolean forceExternalFile) {
        IPath path = Path.fromOSString(FileUtils.getFileAbsolutePath(file));

        if (!forceExternalFile) {
            //May call again to this method (but with forceExternalFile = true)
            IEditorInput input = new PySourceLocatorBase().createEditorInput(path, false, null, null);
            if (input != null) {
                return input;
            }
        }

        IPath zipPath = new Path("");
        while (path.segmentCount() > 0) {
            if (path.toFile().exists()) {
                break;
            }
            zipPath = new Path(path.lastSegment()).append(zipPath);
            path = path.uptoSegment(path.segmentCount() - 1);
        }

        if (zipPath.segmentCount() > 0 && path.segmentCount() > 0) {
            return new PydevZipFileEditorInput(new PydevZipFileStorage(path.toFile(), zipPath.toPortableString()));
        }

        try {
            URI uri = file.toURI();
            return new FileStoreEditorInput(EFS.getStore(uri));
        } catch (Throwable e) {
            //not always available! (only added in eclipse 3.3)
            return new PydevFileEditorInput(file);
        }
    }

}
