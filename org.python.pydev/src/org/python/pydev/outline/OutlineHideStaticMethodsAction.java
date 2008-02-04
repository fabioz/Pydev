package org.python.pydev.outline;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

/**
 * Action that will hide the static methods in the outline
 * 
 * Note: This only works with the 'staticmethod' decorator.
 * 
 * @author laurent.dore
 */
public class OutlineHideStaticMethodsAction extends Action {

    private static final String PREF_HIDE_STATICMETHODS = "org.python.pydev.OUTLINE_HIDE_STATICMETHODS";

    private ViewerFilter hideStaticMethodsFilter;

    private WeakReference<PyOutlinePage> page;

    public OutlineHideStaticMethodsAction(PyOutlinePage page, ImageCache imageCache) {
        super("Hide Static Methods", IAction.AS_CHECK_BOX);
        this.page = new WeakReference<PyOutlinePage>(page);

        // restore it
        setChecked(page.getStore().getBoolean(PREF_HIDE_STATICMETHODS));
        setHideStaticMethods(isChecked());
        try {
            setImageDescriptor(imageCache.getDescriptor(UIConstants.STATIC_MEMBER_HIDE_ICON));
        } catch (MalformedURLException e) {
            PydevPlugin.log("Missing Icon", e);
        }
    }

    public void run() {
        setHideStaticMethods(isChecked());
    }

    /**
     * Should we hide the comments?
     */
    protected void setHideStaticMethods(boolean doHideStaticMethods) {
        PyOutlinePage p = this.page.get();
        if (p != null) {
            p.getStore().setValue(PREF_HIDE_STATICMETHODS, doHideStaticMethods);
            ViewerFilter filter = getHideStaticMethodsFilter();
            if (doHideStaticMethods) {
                p.getTreeViewer().addFilter(filter);
            } else {
                p.getTreeViewer().removeFilter(filter);
            }
        }
    }

    /**
     * @return the filter used to hide comments
     */
    private ViewerFilter getHideStaticMethodsFilter() {
        if (hideStaticMethodsFilter == null) {
        	hideStaticMethodsFilter = new ViewerFilter() {

                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if (element instanceof ParsedItem) {
                        ParsedItem item = (ParsedItem) element;

                        SimpleNode token = item.getAstThis().node;

                        //String name = null;
                        if (token instanceof FunctionDef) {
                        	FunctionDef functionDefToken = (FunctionDef) token;
                        	for (decoratorsType decorator : functionDefToken.decs) {
                        		if (decorator.func instanceof Name) {
                        			Name decoratorFuncName = (Name) decorator.func;
                        			if (decoratorFuncName.id.equals("staticmethod")) {
                        				return false;
                        			}
                        		}
							}
                        }
                    }
                    return true;
                }
            };
        }
        return hideStaticMethodsFilter;
    }
}
