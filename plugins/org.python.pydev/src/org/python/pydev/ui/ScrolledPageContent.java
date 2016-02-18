/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.python.pydev.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.python.pydev.plugin.PydevPlugin;

public class ScrolledPageContent extends SharedScrolledComposite {

    private FormToolkit fToolkit;

    public ScrolledPageContent(Composite parent) {
        this(parent, SWT.V_SCROLL | SWT.H_SCROLL);
    }

    public ScrolledPageContent(Composite parent, int style) {
        super(parent, style);

        setFont(parent.getFont());

        fToolkit = PydevPlugin.getDefault().getDialogsFormToolkit();

        setExpandHorizontal(true);
        setExpandVertical(true);

        Composite body = new Composite(this, SWT.NONE);
        body.setFont(parent.getFont());
        setContent(body);
    }

    public void adaptChild(Control childControl) {
        fToolkit.adapt(childControl, true, true);
    }

    public Composite getBody() {
        return (Composite) getContent();
    }

}
