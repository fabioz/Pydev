/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 21, 2006
 */
package org.python.pydev.django.debug.ui.actions;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;

/**
 * Creates any django-related actions for an editor.
 */
public class DjangoActionCreatorPyEditListener implements IPyEditListener {

    @Override
    public void onSave(BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    @Override
    public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {
        PyEdit edit = (PyEdit) baseEditor;
        edit.addOfflineActionListener("dj", new PyDjangoOfflineAction(edit), "Execute django action", true);
    }

    @Override
    public void onDispose(BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    @Override
    public void onSetDocument(IDocument document, BaseEditor baseEditor, IProgressMonitor monitor) {
    }

}
