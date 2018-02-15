package org.python.pydev.editor;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.swt.widgets.Shell;

public class PyInformationControlCreator implements IInformationControlCreator {

    @Override
    public IInformationControl createInformationControl(Shell parent) {
        return new DefaultInformationControl(parent, new PyInformationPresenter());
    }

}
