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
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

/**
 * Action that will hide the non-public members in the outline
 * 
 * @author laurent.dore
 */
public class OutlineHideNonPublicMembersAction extends Action {

    private static final String PREF_HIDE_NONPUBLICMEMBERS = "org.python.pydev.OUTLINE_HIDE_NONPUBLICMEMBERS";

    private ViewerFilter hideNonPublicMembersFilter;

    private WeakReference<PyOutlinePage> page;

    public OutlineHideNonPublicMembersAction(PyOutlinePage page, ImageCache imageCache) {
        super("Hide Non-Public Members", IAction.AS_CHECK_BOX);
        this.page = new WeakReference<PyOutlinePage>(page);

        // restore it
        setChecked(page.getStore().getBoolean(PREF_HIDE_NONPUBLICMEMBERS));
        setHideNonPublicMembers(isChecked());
        try {
            setImageDescriptor(imageCache.getDescriptor(UIConstants.PUBLIC_ATTR_ICON));
        } catch (MalformedURLException e) {
            PydevPlugin.log("Missing Icon", e);
        }
    }

    public void run() {
        setHideNonPublicMembers(isChecked());
    }

    /**
     * Should we hide the comments?
     */
    protected void setHideNonPublicMembers(boolean doHideNonPublicMembers) {
        PyOutlinePage p = this.page.get();
        if (p != null) {
            p.getStore().setValue(PREF_HIDE_NONPUBLICMEMBERS, doHideNonPublicMembers);
            ViewerFilter filter = getHideNonPublicMembersFilter();
            if (doHideNonPublicMembers) {
                p.getTreeViewer().addFilter(filter);
            } else {
                p.getTreeViewer().removeFilter(filter);
            }
        }
    }

    /**
     * @return the filter used to hide comments
     */
    private ViewerFilter getHideNonPublicMembersFilter() {
        if (hideNonPublicMembersFilter == null) {
        	hideNonPublicMembersFilter = new ViewerFilter() {

                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if (element instanceof ParsedItem) {
                        ParsedItem item = (ParsedItem) element;

                        SimpleNode token = item.getAstThis().node;

                        String name = null;
                        if (token instanceof ClassDef) {
                        	ClassDef classDefToken = (ClassDef) token;
                        	name = NodeUtils.getNameFromNameTok((NameTok) (classDefToken).name);
                        }
                        else if (token instanceof FunctionDef) {
                        	FunctionDef functionDefToken = (FunctionDef) token;
                        	name = NodeUtils.getNameFromNameTok((NameTok) (functionDefToken).name);
                        }
                        else if (token instanceof Attribute) {
                        	Attribute attributeToken = (Attribute) token;
                        	name = NodeUtils.getNameFromNameTok((NameTok) (attributeToken).attr);
                        }
                        else if (token instanceof Name) {
                        	Name nameToken = (Name) token;
                        	name = nameToken.id;
                        }
                        else if (token instanceof NameTok) {
                        	NameTok nameTokToken = (NameTok) token;
                        	name = NodeUtils.getNameFromNameTok(nameTokToken);
                        }
                        
                        if (name != null) {
                        	return (!name.startsWith("_")) || (name.startsWith("__") && name.endsWith("__"));
                        }
                    }
                    return true;
                }

            };
        }
        return hideNonPublicMembersFilter;
    }
}
