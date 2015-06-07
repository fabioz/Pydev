package com.python.pydev.analysis.search_index;

import java.text.MessageFormat;
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
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
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
import org.python.pydev.shared_core.structure.TreeNode;

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
        setElementLimit(new Integer(DEFAULT_ELEMENT_LIMIT));
    }

    private static final String KEY_LIMIT = "org.eclipse.search.resultpage.limit"; //$NON-NLS-1$

    private static final int DEFAULT_ELEMENT_LIMIT = 1000;

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
            fContentProvider.elementsChanged(objects);
        }
    }

    @Override
    protected void clear() {
        if (fContentProvider != null) {
            fContentProvider.clear();
            getViewer().setFilters(new ViewerFilter[0]);
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
        int limit = elementLimit.intValue();
        getSettings().put(KEY_LIMIT, limit);
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
            ReplaceAction replaceAll = new ReplaceAction(getSite().getShell(), getInput(), null, true);
            replaceAll.setText(SearchMessages.ReplaceAction_label_all);
            mgr.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, replaceAll);
        }
    }

    @Override
    public void setViewPart(ISearchResultViewPart part) {
        super.setViewPart(part);
        fActionGroup = new NewTextSearchActionGroup(part);
    }

    // @Override
    // public void init(IPageSite site) {
    //     super.init(site);
    //     IMenuManager menuManager = site.getActionBars().getMenuManager();
    //     menuManager.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES, new OpenSearchPreferencesAction());
    // }

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
        int elementLimit = DEFAULT_ELEMENT_LIMIT;
        try {
            elementLimit = getSettings().getInt(KEY_LIMIT);
        } catch (NumberFormatException e) {
        }
        if (memento != null) {
            Integer value = memento.getInteger(KEY_LIMIT);
            if (value != null) {
                elementLimit = value.intValue();
            }
        }
        setElementLimit(new Integer(elementLimit));
    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
        memento.putInteger(KEY_LIMIT, getElementLimit().intValue());
    }

    public Object getAdapter(Class adapter) {
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
                final Set newSelection = new HashSet(structuredSelection.size());
                Iterator iter = structuredSelection.iterator();
                while (iter.hasNext()) {
                    Object element = iter.next();
                    if (element instanceof ICustomLineElement) {
                        element = ((ICustomLineElement) element).getParent();
                    }
                    newSelection.add(element);
                }

                return new IShowInSource() {
                    public ShowInContext getShowInContext() {
                        return new ShowInContext(null, new StructuredSelection(new ArrayList(newSelection)));
                    }
                };
            }
            return null;
        }

        return null;
    }

    @Override
    public String getLabel() {
        String label = super.getLabel();
        StructuredViewer viewer = getViewer();
        if (viewer instanceof TableViewer) {
            TableViewer tv = (TableViewer) viewer;

            AbstractTextSearchResult result = getInput();
            if (result != null) {
                int itemCount = ((IStructuredContentProvider) tv.getContentProvider()).getElements(getInput()).length;
                int matchCount = getInput().getMatchCount();
                if (itemCount < matchCount) {
                    return MessageFormat.format("{0} (showing {1} of {2} matches)",
                            new Object[] { label, new Integer(itemCount), new Integer(matchCount) });
                }
            }
        }
        return label;
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
        TreeViewer ret = super.createTreeViewer(parent);
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
                        final String text = filterText.getText();
                        AbstractTextSearchResult input = getInput();
                        if (input != null) {
                            ViewerFilter[] filters = new ViewerFilter[] {
                                    new SearchResultsViewerFilter(text)
                            };
                            getViewer().setFilters(filters);
                        }
                    }
                    return Status.OK_STATUS;
                }
            };
            refreshJob.setSystem(true);
        }
        return refreshJob;
    }

}
