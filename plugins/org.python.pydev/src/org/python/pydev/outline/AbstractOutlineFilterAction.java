package org.python.pydev.outline;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This class is provided as a base class for filters acting in the outline nodes.
 *
 * @author Fabio
 */
public abstract class AbstractOutlineFilterAction extends Action{

    protected ViewerFilter filter;

    protected WeakReference<PyOutlinePage> page;

    protected String preference;
    
    /**
     * @param text This is the text explaining this action (shown to the user)
     * @param page the outline page where the action should take place
     * @param imageCache the image cache to be used to get the icon
     * @param preference the name of the preference that should keep the preferences (to keep it checked among sessions)
     * @param uiConstant the name of the constant in UIConstants to get the icon from the image cache.
     */
    public AbstractOutlineFilterAction(String text, PyOutlinePage page, ImageCache imageCache, String preference, 
            String uiConstant) {
        super(text, IAction.AS_CHECK_BOX);
        this.preference = preference;
        
        this.page = new WeakReference<PyOutlinePage>(page);

        setChecked(page.getStore().getBoolean(preference));
        setActionEnabled(isChecked());

        try {
            setImageDescriptor(imageCache.getDescriptor(uiConstant));
        } catch (MalformedURLException e) {
            PydevPlugin.log("Missing Icon", e);
        }
    }
    
    public void run() {
        setActionEnabled(isChecked());
    }


    /**
     * Should we hide things?
     */
    protected void setActionEnabled(boolean enableAction) {
        PyOutlinePage p = this.page.get();
        if (p != null) {
            p.getStore().setValue(preference, enableAction);
            if(filter == null){
                filter = createFilter();
            }
            
            TreeViewer treeViewer = p.getTreeViewer();
            if(treeViewer != null){
                if (enableAction) {
                    treeViewer.addFilter(filter);
                } else {
                    treeViewer.removeFilter(filter);
                }
            }
        }
    }

    /**
     * Subclasses should implement this method to return a filter to filter whatever it should target.
     */
    protected abstract ViewerFilter createFilter();

}
