package com.python.pydev.analysis.search_index;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.views.navigator.NavigatorDragAdapter;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;

import com.python.pydev.analysis.search.ICustomLineElement;
import com.python.pydev.analysis.search.ICustomMatch;
import com.python.pydev.analysis.search.SearchMessages;
import com.python.pydev.analysis.search.replace.ReplaceAction;

/**
 * Based on org.eclipse.search.internal.ui.text.FileSearchPage
 *
 * Still a work in progress!!
 *
 * Missing replace
 * Show line matches when viewing in table
 * Filtering through this UI without requiring a new search
 */
public class SearchIndexResultPage extends AbstractTextSearchViewPage {

    public SearchIndexResultPage() {
        super(FLAG_LAYOUT_TREE);
        ImageCache imageCache = SharedUiPlugin.getImageCache();

        fGroupByActions = new Action[] {
                new GroupByAction(this, SearchIndexTreeContentProvider.GROUP_WITH_PROJECT,
                        imageCache.getDescriptor(UIConstants.PROJECT_ICON), "Group: Projects"),

                new GroupByAction(this, SearchIndexTreeContentProvider.GROUP_WITH_MODULES,
                        imageCache.getDescriptor(UIConstants.FOLDER_PACKAGE_ICON), "Group: Packages"),

                new GroupByAction(this, SearchIndexTreeContentProvider.GROUP_WITH_FOLDERS,
                        imageCache.getDescriptor(UIConstants.FOLDER_ICON), "Group: Folders"),

        };
    }

    private int groupWithConfiguration = SearchIndexTreeContentProvider.GROUP_WITH_PROJECT
            | SearchIndexTreeContentProvider.GROUP_WITH_MODULES;

    public int getGroupWithConfiguration() {
        return groupWithConfiguration;
    }

    public void setGroupWithConfiguration(int groupWithConfiguration) {
        this.groupWithConfiguration = groupWithConfiguration;
        updateGroupWith(getViewer());
    }

    private static String STORE_GROUP_WITH = "group_with";

    private final Action[] fGroupByActions;

    private ActionGroup fActionGroup;

    public static class DecoratorIgnoringViewerSorter extends ViewerComparator {
        private final ILabelProvider fLabelProvider;

        public DecoratorIgnoringViewerSorter(ILabelProvider labelProvider) {
            fLabelProvider = labelProvider;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ViewerComparator#category(java.lang.Object)
         */
        @Override
        public int category(Object element) {
            if (element instanceof IContainer) {
                return 1;
            }
            return 2;
        }

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            int cat1 = category(e1);
            int cat2 = category(e2);

            if (cat1 != cat2) {
                return cat1 - cat2;
            }

            if (e1 instanceof ICustomLineElement && e2 instanceof ICustomLineElement) {
                ICustomLineElement m1 = (ICustomLineElement) e1;
                ICustomLineElement m2 = (ICustomLineElement) e2;
                return m1.getOffset() - m2.getOffset();
            }

            String name1 = fLabelProvider.getText(e1);
            String name2 = fLabelProvider.getText(e2);
            if (name1 == null) {
                name1 = "";//$NON-NLS-1$
            }
            if (name2 == null) {
                name2 = "";//$NON-NLS-1$
            }
            return getComparator().compare(name1, name2);
        }
    }

    private static final String[] SHOW_IN_TARGETS = new String[] { IPageLayout.ID_RES_NAV };
    private static final IShowInTargetList SHOW_IN_TARGET_LIST = new IShowInTargetList() {
        public String[] getShowInTargetIds() {
            return SHOW_IN_TARGETS;
        }
    };

    private ISearchIndexContentProvider fContentProvider;
    private Text filterText;
    private WorkbenchJob refreshJob;

    @Override
    protected void elementsChanged(Object[] objects) {
        if (fContentProvider != null) {
            ViewerFilter[] filters = getViewer().getFilters();
            for (ViewerFilter viewerFilter : filters) {
                if (viewerFilter instanceof SearchResultsViewerFilter) {
                    SearchResultsViewerFilter searchResultsViewerFilter = (SearchResultsViewerFilter) viewerFilter;
                    searchResultsViewerFilter.clearCache();
                }
            }
            fContentProvider.elementsChanged(objects);
        }
    }

