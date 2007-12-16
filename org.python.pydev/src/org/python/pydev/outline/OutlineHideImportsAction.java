package org.python.pydev.outline;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

/**
 * Action that will hide the imports in the outline
 * 
 * @author Fabio
 */
public class OutlineHideImportsAction extends Action {

    private static final String PREF_HIDE_IMPORTS = "org.python.pydev.OUTLINE_HIDE_IMPORTS";

    private ViewerFilter hideImportsFilter;

    private WeakReference<PyOutlinePage> page;

    public OutlineHideImportsAction(PyOutlinePage page, ImageCache imageCache) {
        super("Hide Imports", IAction.AS_CHECK_BOX);
        this.page = new WeakReference<PyOutlinePage>(page);

        // restore it
        setChecked(page.getStore().getBoolean(PREF_HIDE_IMPORTS));
        setHideImports(isChecked());
        try {
            setImageDescriptor(imageCache.getDescriptor(UIConstants.IMPORT_ICON));
        } catch (MalformedURLException e) {
            PydevPlugin.log("Missing Icon", e);
        }
    }

    public void run() {
        setHideImports(isChecked());
    }

    /**
     * Should we hide the comments?
     */
    protected void setHideImports(boolean doHideImports) {
        PyOutlinePage p = this.page.get();
        if (p != null) {
            p.getStore().setValue(PREF_HIDE_IMPORTS, doHideImports);
            ViewerFilter filter = getHideImportsFilter();
            if (doHideImports) {
                p.getTreeViewer().addFilter(filter);
            } else {
                p.getTreeViewer().removeFilter(filter);
            }
        }
    }

    /**
     * @return the filter used to hide comments
     */
    private ViewerFilter getHideImportsFilter() {
        if (hideImportsFilter == null) {
            hideImportsFilter = new ViewerFilter() {

                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if (element instanceof ParsedItem) {
                        ParsedItem item = (ParsedItem) element;
                        SimpleNode n = item.getAstThis().node;
                        if (n instanceof ImportFrom || n instanceof Import) {
                            return false;
                        }

                    }
                    return true;
                }

            };
        }
        return hideImportsFilter;
    }

}
