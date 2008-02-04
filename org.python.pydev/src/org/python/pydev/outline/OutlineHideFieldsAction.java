package org.python.pydev.outline;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

/**
 * Action that will hide the fields in the outline
 * 
 * @author laurent.dore
 */
public class OutlineHideFieldsAction extends Action {

    private static final String PREF_HIDE_FIELDS = "org.python.pydev.OUTLINE_HIDE_FIELDS";

    private ViewerFilter hideFieldsFilter;

    private WeakReference<PyOutlinePage> page;

    public OutlineHideFieldsAction(PyOutlinePage page, ImageCache imageCache) {
        super("Hide Fields", IAction.AS_CHECK_BOX);
        this.page = new WeakReference<PyOutlinePage>(page);

        // restore it
        setChecked(page.getStore().getBoolean(PREF_HIDE_FIELDS));
        setHideFields(isChecked());
        try {
            setImageDescriptor(imageCache.getDescriptor(UIConstants.FIELDS_HIDE_ICON));
        } catch (MalformedURLException e) {
            PydevPlugin.log("Missing Icon", e);
        }
    }

    public void run() {
        setHideFields(isChecked());
    }

    /**
     * Should we hide the comments?
     */
    protected void setHideFields(boolean doHideFields) {
        PyOutlinePage p = this.page.get();
        if (p != null) {
            p.getStore().setValue(PREF_HIDE_FIELDS, doHideFields);
            ViewerFilter filter = getHideFieldsFilter();
            if (doHideFields) {
                p.getTreeViewer().addFilter(filter);
            } else {
                p.getTreeViewer().removeFilter(filter);
            }
        }
    }

    /**
     * @return the filter used to hide comments
     */
    private ViewerFilter getHideFieldsFilter() {
        if (hideFieldsFilter == null) {
        	hideFieldsFilter = new ViewerFilter() {

                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if (element instanceof ParsedItem) {
                        ParsedItem item = (ParsedItem) element;

                        SimpleNode token = item.getAstThis().node;

                        //String name = null;
                        if (token instanceof Attribute) {
                        	return false;
                        }
                        else if (token instanceof Name) {
                        	if (parentElement instanceof ParsedItem) {
                        		ParsedItem parentItem = (ParsedItem) parentElement;
                        		if (parentItem != null) {
                        			ASTEntry ast = parentItem.getAstThis();
                        			if (ast != null) {
		                                SimpleNode parentToken = ast.node;
		                                
		                                if (parentToken instanceof ClassDef) {
		                                	return false;
		                                }
                        			}
                        		}
                        	}
                        }
                    }
                    return true;
                }
            };
        }
        return hideFieldsFilter;
    }
}
