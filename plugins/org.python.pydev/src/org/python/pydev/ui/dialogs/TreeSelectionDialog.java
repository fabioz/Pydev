/******************************************************************************
* Copyright (C) 2009-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.ui.dialogs;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.ui.NotifyViewCreated;

public class TreeSelectionDialog extends org.python.pydev.shared_ui.dialogs.TreeSelectionDialog {

    public TreeSelectionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
        super(parent, labelProvider, contentProvider);
    }

    @Override
    protected void notifyViewCreated() {
        super.notifyViewCreated();
        NotifyViewCreated.notifyViewCreated(this);
    }

    @Override
    protected Control createContents(Composite parent) {
        Control ret = super.createContents(parent);
        org.python.pydev.plugin.PydevPlugin.setCssId(parent, "py-tree-selection-dialog", true);
        return ret;
    }
}
