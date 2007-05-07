package org.python.pydev.outline;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

/**
 * Action that will hide comments in the outline
 * 
 * @author Fabio
 */
public class OutlineHideCommentsAction extends Action {
    private static final String PREF_HIDE_COMMENTS = "org.python.pydev.OUTLINE_HIDE_COMMENTS";

    private ViewerFilter hideCommentsFilter;

    private WeakReference<PyOutlinePage> page;

    public OutlineHideCommentsAction(PyOutlinePage page, ImageCache imageCache) {
        super("Hide Comments", IAction.AS_CHECK_BOX);
        this.page = new WeakReference<PyOutlinePage>(page);

        setChecked(page.getStore().getBoolean(PREF_HIDE_COMMENTS));
        setHideComments(isChecked());

        try {
            setImageDescriptor(imageCache.getDescriptor(UIConstants.COMMENT_BLACK));
        } catch (MalformedURLException e) {
            PydevPlugin.log("Missing Icon", e);
        }
    }

    public void run() {
        setHideComments(isChecked());
    }

    /**
     * Should we hide the comments?
     */
    protected void setHideComments(boolean doHideComments) {
        PyOutlinePage p = this.page.get();
        if (p != null) {
            p.getStore().setValue(PREF_HIDE_COMMENTS, doHideComments);
            ViewerFilter filter = getHideCommentsFilter();
            if (doHideComments) {
                p.getTreeViewer().addFilter(filter);
            } else {
                p.getTreeViewer().removeFilter(filter);
            }
        }
    }

    /**
     * @return the filter used to hide comments
     */
    private ViewerFilter getHideCommentsFilter() {
        if (hideCommentsFilter == null) {
            hideCommentsFilter = new ViewerFilter() {

                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if (element instanceof ParsedItem) {
                        ParsedItem item = (ParsedItem) element;
                        if (item == null || item.astThis == null || !(item.astThis.node instanceof commentType)) {
                            return true;
                        }

                    }
                    return false;
                }

            };
        }
        return hideCommentsFilter;
    }

}
