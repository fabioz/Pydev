/**
 * Copyright (c) 2016 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 * 
 * Copied from the JDT implementation of
 * <code>org.eclipse.jdt.internal.ui.preferences.ScrolledPageContent</code>.
 */
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
