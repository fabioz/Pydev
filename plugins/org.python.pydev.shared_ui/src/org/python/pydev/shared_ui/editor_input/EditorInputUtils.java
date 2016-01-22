/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.editor_input;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IURIEditorInput;

public class EditorInputUtils {

    /**
     * @return a file that the passed editor input wraps or null if it can't find out about it.
     */
    public static File getFile(IEditorInput o) {
        if (o == null) {
            return null;
        }

        IFile file = o.getAdapter(IFile.class);
        if (file != null) {
            URI locationURI = file.getLocationURI();
            if (locationURI == null) {
                return null;
            }
            return new File(locationURI);
        }

        URI uri = o.getAdapter(URI.class);
        if (uri != null) {
            return new File(uri);
        }

        if (o instanceof PydevFileEditorInput) {
            PydevFileEditorInput input = (PydevFileEditorInput) o;
            return input.fFile;
        }

        if (o instanceof IPathEditorInput) {
            IPathEditorInput input = (IPathEditorInput) o;
            return new File(input.getPath().toOSString());
        }

        try {
            if (o instanceof IURIEditorInput) {
                IURIEditorInput iuriEditorInput = (IURIEditorInput) o;
                return new File(iuriEditorInput.getURI());
            }
        } catch (Throwable e) {
            //IURIEditorInput not added until eclipse 3.3
        }
        return null;
    }

}
