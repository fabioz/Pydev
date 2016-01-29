package org.python.pydev.plugin.preferences;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

public class ScrolledPageContent extends SharedScrolledComposite {

    private FormToolkit fToolkit;

    public ScrolledPageContent(Composite parent) {
        this(parent, SWT.V_SCROLL | SWT.H_SCROLL);
    }

    public ScrolledPageContent(Composite parent, int style) {
        super(parent, style);

        setFont(parent.getFont());

        fToolkit = JavaPlugin.getDefault().getDialogsFormToolkit();

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
