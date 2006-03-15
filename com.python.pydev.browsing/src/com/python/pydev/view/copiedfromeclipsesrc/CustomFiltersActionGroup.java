/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.python.pydev.view.copiedfromeclipsesrc;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionGroup;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.browsing.BrowsingPlugin;
import com.python.pydev.browsing.ui.UIConstants;
import com.python.pydev.view.filters.PydevViewerFilterGenerator;

/**
 * Action group to add the filter action to a view part's tool bar
 * menu.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class CustomFiltersActionGroup extends ActionGroup {
	private final String OpenCustomFiltersDialogAction_text = "Filters...";	
	
	class ShowFilterDialogAction extends Action {
		private ImageCache imageCache;
		
		ShowFilterDialogAction() {
			imageCache = new ImageCache(BrowsingPlugin.getDefault().getBundle().getEntry("/"));
			
			setText(OpenCustomFiltersDialogAction_text); 
			try {			
				setImageDescriptor(imageCache.getDescriptor(UIConstants.FILTERS_PS));
				setDisabledImageDescriptor(imageCache.getDescriptor(UIConstants.FILTERS_PS));
			} catch (MalformedURLException e) {			
				e.printStackTrace();
			}			
		}
		
		public void run() {
			openDialog();
		}
	}

	/**
	 * Menu contribution item which shows and lets check and uncheck filters.
	 * 
	 * @since 3.0
	 */
	class FilterActionMenuContributionItem extends ContributionItem {

		private int fItemNumber;
		private boolean fState;
		private String fFilterId;
		private String fFilterName;
		private CustomFiltersActionGroup fActionGroup;

		/**
		 * Constructor for FilterActionMenuContributionItem.
		 * 
		 * @param actionGroup 	the action group
		 * @param filterId		the id of the filter
		 * @param filterName	the name of the filter
		 * @param state			the initial state of the filter
		 * @param itemNumber	the menu item index
		 */
		public FilterActionMenuContributionItem(CustomFiltersActionGroup actionGroup, String filterId, String filterName, boolean state, int itemNumber) {
			super(filterId);
			Assert.isNotNull(actionGroup);
			Assert.isNotNull(filterId);
			Assert.isNotNull(filterName);
			fActionGroup= actionGroup;
			fFilterId= filterId;
			fFilterName= filterName;
			fState= state;
			fItemNumber= itemNumber;
		}

		/*
		 * Overrides method from ContributionItem.
		 */
		public void fill(Menu menu, int index) {
			MenuItem mi= new MenuItem(menu, SWT.CHECK, index);
			mi.setText("&" + fItemNumber + " " + fFilterName);  //$NON-NLS-1$  //$NON-NLS-2$
			/*
			 * XXX: Don't set the image - would look bad because other menu items don't provide image
			 * XXX: Get working set specific image name from XML - would need to cache icons
			 */
//			mi.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVA_WORKING_SET));
			mi.setSelection(fState);
			mi.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fState= !fState;
					fActionGroup.setFilter(fFilterId, fState);
				}
			});
		}
	
		/*
		 * @see org.eclipse.jface.action.IContributionItem#isDynamic()
		 */
		public boolean isDynamic() {
			return true;
		}
	}

	private static final String TAG_CUSTOM_FILTERS = "customFilters"; //$NON-NLS-1$
	private static final String TAG_USER_DEFINED_PATTERNS_ENABLED= "userDefinedPatternsEnabled"; //$NON-NLS-1$
	private static final String TAG_USER_DEFINED_PATTERNS= "userDefinedPatterns"; //$NON-NLS-1$
	private static final String TAG_XML_DEFINED_FILTERS= "xmlDefinedFilters"; //$NON-NLS-1$
	private static final String TAG_LRU_FILTERS = "lastRecentlyUsedFilters"; //$NON-NLS-1$

	private static final String TAG_CHILD= "child"; //$NON-NLS-1$
	private static final String TAG_PATTERN= "pattern"; //$NON-NLS-1$
	private static final String TAG_FILTER_ID= "filterId"; //$NON-NLS-1$
	private static final String TAG_IS_ENABLED= "isEnabled"; //$NON-NLS-1$

	private static final String SEPARATOR= ",";  //$NON-NLS-1$

	private static final int MAX_FILTER_MENU_ENTRIES= 3;
	private static final String RECENT_FILTERS_GROUP_NAME= "recentFiltersGroup"; //$NON-NLS-1$

	private StructuredViewer fViewer;

	private NamePatternFilter fPatternFilter;
	private Map fInstalledBuiltInFilters;
	
	private Map fEnabledFilterIds;
	private boolean fUserDefinedPatternsEnabled;
	private String[] fUserDefinedPatterns;
	private FilterDescriptor[] fCachedFilterDescriptors;

	/**
	 * Recently changed filter Ids stack with oldest on top (i.e. at the end).
	 *
	 * @since 3.0
	 */
	private Stack fLRUFilterIdsStack; 
	/**
	 * Handle to menu manager to dynamically update
	 * the last recently used filters.
	 * 
	 * @since 3.0
	 */
	private IMenuManager fMenuManager;
	/**
	 * The menu listener which dynamically updates
	 * the last recently used filters.
	 * 
	 * @since 3.0
	 */
	private IMenuListener fMenuListener;
	/**
	 * Filter Ids used in the last view menu invocation.
	 * 
	 * @since 3.0
	 */
	private String[] fFilterIdsUsedInLastViewMenu;
	private HashMap fFilterDescriptorMap;
	private String fTargetId;
	
	/**
	 * Creates a new <code>CustomFiltersActionGroup</code>.
	 * 
	 * @param part		the view part that owns this action group
	 * @param viewer	the viewer to be filtered
	 */
	public CustomFiltersActionGroup(IViewPart part, StructuredViewer viewer) {
		this(part.getViewSite().getId(), viewer);
	}

	/**
	 * Creates a new <code>CustomFiltersActionGroup</code>.
	 * 
	 * @param ownerId	the id of this action group's owner
	 * @param viewer	the viewer to be filtered
	 */
	public CustomFiltersActionGroup(String ownerId, StructuredViewer viewer) {
		Assert.isNotNull(ownerId);
		Assert.isNotNull(viewer);
		fTargetId= ownerId;
		fViewer= viewer;

		fLRUFilterIdsStack= new Stack();

		initializeWithPluginContributions();
		initializeWithViewDefaults();
		
		installFilters();
	}
	
	/*
	 * Method declared on ActionGroup.
	 */
	public void fillActionBars(IActionBars actionBars) {
		fillToolBar(actionBars.getToolBarManager());
		fillViewMenu(actionBars.getMenuManager());
	}
	
	/**
	 * Returns a list of currently enabled filters. The filter
	 * is identified by its id.
	 * <p>
	 * This method is for internal use only and should not
	 * be called by clients outside of JDT/UI.
	 * </p>
	 * 
	 * @return a list of currently enabled filters
	 * 
	 * @since 3.1
	 */
	public String[] internalGetEnabledFilterIds() {
		Set enabledFilterIds= new HashSet(fEnabledFilterIds.size());
		Iterator iter= fEnabledFilterIds.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry= (Map.Entry)iter.next();
			String id= (String)entry.getKey();
			boolean isEnabled= ((Boolean)entry.getValue()).booleanValue();
			if (isEnabled)
				enabledFilterIds.add(id);
		}
		return (String[])enabledFilterIds.toArray(new String[enabledFilterIds.size()]);
	}

	/**
	 * Removes filters for the given parent and element
	 * 
	 * @param parent the parent of the element
	 * @param element the element
	 * @param contentProvider the content provider of the viewer from which 
	 *  the filters will be removed
	 *  
	 * @return the array of new filter ids
	 */
	public String[] removeFiltersFor(Object parent, Object element, IContentProvider contentProvider) {
	    String[] enabledFilters= internalGetEnabledFilterIds();
	    Set newFilters= new HashSet();
	    for (int i= 0; i < enabledFilters.length; i++) {
            String filterName= enabledFilters[i];
            ViewerFilter filter= (ViewerFilter) fInstalledBuiltInFilters.get(filterName);
            if (filter == null)
                newFilters.add(filterName);
            else if (isSelected(parent, element, contentProvider, filter)) 
                newFilters.add(filterName);
        }
	    if (newFilters.size() == enabledFilters.length)
	        return new String[0];
	    return (String[])newFilters.toArray(new String[newFilters.size()]);
	}
	
	/**
	 * Sets the filters to the given array of new filters
	 * 
	 * @param newFilters the new filters
	 */
	public void setFilters(String[] newFilters) {
	    setEnabledFilterIds(newFilters);
	    updateViewerFilters(true);
	}
	
	private boolean isSelected(Object parent, Object element, IContentProvider contentProvider, ViewerFilter filter) {
	    if (contentProvider instanceof ITreeContentProvider) {
	        // the element and all its parents have to be selected
	        ITreeContentProvider provider = (ITreeContentProvider) contentProvider;
	        //while (element != null && !(element instanceof IJavaModel)) {
	        while (element != null ) {
	            if (!filter.select(fViewer, parent, element)) 
	                return false;
	            element= provider.getParent( element);
	        }
	        return true;
	    } 
	    return filter.select(fViewer, parent, element);
	}

    /**
	 * Sets the enable state of the given filter.
	 * 
	 * @param filterId the id of the filter
	 * @param state the filter state
	 */
	private void setFilter(String filterId, boolean state) {
		// Renew filter id in LRU stack
		fLRUFilterIdsStack.remove(filterId);
		fLRUFilterIdsStack.add(0, filterId);
		
		fEnabledFilterIds.put(filterId, new Boolean(state));
		storeViewDefaults();
		
		updateViewerFilters(true);
	}
		
	private void setEnabledFilterIds(String[] enabledIds) {
		Iterator iter= fEnabledFilterIds.keySet().iterator();
		while (iter.hasNext()) {
			String id= (String)iter.next();
			fEnabledFilterIds.put(id, Boolean.FALSE);
		}
		for (int i= 0; i < enabledIds.length; i++)
			fEnabledFilterIds.put(enabledIds[i], Boolean.TRUE);
	}

	private void setUserDefinedPatterns(String[] patterns) {
		fUserDefinedPatterns= patterns;
		cleanUpPatternDuplicates();
	}

	/**
	 * Sets the recently changed filters.
	 * 
	 * @param changeHistory the change history
	 * @since 3.0
	 */
	private void setRecentlyChangedFilters(Stack changeHistory) {
		Stack oldestFirstStack= new Stack();
		
		int length= Math.min(changeHistory.size(), MAX_FILTER_MENU_ENTRIES);
		for (int i= 0; i < length; i++)
			oldestFirstStack.push(((FilterDescriptor)changeHistory.pop()).getId());
		
		length= Math.min(fLRUFilterIdsStack.size(), MAX_FILTER_MENU_ENTRIES - oldestFirstStack.size());
		int NEWEST= 0;
		for (int i= 0; i < length; i++) {
			Object filter= fLRUFilterIdsStack.remove(NEWEST);
			if (!oldestFirstStack.contains(filter))
				oldestFirstStack.push(filter);
		}
		fLRUFilterIdsStack= oldestFirstStack;
	}
	
	private boolean areUserDefinedPatternsEnabled() {
		return fUserDefinedPatternsEnabled;
	}

	private void setUserDefinedPatternsEnabled(boolean state) {
		fUserDefinedPatternsEnabled= state;
	}

	private void fillToolBar(IToolBarManager tooBar) {
	}

	/**
	 * Fills the given view menu with the entries managed by the
	 * group.
	 * 
	 * @param viewMenu the menu to fill
	 */
	public void fillViewMenu(IMenuManager viewMenu) {
		/*
		 * Don't change the separator group name.
		 * Using this name ensures that other filters
		 * get contributed to the same group.
		 */
		viewMenu.add(new Separator("filters")); //$NON-NLS-1$
		viewMenu.add(new GroupMarker(RECENT_FILTERS_GROUP_NAME));
		viewMenu.add(new ShowFilterDialogAction());

		fMenuManager= viewMenu;
		fMenuListener= new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				removePreviousLRUFilterActions(manager);
				addLRUFilterActions(manager);
			}
		};
		fMenuManager.addMenuListener(fMenuListener);
	}

	private void removePreviousLRUFilterActions(IMenuManager mm) {
		if (fFilterIdsUsedInLastViewMenu == null)
			return;
		
		for (int i= 0; i < fFilterIdsUsedInLastViewMenu.length; i++)
			mm.remove(fFilterIdsUsedInLastViewMenu[i]);
	}

	private void addLRUFilterActions(IMenuManager mm) {
		if (fLRUFilterIdsStack.isEmpty()) {
			fFilterIdsUsedInLastViewMenu= null;
			return;
		}
		
		SortedSet sortedFilters= new TreeSet(fLRUFilterIdsStack);
		String[] recentlyChangedFilterIds= (String[])sortedFilters.toArray(new String[sortedFilters.size()]);
		
		fFilterIdsUsedInLastViewMenu= new String[recentlyChangedFilterIds.length];
		for (int i= 0; i < recentlyChangedFilterIds.length; i++) {
			String id= recentlyChangedFilterIds[i];
			fFilterIdsUsedInLastViewMenu[i]= id;
			boolean state= fEnabledFilterIds.containsKey(id) && ((Boolean)fEnabledFilterIds.get(id)).booleanValue();
			FilterDescriptor filterDesc= (FilterDescriptor)fFilterDescriptorMap.get(id);
			if (filterDesc != null) {
				IContributionItem item= new FilterActionMenuContributionItem(this, id, filterDesc.getName(), state, i+1);
				mm.insertBefore(RECENT_FILTERS_GROUP_NAME, item);
			}
		}
	}

	/*
	 * Method declared on ActionGroup.
	 */
	public void dispose() {
		if (fMenuManager != null)
			fMenuManager.removeMenuListener(fMenuListener);
		fCachedFilterDescriptors= null;
		super.dispose();
	}
	
	private void initializeWithPluginContributions() {
		fUserDefinedPatterns= new String[0];
		fUserDefinedPatternsEnabled= false;

		FilterDescriptor[] filterDescs= getCachedFilterDescriptors();
		fFilterDescriptorMap= new HashMap(filterDescs.length);
		fEnabledFilterIds= new HashMap(filterDescs.length);
		for (int i= 0; i < filterDescs.length; i++) {
			String id= filterDescs[i].getId();
			Boolean isEnabled= new Boolean(filterDescs[i].isEnabled());
			if (fEnabledFilterIds.containsKey(id))
				PydevPlugin.log("WARNING: Duplicate id for extension-point \"org.eclipse.jdt.ui.javaElementFilters\""); //$NON-NLS-1$
			fEnabledFilterIds.put(id, isEnabled);
			fFilterDescriptorMap.put(id, filterDescs[i]);
		}
	}

	// ---------- viewer filter handling ----------
	
	private void installFilters() {
		fInstalledBuiltInFilters= new HashMap(fEnabledFilterIds.size());
		fPatternFilter= new NamePatternFilter();
		fPatternFilter.setPatterns(getUserAndBuiltInPatterns());
		fViewer.addFilter(fPatternFilter);
		updateBuiltInFilters();
	}

	private void updateViewerFilters(boolean refresh) {
		String[] patterns= getUserAndBuiltInPatterns();
		fPatternFilter.setPatterns(patterns);
		fViewer.getControl().setRedraw(false);
		updateBuiltInFilters();
		if (refresh)
			fViewer.refresh();
		fViewer.getControl().setRedraw(true);
	}
	
	private void updateBuiltInFilters() {
		Set installedFilters= fInstalledBuiltInFilters.keySet();
		Set filtersToAdd= new HashSet(fEnabledFilterIds.size());
		Set filtersToRemove= new HashSet(fEnabledFilterIds.size());
		Iterator iter= fEnabledFilterIds.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry= (Map.Entry)iter.next();
			String id= (String)entry.getKey();
			boolean isEnabled= ((Boolean)entry.getValue()).booleanValue();
			if (isEnabled && !installedFilters.contains(id))
				filtersToAdd.add(id);
			else if (!isEnabled && installedFilters.contains(id))
				filtersToRemove.add(id);
		}
		
		// Install the filters
		FilterDescriptor[] filterDescs= getCachedFilterDescriptors();
		for (int i= 0; i < filterDescs.length; i++) {
			String id= filterDescs[i].getId();
			// just to double check - id should denote a custom filter anyway
			boolean isCustomFilter= filterDescs[i].isCustomFilter();
			if (isCustomFilter) {
				if (filtersToAdd.contains(id)) {
					ViewerFilter filter= filterDescs[i].createViewerFilter();
					if (filter != null) {
						fViewer.addFilter(filter);
						fInstalledBuiltInFilters.put(id, filter);
					}
				}
				if (filtersToRemove.contains(id)) {
					fViewer.removeFilter((ViewerFilter)fInstalledBuiltInFilters.get(id));
					fInstalledBuiltInFilters.remove(id);
				}
			}
		}
	}

	private String[] getUserAndBuiltInPatterns() {
		List patterns= new ArrayList(fUserDefinedPatterns.length);
		if (areUserDefinedPatternsEnabled())
			patterns.addAll(Arrays.asList(fUserDefinedPatterns));
		FilterDescriptor[] filterDescs= getCachedFilterDescriptors();
		for (int i= 0; i < filterDescs.length; i++) {
			String id= filterDescs[i].getId();
			boolean isPatternFilter= filterDescs[i].isPatternFilter();
			Object isEnabled= fEnabledFilterIds.get(id);
			if (isEnabled != null && isPatternFilter && ((Boolean)isEnabled).booleanValue())
				patterns.add(filterDescs[i].getPattern());
		}
		return (String[])patterns.toArray(new String[patterns.size()]);
	}

	// ---------- view kind/defaults persistency ----------
		
	private void initializeWithViewDefaults() {
		// get default values for view
		IPreferenceStore store= BrowsingPlugin.getDefault().getPreferenceStore();

		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=22533
		if (!store.contains(getPreferenceKey("TAG_DUMMY_TO_TEST_EXISTENCE")))//$NON-NLS-1$
			return;
		
		fUserDefinedPatternsEnabled= store.getBoolean(getPreferenceKey(TAG_USER_DEFINED_PATTERNS_ENABLED));
		setUserDefinedPatterns(CustomFiltersDialog.convertFromString(store.getString(getPreferenceKey(TAG_USER_DEFINED_PATTERNS)), SEPARATOR));

		Iterator iter= fEnabledFilterIds.keySet().iterator();
		while (iter.hasNext()) {
			String id= (String)iter.next();
			Boolean isEnabled= new Boolean(store.getBoolean(id));
			fEnabledFilterIds.put(id, isEnabled);
		}
		
		fLRUFilterIdsStack.clear();
		String lruFilterIds= store.getString(TAG_LRU_FILTERS);
		StringTokenizer tokenizer= new StringTokenizer(lruFilterIds, SEPARATOR);
		while (tokenizer.hasMoreTokens()) {
			String id= tokenizer.nextToken();
			if (fFilterDescriptorMap.containsKey(id) && !fLRUFilterIdsStack.contains(id))
				fLRUFilterIdsStack.push(id);
		}
	}

	private void storeViewDefaults() {
		// get default values for view
		IPreferenceStore store= BrowsingPlugin.getDefault().getPreferenceStore();

		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=22533
		store.setValue(getPreferenceKey("TAG_DUMMY_TO_TEST_EXISTENCE"), "storedViewPreferences");//$NON-NLS-1$//$NON-NLS-2$
		
		store.setValue(getPreferenceKey(TAG_USER_DEFINED_PATTERNS_ENABLED), fUserDefinedPatternsEnabled);
		store.setValue(getPreferenceKey(TAG_USER_DEFINED_PATTERNS), CustomFiltersDialog.convertToString(fUserDefinedPatterns ,SEPARATOR));

		Iterator iter= fEnabledFilterIds.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry= (Map.Entry)iter.next();
			String id= (String)entry.getKey();
			boolean isEnabled= ((Boolean)entry.getValue()).booleanValue();
			store.setValue(id, isEnabled);
		}

		StringBuffer buf= new StringBuffer(fLRUFilterIdsStack.size() * 20);
		iter= fLRUFilterIdsStack.iterator();
		while (iter.hasNext()) {
			buf.append((String)iter.next());
			buf.append(SEPARATOR);
		}
		store.setValue(TAG_LRU_FILTERS, buf.toString());
	}
	
	private String getPreferenceKey(String tag) {
		return "CustomFiltersActionGroup." + fTargetId + '.' + tag; //$NON-NLS-1$
	}

	// ---------- view instance persistency ----------

	/**
	 * Saves the state of the custom filters in a memento.
	 * 
	 * @param memento the memento into which the state is saved
	 */
	public void saveState(IMemento memento) {
		IMemento customFilters= memento.createChild(TAG_CUSTOM_FILTERS);
		customFilters.putString(TAG_USER_DEFINED_PATTERNS_ENABLED, new Boolean(fUserDefinedPatternsEnabled).toString());
		saveUserDefinedPatterns(customFilters);
		saveXmlDefinedFilters(customFilters);
		saveLRUFilters(customFilters);
	}

	private void saveXmlDefinedFilters(IMemento memento) {
		if(fEnabledFilterIds != null && !fEnabledFilterIds.isEmpty()) {
			IMemento xmlDefinedFilters= memento.createChild(TAG_XML_DEFINED_FILTERS);
			Iterator iter= fEnabledFilterIds.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry= (Map.Entry)iter.next();
				String id= (String)entry.getKey();
				Boolean isEnabled= (Boolean)entry.getValue();
				IMemento child= xmlDefinedFilters.createChild(TAG_CHILD);
				child.putString(TAG_FILTER_ID, id);
				child.putString(TAG_IS_ENABLED, isEnabled.toString());
			}
		}
	}
	/**
	 * Stores the last recently used filter Ids into
	 * the given memento
	 * 
	 * @param memento the memento into which to store the LRU filter Ids
	 * @since 3.0
	 */
	private void saveLRUFilters(IMemento memento) {
		if(fLRUFilterIdsStack != null && !fLRUFilterIdsStack.isEmpty()) {
			IMemento lruFilters= memento.createChild(TAG_LRU_FILTERS);
			Iterator iter= fLRUFilterIdsStack.iterator();
			while (iter.hasNext()) {
				String id= (String)iter.next();
				IMemento child= lruFilters.createChild(TAG_CHILD);
				child.putString(TAG_FILTER_ID, id);
			}
		}
	}

	private void saveUserDefinedPatterns(IMemento memento) {
		if(fUserDefinedPatterns != null && fUserDefinedPatterns.length > 0) {
			IMemento userDefinedPatterns= memento.createChild(TAG_USER_DEFINED_PATTERNS);
			for (int i= 0; i < fUserDefinedPatterns.length; i++) {
				IMemento child= userDefinedPatterns.createChild(TAG_CHILD);
				child.putString(TAG_PATTERN, fUserDefinedPatterns[i]);
			}
		}
	}

	/**
	 * Restores the state of the filter actions from a memento.
	 * <p>
	 * Note: This method does not refresh the viewer.
	 * </p>
	 * 
	 * @param memento the memento from which the state is restored
	 */	
	public void restoreState(IMemento memento) {
		if (memento == null)
			return;
		IMemento customFilters= memento.getChild(TAG_CUSTOM_FILTERS);
		if (customFilters == null)
			return;
		String userDefinedPatternsEnabled= customFilters.getString(TAG_USER_DEFINED_PATTERNS_ENABLED);
		if (userDefinedPatternsEnabled == null)
			return;

		fUserDefinedPatternsEnabled= Boolean.valueOf(userDefinedPatternsEnabled).booleanValue();
		restoreUserDefinedPatterns(customFilters);
		restoreXmlDefinedFilters(customFilters);
		restoreLRUFilters(customFilters);
		
		updateViewerFilters(false);
	}

	private void restoreUserDefinedPatterns(IMemento memento) {
		IMemento userDefinedPatterns= memento.getChild(TAG_USER_DEFINED_PATTERNS);
		if(userDefinedPatterns != null) {	
			IMemento children[]= userDefinedPatterns.getChildren(TAG_CHILD);
			String[] patterns= new String[children.length];
			for (int i = 0; i < children.length; i++)
				patterns[i]= children[i].getString(TAG_PATTERN);

			setUserDefinedPatterns(patterns);
		} else
			setUserDefinedPatterns(new String[0]);
	}

	private void restoreXmlDefinedFilters(IMemento memento) {
		IMemento xmlDefinedFilters= memento.getChild(TAG_XML_DEFINED_FILTERS);
		if(xmlDefinedFilters != null) {
			IMemento[] children= xmlDefinedFilters.getChildren(TAG_CHILD);
			for (int i= 0; i < children.length; i++) {
				String id= children[i].getString(TAG_FILTER_ID);
				Boolean isEnabled= new Boolean(children[i].getString(TAG_IS_ENABLED));
				fEnabledFilterIds.put(id, isEnabled);
			}
		}
	}

	private void restoreLRUFilters(IMemento memento) {
		IMemento lruFilters= memento.getChild(TAG_LRU_FILTERS);
		fLRUFilterIdsStack.clear();
		if(lruFilters != null) {
			IMemento[] children= lruFilters.getChildren(TAG_CHILD);
			for (int i= 0; i < children.length; i++) {
				String id= children[i].getString(TAG_FILTER_ID);
				if (fFilterDescriptorMap.containsKey(id) && !fLRUFilterIdsStack.contains(id))
					fLRUFilterIdsStack.push(id);
			}
		}
	}

	private void cleanUpPatternDuplicates() {
		if (!areUserDefinedPatternsEnabled())
			return;
		List userDefinedPatterns= new ArrayList(Arrays.asList(fUserDefinedPatterns));
		FilterDescriptor[] filters= getCachedFilterDescriptors();

		for (int i= 0; i < filters.length; i++) {
			if (filters[i].isPatternFilter()) {
				String pattern= filters[i].getPattern();
				if (userDefinedPatterns.contains(pattern)) {
					fEnabledFilterIds.put(filters[i].getId(), Boolean.TRUE);
					boolean hasMore= true;
					while (hasMore)
						hasMore= userDefinedPatterns.remove(pattern);
				}
			}
		}
		fUserDefinedPatterns= (String[])userDefinedPatterns.toArray(new String[userDefinedPatterns.size()]);
		setUserDefinedPatternsEnabled(fUserDefinedPatternsEnabled && fUserDefinedPatterns.length > 0);
	}

	private FilterDescriptor[] getCachedFilterDescriptors() {
		if (fCachedFilterDescriptors == null)
			fCachedFilterDescriptors= PydevViewerFilterGenerator.getBuiltinViewrFilter();
		return fCachedFilterDescriptors;
	}
	
	// ---------- dialog related code ----------

	private void openDialog() {
		CustomFiltersDialog dialog= new CustomFiltersDialog(
			fViewer.getControl().getShell(),
			fTargetId,
			areUserDefinedPatternsEnabled(),
			fUserDefinedPatterns,
			internalGetEnabledFilterIds());
		
		if (dialog.open() == Window.OK) {
			setEnabledFilterIds(dialog.getEnabledFilterIds());
			setUserDefinedPatternsEnabled(dialog.areUserDefinedPatternsEnabled());
			setUserDefinedPatterns(dialog.getUserDefinedPatterns());
			setRecentlyChangedFilters(dialog.getFilterDescriptorChangeHistory());

			storeViewDefaults();

			updateViewerFilters(true);
		}
	}
}
