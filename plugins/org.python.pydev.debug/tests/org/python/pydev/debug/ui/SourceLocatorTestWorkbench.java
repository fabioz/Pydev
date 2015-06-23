/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.editorinput.EditorInputFactory;
import org.python.pydev.editorinput.PySourceLocatorBase;
import org.python.pydev.editorinput.PySourceLocatorPrefs;

public class SourceLocatorTestWorkbench extends AbstractWorkbenchTestCase {

    public void testSourceLocator() throws Exception {
        final Boolean[] called = new Boolean[] { false };
        final IPath madeUpPath = mod1.getLocation().append("inexistent");

        PySourceLocatorBase locator = new PySourceLocatorBase() {
            @Override
            protected IEditorInput selectFilesystemFileForPath(IPath path) {
                called[0] = true;
                assertEquals(path, madeUpPath);
                return EditorInputFactory.create(new File(path.removeLastSegments(1).toOSString()), true);
            }
        };
        IEditorInput editorInput = locator.createEditorInput(madeUpPath);
        assertTrue(editorInput != null);
        assertTrue(called[0]);
        called[0] = false;

        editorInput = locator.createEditorInput(madeUpPath);
        assertTrue(!called[0]);
        assertTrue(editorInput != null);

        PySourceLocatorPrefs.setIgnorePathTranslation(madeUpPath);
        editorInput = locator.createEditorInput(madeUpPath);
        assertTrue(!called[0]);
        assertTrue(editorInput == null);

    }

}
