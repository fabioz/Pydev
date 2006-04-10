package com.python.pydev.ui.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.internal.core.SearchScope;
import org.eclipse.search.internal.ui.ISearchHelpContextIds;
import org.eclipse.search.internal.ui.Messages;
import org.eclipse.search.internal.ui.ScopePart;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

public class PySearchPage extends DialogPage implements ISearchPage{

	private static final int HISTORY_SIZE= 12;
	public static final String EXTENSION_POINT_ID= "com.python.pydev.ui.search.pySearchPage"; 

	// Dialog store id constants
	private static final String PAGE_NAME= "PydevSearchPage"; 
	private static final String STORE_CASE_SENSITIVE= EXTENSION_POINT_ID+"CASE_SENSITIVE"; 
	private static final String STORE_SEARCH_DERIVED = EXTENSION_POINT_ID+"SEARCH_DERIVED"; 
	private static final String STORE_HISTORY= EXTENSION_POINT_ID+"HISTORY"; 
	private static final String STORE_HISTORY_SIZE= EXTENSION_POINT_ID+"HISTORY_SIZE"; 

	private List<SearchPatternData> fPreviousSearchPatterns= new ArrayList<SearchPatternData>(20);

	private IDialogSettings fDialogSettings;
	private boolean fFirstTime= true;
	private boolean fIsCaseSensitive;
	private boolean fSearchDerived;
	
	private Combo fPattern;
	private Button fIgnoreCase;
	private CLabel fStatusLabel;

	private ISearchPageContainer fContainer;

	private static class SearchPatternData {
		public final boolean ignoreCase;
		public final String textPattern;
		public final int scope;
		public final IWorkingSet[] workingSets;
		
		public SearchPatternData(String textPattern, boolean ignoreCase, int scope, IWorkingSet[] workingSets) {
			this.ignoreCase= ignoreCase;
			this.textPattern= textPattern;
			this.scope= scope;
			this.workingSets= workingSets; // can be null
		}
		
		public void store(IDialogSettings settings) {
			settings.put("ignoreCase", ignoreCase); //$NON-NLS-1$
			settings.put("textPattern", textPattern); //$NON-NLS-1$
			settings.put("scope", scope); //$NON-NLS-1$ //$NON-NLS-2$
			if (workingSets != null) {
				String[] wsIds= new String[workingSets.length];
				for (int i= 0; i < workingSets.length; i++) {
					wsIds[i]= workingSets[i].getId();
				}
				settings.put("workingSets", wsIds); //$NON-NLS-1$
			} else {
				settings.put("workingSets", new String[0]); //$NON-NLS-1$
			}

		}
		
		public static SearchPatternData create(IDialogSettings settings) {
			String textPattern= settings.get("textPattern"); //$NON-NLS-1$
			String[] wsIds= settings.getArray("workingSets"); //$NON-NLS-1$
			IWorkingSet[] workingSets= null;
			if (wsIds != null && wsIds.length > 0) {
				IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
				workingSets= new IWorkingSet[wsIds.length];
				for (int i= 0; workingSets != null && i < wsIds.length; i++) {
					workingSets[i]= workingSetManager.getWorkingSet(wsIds[i]);
					if (workingSets[i] == null) {
						workingSets= null;
					}
				}
			}
			try {
				int scope= settings.getInt("scope"); //$NON-NLS-1$
				boolean ignoreCase= settings.getBoolean("ignoreCase"); //$NON-NLS-1$

				return	new SearchPatternData(textPattern, ignoreCase, scope, workingSets);
			} catch (NumberFormatException e) {
				return null;
			}
		}

	}
	//---- Action Handling ------------------------------------------------
	
	public boolean performAction() {
		NewSearchUI.runQueryInBackground(getSearchQuery());
		return true;
	}
	

