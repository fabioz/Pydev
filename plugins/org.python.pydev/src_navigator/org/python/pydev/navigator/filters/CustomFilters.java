package org.python.pydev.navigator.filters;

import java.lang.ref.WeakReference;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.navigator.actions.PySetupCustomFilters;
import org.python.pydev.navigator.properties.StringMatcherSimple;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Will filter out any resource that matches a filter that the user specified.
 *
 * @author Fabio
 */
public class CustomFilters extends ViewerFilter{

    /**
     * This property listener will just store a weak reference to the custom filter that actually needs the values
     * (so that it is not kept alive by registering itself in the preferences).
     *
     * @author Fabio
     */
    private static class PropertyListener implements IPropertyChangeListener{

        private WeakReference<CustomFilters> weakCustomFilter;
        
        public PropertyListener(CustomFilters customFilter){
            weakCustomFilter = new WeakReference<CustomFilters>(customFilter);
            IPreferenceStore prefs = PydevPlugin.getDefault().getPreferenceStore();
            prefs.addPropertyChangeListener(this);
        }
        
        public void propertyChange(PropertyChangeEvent event) {
            CustomFilters customFilters = weakCustomFilter.get();
            if(customFilters == null){
                IPreferenceStore prefs = PydevPlugin.getDefault().getPreferenceStore();
                prefs.removePropertyChangeListener(this);
            }else{
                String property = event.getProperty();
                if(property.equals(PySetupCustomFilters.CUSTOM_FILTERS_PREFERENCE_NAME)){
                    customFilters.update((String)event.getNewValue());
                }
            }
        }
        
    }
    
    
    /**
     * Update the initial filters and register a listener for it.
     */
    public CustomFilters(){
        IPreferenceStore prefs = PydevPlugin.getDefault().getPreferenceStore();
        update(prefs.getString(PySetupCustomFilters.CUSTOM_FILTERS_PREFERENCE_NAME));
        new PropertyListener(this); //this is the listener that will update this filter
    }
    
    
    /**
     * Filter things out based on the filter.
     */
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if(element instanceof IAdaptable){
            IAdaptable adaptable = (IAdaptable) element;
            Object adapted = adaptable.getAdapter(IResource.class);
            if(adapted instanceof IResource){
                IResource resource = (IResource) adapted;
                String name = resource.getName();
                StringMatcherSimple[] temp = filters;
                for(int i=0; i<temp.length;i++){
                    if(temp[i].match(name)){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    
    /**
     * Holds the filters available.
     */
    private StringMatcherSimple[] filters;
    
    
    public void update(String customFilters) {
        String[] splittedCustomFilters = StringUtils.split(customFilters, ',');
        StringMatcherSimple[] temp = new StringMatcherSimple[splittedCustomFilters.length];
        for (int i = 0; i < temp.length; i++) {
            temp[i] = new StringMatcherSimple(splittedCustomFilters[i].trim());
        }
        filters = temp;
    }

}
