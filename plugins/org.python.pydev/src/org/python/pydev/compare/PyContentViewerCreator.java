package org.python.pydev.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

public class PyContentViewerCreator implements IViewerCreator {

    public Viewer createViewer(Composite parent, CompareConfiguration mp) {
        return new PyContentViewer(parent, mp);
    }
}