	private ISearchQuery getSearchQuery() {
		
		SearchPatternData patternData= getPatternData();
	
		// Setup search scope
		SearchScope scope= null;
		switch (getContainer().getSelectedScope()) {
			case ISearchPageContainer.WORKSPACE_SCOPE:
				scope= SearchScope.newWorkspaceScope();
				break;
			case ISearchPageContainer.SELECTION_SCOPE:
				scope= getSelectedResourcesScope(false);
				break;
			case ISearchPageContainer.SELECTED_PROJECTS_SCOPE:
				scope= getSelectedResourcesScope(true);
				break;
			case ISearchPageContainer.WORKING_SET_SCOPE:
				IWorkingSet[] workingSets= getContainer().getSelectedWorkingSets();
				String desc= Messages.format(SearchMessages.WorkingSetScope, ScopePart.toString(workingSets)); 
				scope= SearchScope.newSearchScope(desc, workingSets);
		}		
		scope.addFileNamePattern("*.py");
		scope.addFileNamePattern("*.pyw");
		NewSearchUI.activateSearchResultView();
		return new FileSearchQuery(scope,  getSearchOptions(), patternData.textPattern, fSearchDerived);
	}

	private String getPattern() {
		return fPattern.getText();
	}
	
	private SearchPatternData findInPrevious(String pattern) {
		for (Iterator iter= fPreviousSearchPatterns.iterator(); iter.hasNext();) {
			SearchPatternData element= (SearchPatternData) iter.next();
			if (pattern.equals(element.textPattern)) {
				return element;
			}
		}
		return null;
	}

	/**
	 * Return search pattern data and update previous searches.
	 * An existing entry will be updated.
	 * @return the search pattern data
	 */
	private SearchPatternData getPatternData() {
		SearchPatternData match= findInPrevious(fPattern.getText());
		if (match != null) {
			fPreviousSearchPatterns.remove(match);
		}
		match= new SearchPatternData(
					getPattern(),
					ignoreCase(),
					getContainer().getSelectedScope(),
					getContainer().getSelectedWorkingSets());
		fPreviousSearchPatterns.add(0, match);
		return match;
	}

	private String[] getPreviousSearchPatterns() {
		int size= fPreviousSearchPatterns.size();
		String [] patterns= new String[size];
		for (int i= 0; i < size; i++)
			patterns[i]= ((SearchPatternData) fPreviousSearchPatterns.get(i)).textPattern;
		return patterns;
	}
	
	private String getSearchOptions() {
		StringBuffer result= new StringBuffer();
		if (!ignoreCase())
			result.append('i');

		return result.toString();	
	}
	
	private boolean ignoreCase() {
		return fIgnoreCase.getSelection();
	}

	/*
	 * Implements method from IDialogPage
	 */
	public void setVisible(boolean visible) {
		if (visible && fPattern != null) {
			if (fFirstTime) {
				fFirstTime= false;
				// Set item and text here to prevent page from resizing
				fPattern.setItems(getPreviousSearchPatterns());
				if (!initializePatternControl()) {
					fPattern.select(0);
					handleWidgetSelected();
				}
			}
			fPattern.setFocus();
		}
		updateOKStatus();
		super.setVisible(visible);
	}
	
	final void updateOKStatus() {
		getContainer().setPerformActionEnabled(true);
	}

	//---- Widget creation ------------------------------------------------

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		readConfiguration();
		
		Composite result= new Composite(parent, SWT.NONE);
		result.setFont(parent.getFont());
		GridLayout layout= new GridLayout(2, false);
		result.setLayout(layout);
		
		addTextPatternControls(result);
		