    @Override
    protected void clear() {
        if (fContentProvider != null) {
            fContentProvider.clear();
            Job r = this.refreshJob;
            if (r != null) {
                r.cancel();
            }
            filterText.setText("");
            getViewer().setFilters(new ViewerFilter[0]);
        }
    }

    @Override
    public Object getUIState() {
        return new Tuple<>(super.getUIState(), filterText.getText());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setInput(ISearchResult newSearch, Object viewState) {
        String filter = "";
        if (viewState instanceof Tuple) {
            Tuple<Object, Object> tuple = (Tuple<Object, Object>) viewState;
            filter = (String) tuple.o2;
            viewState = tuple.o1;
        }

        StructuredViewer viewer = getViewer();
        Control control = viewer.getControl();
        control.setRedraw(false);
        try {
            viewer.setFilters(new ViewerFilter[0]); //Reset the filter before setting the new selection
            try {
                super.setInput(newSearch, viewState);
            } catch (Exception e) {
                Log.log(e);
                super.setInput(newSearch, null);
            }
            filterText.setText(filter);
            textChanged();
        } finally {
            control.setRedraw(true);
        }
    }

    @Override
    protected void configureTreeViewer(TreeViewer viewer) {
        viewer.setUseHashlookup(true);
        SearchIndexLabelProvider innerLabelProvider = new SearchIndexLabelProvider(this);
        viewer.setLabelProvider(new DecoratingFileSearchLabelProvider(innerLabelProvider));
        viewer.setContentProvider(new SearchIndexTreeContentProvider(this, viewer));
        viewer.setComparator(new DecoratorIgnoringViewerSorter(innerLabelProvider));
        fContentProvider = (ISearchIndexContentProvider) viewer.getContentProvider();
        addDragAdapters(viewer);

        updateGroupWith(viewer);
    }

    @Override
    protected void configureTableViewer(TableViewer viewer) {
        throw new RuntimeException("Table layout is unsupported.");
    }

    @Override
    public StructuredViewer getViewer() {
        return super.getViewer();
    }

    @Override
    public void setElementLimit(Integer elementLimit) {
        super.setElementLimit(elementLimit);
    }

    private void addDragAdapters(StructuredViewer viewer) {
        Transfer[] transfers = new Transfer[] { ResourceTransfer.getInstance() };
        int ops = DND.DROP_COPY | DND.DROP_LINK;
        viewer.addDragSupport(ops, transfers, new NavigatorDragAdapter(viewer));
    }

    @Override
    protected void showMatch(Match match, int offset, int length, boolean activate) throws PartInitException {
        IFile file = (IFile) match.getElement();
        IWorkbenchPage page = getSite().getPage();
        if (offset >= 0 && length != 0) {
            openAndSelect(page, file, offset, length, activate);
        } else {
            open(page, file, activate);
        }
    }

    @Override
    protected void handleOpen(OpenEvent event) {
        Object firstElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
        if (getDisplayedMatchCount(firstElement) == 0) {
            try {
                if (firstElement instanceof IAdaptable) {
                    IAdaptable iAdaptable = (IAdaptable) firstElement;
                    IFile file = iAdaptable.getAdapter(IFile.class);
                    if (file != null) {
                        open(getSite().getPage(), file, false);
                    }
                }
            } catch (PartInitException e) {
                ErrorDialog.openError(getSite().getShell(),
                        "Open File",
                        "Opening the file failed.", e.getStatus());
            }
            return;
        }
        super.handleOpen(event);
    }

    @Override
    protected void fillContextMenu(IMenuManager mgr) {
        super.fillContextMenu(mgr);
        fActionGroup.setContext(new ActionContext(getSite().getSelectionProvider().getSelection()));
        fActionGroup.fillContextMenu(mgr);
        SearchIndexQuery query = (SearchIndexQuery) getInput().getQuery();
        if (query.getSearchString().length() > 0) {
            IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
            if (!selection.isEmpty()) {
                ReplaceAction replaceSelection = new ReplaceAction(getSite().getShell(), getInput(),
                        selection.toArray(), true);
                replaceSelection.setText(SearchMessages.ReplaceAction_label_selected);
                mgr.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, replaceSelection);

            }
            ICallback<Boolean, Match> skipMatch = new ICallback<Boolean, Match>() {

                @Override
                public Boolean call(Match match) {
                    StructuredViewer viewer = getViewer();
                    ViewerFilter[] filters = viewer.getFilters();
                    if (filters == null || filters.length == 0) {
                        return false;
                    }
                    for (ViewerFilter viewerFilter : filters) {
                        if (viewerFilter instanceof SearchResultsViewerFilter) {
                            SearchResultsViewerFilter searchResultsViewerFilter = (SearchResultsViewerFilter) viewerFilter;
                            if (searchResultsViewerFilter.isLeafMatch(viewer, match)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            };
            ReplaceAction replaceAll = new ReplaceAction(getSite().getShell(), getInput(), null, true, skipMatch);
            replaceAll.setText(SearchMessages.ReplaceAction_label_all);
            mgr.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, replaceAll);
        }
    }

    @Override
    public void setViewPart(ISearchResultViewPart part) {
        super.setViewPart(part);
        fActionGroup = new NewTextSearchActionGroup(part);
    }

    @Override
    protected void fillToolbar(IToolBarManager tbm) {
        super.fillToolbar(tbm);
        for (Action a : fGroupByActions) {
            String id = IContextMenuConstants.GROUP_PROPERTIES + "." + a.hashCode();
            a.setId(id);
            tbm.add(a);
        }

    }

    @Override
    public void dispose() {
        fActionGroup.dispose();
        if (this.filterText != null) {
            this.filterText.dispose();
            this.filterText = null;
        }
        if (refreshJob != null) {
            refreshJob.cancel();
            refreshJob = null;
        }
        super.dispose();
    }

    @Override
    public void restoreState(IMemento memento) {
        super.restoreState(memento);
        if (memento != null) {
            Integer value = memento.getInteger(STORE_GROUP_WITH);
            if (value != null) {
                groupWithConfiguration = value.intValue();
                updateGroupWith(this.getViewer());
            }
        }
    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
        memento.putInteger(STORE_GROUP_WITH, this.groupWithConfiguration);
    }

    private void updateGroupWith(StructuredViewer viewer) {
        if (viewer != null) {
            IContentProvider contentProvider = viewer.getContentProvider();
            if (contentProvider instanceof SearchIndexTreeContentProvider) {
                SearchIndexTreeContentProvider searchIndexTreeContentProvider = (SearchIndexTreeContentProvider) contentProvider;
                searchIndexTreeContentProvider.setGroupWith(groupWithConfiguration);
            }
        }
    }

    public Object getAdapter(Class<?> adapter) {
        if (IShowInTargetList.class.equals(adapter)) {
            return SHOW_IN_TARGET_LIST;
        }

        if (adapter == IShowInSource.class) {
            ISelectionProvider selectionProvider = getSite().getSelectionProvider();
            if (selectionProvider == null) {
                return null;
            }

            ISelection selection = selectionProvider.getSelection();
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection structuredSelection = ((StructuredSelection) selection);
                final Set<Object> newSelection = new HashSet<>(structuredSelection.size());
                Iterator<?> iter = structuredSelection.iterator();
                while (iter.hasNext()) {
                    Object element = iter.next();
                    if (element instanceof ICustomLineElement) {
                        element = ((ICustomLineElement) element).getParent();
                    }
                    newSelection.add(element);
                }

                return new IShowInSource() {
                    public ShowInContext getShowInContext() {
                        return new ShowInContext(null, new StructuredSelection(new ArrayList<>(newSelection)));
                    }
                };
            }
            return null;
        }

        return null;
    }

    @Override
    public String getLabel() {
        StructuredViewer viewer = getViewer();
        if (viewer instanceof TreeViewer) {
            int count = 0;
            TreeViewer tv = (TreeViewer) viewer;

            final AbstractTextSearchResult input = getInput();
            if (input != null) {
                ViewerFilter[] filters = tv.getFilters();
                if (filters != null && filters.length > 0) {
                    Object[] elements = input.getElements();
                    for (int j = 0; j < elements.length; j++) {
                        Object element = elements[j];
                        Match[] matches = input.getMatches(element);
                        for (Match match : matches) {
                            for (int i = 0; i < filters.length; i++) {
                                ViewerFilter vf = filters[i];
                                if (vf instanceof SearchResultsViewerFilter) {
                                    SearchResultsViewerFilter searchResultsViewerFilter = (SearchResultsViewerFilter) vf;
                                    if (searchResultsViewerFilter.isLeafMatch(viewer, match)) {
                                        count += 1;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // No active filters
                    count = input.getMatchCount();
                }
            }
            AbstractTextSearchResult result = getInput();
            if (result == null) {
                return "";
            }
            ISearchQuery query = result.getQuery();
            if (query instanceof SearchIndexQuery) {
                SearchIndexQuery searchIndexQuery = (SearchIndexQuery) query;
                return searchIndexQuery.getResultLabel(count);
            }
        }
        return super.getLabel();
    }

    @Override
    public int getDisplayedMatchCount(Object element) {
        if (element instanceof TreeNode<?>) {
            element = ((TreeNode<?>) element).data;
        }
        if (element instanceof ICustomLineElement) {
            ICustomLineElement lineEntry = (ICustomLineElement) element;
            return lineEntry.getNumberOfMatches(getInput());
        }
        return 0;
    }

    @Override
    public Match[] getDisplayedMatches(Object element) {
        if (element instanceof TreeNode<?>) {
            element = ((TreeNode<?>) element).data;
        }

        if (element instanceof CustomModule) {
            CustomModule customModule = (CustomModule) element;
            element = customModule.moduleLineElement;
        }

        if (element instanceof ICustomLineElement) {
            ICustomLineElement lineEntry = (ICustomLineElement) element;
            return lineEntry.getMatches(getInput());
        }
        return new Match[0];
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void evaluateChangedElements(Match[] matches, Set changedElements) {
        for (int i = 0; i < matches.length; i++) {
            changedElements.add(((ICustomMatch) matches[i]).getLineElement());
        }
    }

    @Override
    protected TreeViewer createTreeViewer(Composite parent) {
        createFilterControl(parent);
        TreeViewer ret = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL) {
            @Override
            public void expandAll() {
                Control control = this.getControl();
                control.setRedraw(false);
                try {
                    super.expandAll();
                } finally {
                    control.setRedraw(true);
                }
            }

            @Override
            public void collapseAll() {
                Control control = this.getControl();
                control.setRedraw(false);
                try {
                    super.collapseAll();
                } finally {
                    control.setRedraw(true);
                }
            }
        };
        fixViewerLayout(ret.getControl());
        return ret;
    }

    @Override
    protected TableViewer createTableViewer(Composite parent) {
        createFilterControl(parent);
        TableViewer ret = super.createTableViewer(parent);
        fixViewerLayout(ret.getControl());
        return ret;
    }

    private void fixViewerLayout(Control control) {
        GridData layoutData = new GridData(GridData.FILL_BOTH);
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalSpan = 3;
        control.setLayoutData(layoutData);
    }

    private void createFilterControl(Composite parent) {
        GridLayout layout = new GridLayout(3, false);
        parent.setLayout(layout);

        Label label = new Label(parent, SWT.NONE);
        label.setText("Exclude modules");

        filterText = new Text(parent, SWT.BORDER | SWT.SINGLE);
        GridData layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
        filterText.setLayoutData(layoutData);
        filterText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                textChanged();
            }
        });

        label = new Label(parent, SWT.NONE);
        label.setText("(comma-separated, * = any string, ? = any char)");
    }

    protected void textChanged() {
        if (refreshJob != null) {
            refreshJob.cancel();
        }
        getRefreshJob().schedule(200);
    }

    private WorkbenchJob getRefreshJob() {
        if (refreshJob == null) {
            refreshJob = new WorkbenchJob("Refresh Filter") {//$NON-NLS-1$
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    if (filterText != null && !filterText.isDisposed()) {
                        final String text = filterText.getText().trim();
                        AbstractTextSearchResult input = getInput();
                        if (input != null) {
                            if (!text.isEmpty()) {
                                ViewerFilter[] filters = new ViewerFilter[] {
                                        new SearchResultsViewerFilter(text)
                                };
                                getViewer().setFilters(filters);
                                TreeViewer viewer = (TreeViewer) getViewer();
                                viewer.expandAll();
                            } else {
                                getViewer().setFilters(new ViewerFilter[0]);
                            }
                        }
                    }
                    getViewPart().updateLabel();
                    return Status.OK_STATUS;
                }
            };
            refreshJob.setSystem(true);
        }
        return refreshJob;
    }

}