		Label separator= new Label(result, SWT.NONE);
		separator.setVisible(false);
		GridData data= new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1);
		data.heightHint= convertHeightInCharsToPixels(1) / 3;
		separator.setLayoutData(data);
		
		setControl(result);
		Dialog.applyDialogFont(result);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(result, ISearchHelpContextIds.TEXT_SEARCH_PAGE);
	}
	

	private void addTextPatternControls(Composite group) {
		// grid layout with 2 columns

		// Info text		
		Label label= new Label(group, SWT.LEAD);
		label.setText(SearchMessages.SearchPage_containingText_text); 
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		label.setFont(group.getFont());

		// Pattern combo
		fPattern= new Combo(group, SWT.SINGLE | SWT.BORDER);
		// Not done here to prevent page from resizing
		// fPattern.setItems(getPreviousSearchPatterns());
		fPattern.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected();
				updateOKStatus();
			}
		});
		// add some listeners for regex syntax checking
		fPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateOKStatus();
			}
		});
		fPattern.setFont(group.getFont());
		GridData data= new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
		data.widthHint= convertWidthInCharsToPixels(50);
		fPattern.setLayoutData(data);
		
		fIgnoreCase= new Button(group, SWT.CHECK);
		fIgnoreCase.setText(SearchMessages.SearchPage_caseSensitive); 
		fIgnoreCase.setSelection(!fIsCaseSensitive);
		fIgnoreCase.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fIsCaseSensitive= !fIgnoreCase.getSelection();
			}
		});
		fIgnoreCase.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		fIgnoreCase.setFont(group.getFont());

		// Text line which explains the special characters
		fStatusLabel= new CLabel(group, SWT.LEAD);
		fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fStatusLabel.setFont(group.getFont());
		fStatusLabel.setAlignment(SWT.LEFT);
		fStatusLabel.setText(SearchMessages.SearchPage_containingText_hint); 

	}

	private void handleWidgetSelected() {
		int selectionIndex= fPattern.getSelectionIndex();
		if (selectionIndex < 0 || selectionIndex >= fPreviousSearchPatterns.size())
			return;
		
		SearchPatternData patternData= (SearchPatternData) fPreviousSearchPatterns.get(selectionIndex);
		if (!fPattern.getText().equals(patternData.textPattern))
			return;
		fIgnoreCase.setSelection(patternData.ignoreCase);
		fPattern.setText(patternData.textPattern);
		if (patternData.workingSets != null)
			getContainer().setSelectedWorkingSets(patternData.workingSets);
		else
			getContainer().setSelectedScope(patternData.scope);
	}

	private boolean initializePatternControl() {
		ISelection selection= getSelection();
		if (selection instanceof ITextSelection && !selection.isEmpty()) {
			String text= ((ITextSelection) selection).getText();
			fPattern.setText(insertEscapeChars(text));
			return true;
		}
		return false;
	}
	
	private String insertEscapeChars(String text) {
		if (text == null || text.equals("")) //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		StringBuffer sbIn= new StringBuffer(text);
		BufferedReader reader= new BufferedReader(new StringReader(text));
		int lengthOfFirstLine= 0;
		try {
			lengthOfFirstLine= reader.readLine().length();
		} catch (IOException ex) {
			return ""; //$NON-NLS-1$
		}
		StringBuffer sbOut= new StringBuffer(lengthOfFirstLine + 5);
		int i= 0;
		while (i < lengthOfFirstLine) {
			char ch= sbIn.charAt(i);
			if (ch == '*' || ch == '?' || ch == '\\')
				sbOut.append("\\"); //$NON-NLS-1$
			sbOut.append(ch);
			i++;
		}
		return sbOut.toString();
	}



	/**
	 * Sets the search page's container.
	 * @param container the container to set
	 */
	public void setContainer(ISearchPageContainer container) {
		fContainer= container;
	}
	
	private ISearchPageContainer getContainer() {
		return fContainer;
	}
	
	private ISelection getSelection() {
		return fContainer.getSelection();
	}

	private SearchScope getSelectedResourcesScope(boolean isProjectScope) {
		HashSet<IResource> resources= new HashSet<IResource>();
		String firstProjectName= null;
		ISelection selection= getSelection();
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator iter= ((IStructuredSelection) selection).iterator();
			while (iter.hasNext()) {
				Object curr= iter.next();

				IResource resource= null;			
				if (curr instanceof IResource) {
					resource= (IResource) curr;
				} else if (curr instanceof IAdaptable) {
					resource= (IResource) ((IAdaptable)curr).getAdapter(IResource.class);
					if (resource == null && isProjectScope)
						resource= (IProject) ((IAdaptable)curr).getAdapter(IProject.class);
				}
				if (resource != null) {
					if (isProjectScope) {
						resource= resource.getProject();
						if (firstProjectName == null) {
							firstProjectName= resource.getName();
						}
					}
					resources.add(resource);
				}
			}
		}
		if (resources.isEmpty() && isProjectScope) {
			IProject editorProject= getEditorProject();
			if (editorProject != null) {
				resources.add(editorProject);
				firstProjectName= editorProject.getName();
			}
		}

		String name;
		if (isProjectScope) {
			int elementCount= resources.size();
			if (elementCount > 1)
				name= Messages.format(SearchMessages.EnclosingProjectsScope, firstProjectName); 
			else if (elementCount == 1)
				name= Messages.format(SearchMessages.EnclosingProjectScope, firstProjectName); 
			else 
				name= Messages.format(SearchMessages.EnclosingProjectScope, "");  //$NON-NLS-1$
		} else {
			name= SearchMessages.SelectionScope; 
		}
		IResource[] arr= (IResource[]) resources.toArray(new IResource[resources.size()]);
		return SearchScope.newSearchScope(name, arr);
	}

	private IProject getEditorProject() {
		IWorkbenchPart activePart= SearchPlugin.getActivePage().getActivePart();
		if (activePart instanceof IEditorPart) {
			IEditorPart editor= (IEditorPart) activePart;
			IEditorInput input= editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				return ((IFileEditorInput)input).getFile().getProject();
			}
		}
		return null;
	}
	//--------------- Configuration handling --------------
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		writeConfiguration();
		super.dispose();
	}
	
	/**
	 * Returns the page settings for this Text search page.
	 * 
	 * @return the page settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		IDialogSettings settings= SearchPlugin.getDefault().getDialogSettings();
		fDialogSettings= settings.getSection(PAGE_NAME);
		if (fDialogSettings == null)
			fDialogSettings= settings.addNewSection(PAGE_NAME);
		return fDialogSettings;
	}
		
	
	/**
	 * Initializes itself from the stored page settings.
	 */
	private void readConfiguration() {
		IDialogSettings s= getDialogSettings();
		fIsCaseSensitive= s.getBoolean(STORE_CASE_SENSITIVE);
		fSearchDerived= s.getBoolean(STORE_SEARCH_DERIVED);
		
		try {
			int historySize= s.getInt(STORE_HISTORY_SIZE);
			for (int i= 0; i < historySize; i++) {
				IDialogSettings histSettings= s.getSection(STORE_HISTORY + i);
				if (histSettings != null) {
					SearchPatternData data= SearchPatternData.create(histSettings);
					if (data != null) {
						fPreviousSearchPatterns.add(data);
					}
				}
			}
		} catch (NumberFormatException e) {
			// ignore
		}
	}
	
	/**
	 * Stores it current configuration in the dialog store.
	 */
	private void writeConfiguration() {
		IDialogSettings s= getDialogSettings();
		s.put(STORE_CASE_SENSITIVE, fIsCaseSensitive);
		s.put(STORE_SEARCH_DERIVED, fSearchDerived);
		
		int historySize= Math.min(fPreviousSearchPatterns.size(), HISTORY_SIZE);
		s.put(STORE_HISTORY_SIZE, historySize);
		for (int i= 0; i < historySize; i++) {
			IDialogSettings histSettings= s.addNewSection(STORE_HISTORY + i);
			SearchPatternData data= ((SearchPatternData) fPreviousSearchPatterns.get(i));
			data.store(histSettings);
		}
	}
	
	private void statusMessage(boolean error, String message) {
		fStatusLabel.setText(message);
		if (error)
			fStatusLabel.setForeground(JFaceColors.getErrorText(fStatusLabel.getDisplay()));
		else
			fStatusLabel.setForeground(null);
	}
}
