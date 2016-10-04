/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package com.python.pydev.analysis.actions;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.SortedMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ResourceWorkingSetFilter;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.actions.WorkingSetFilterActionGroup;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.statushandlers.StatusManager;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackWithListeners;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.utils.IViewWithControls;
import org.python.pydev.ui.NotifyViewCreated;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;
import com.python.pydev.analysis.additionalinfo.InfoFactory;
import com.python.pydev.analysis.additionalinfo.ModInfo;

/**
 * Let us choose from a list of IInfo (and the related additional info)
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GlobalsTwoPanelElementSelector2 extends FilteredItemsSelectionDialog implements IViewWithControls {

    private static final String DIALOG_SETTINGS = "com.python.pydev.analysis.actions.GlobalsTwoPanelElementSelector2"; //$NON-NLS-1$

    private static final String WORKINGS_SET_SETTINGS = "WorkingSet"; //$NON-NLS-1$

    private WorkingSetFilterActionGroup workingSetFilterActionGroup;

    private CustomWorkingSetFilter workingSetFilter = new CustomWorkingSetFilter();

    private String title;

    private List<AbstractAdditionalTokensInfo> additionalInfo;

    private String selectedText;

    public final ICallbackWithListeners onControlCreated = new CallbackWithListeners();
    public final ICallbackWithListeners onControlDisposed = new CallbackWithListeners();

    private List createdCallbacksForControls;

    public GlobalsTwoPanelElementSelector2(Shell shell, boolean multi, String selectedText) {
        super(shell, multi);
        this.selectedText = selectedText;

        setSelectionHistory(new InfoSelectionHistory());

        setTitle("PyDev: Globals Browser");
        setMessage(
                "Matching: ? = any char    * = any str    CamelCase (TC=TestCase)    Space in the end = exact match.\n"
                        + "Dotted names may be used to filter with package (e.g.: django.utils.In or just dj.ut.in)");

        NameIInfoLabelProvider resourceItemLabelProvider = new NameIInfoStyledLabelProvider(true);

        ModuleIInfoLabelProvider resourceItemDetailsLabelProvider = new ModuleIInfoLabelProvider();

        setListLabelProvider(resourceItemLabelProvider);
        setDetailsLabelProvider(resourceItemDetailsLabelProvider);
    }

    @Override
    protected void updateStatus(IStatus status) {
        super.updateStatus(status);
        PydevPlugin.fixSelectionStatusDialogStatusLineColor(this, this.getDialogArea()
                .getBackground());
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        this.title = title;
    }

    @Override
    public boolean isHelpAvailable() {
        return false;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control ret = super.createContents(parent);
        org.python.pydev.plugin.PydevPlugin.setCssId(parent, "py-globals-browser-dialog", true);
        return ret;
    }

    /**
     * Used to add the working set to the title.
     */
    private void setSubtitle(String text) {
        if (text == null || text.length() == 0) {
            getShell().setText(title);
        } else {
            getShell().setText(title + " - " + text); //$NON-NLS-1$
        }
    }

    @Override
    protected IDialogSettings getDialogSettings() {
        IDialogSettings settings = AnalysisPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

        if (settings == null) {
            settings = AnalysisPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
        }

        return settings;
    }

    @Override
    protected void storeDialog(IDialogSettings settings) {
        super.storeDialog(settings);

        XMLMemento memento = XMLMemento.createWriteRoot("workingSet"); //$NON-NLS-1$
        workingSetFilterActionGroup.saveState(memento);
        workingSetFilterActionGroup.dispose();
        StringWriter writer = new StringWriter();
        try {
            memento.save(writer);
            settings.put(WORKINGS_SET_SETTINGS, writer.getBuffer().toString());
        } catch (IOException e) {
            StatusManager.getManager().handle(
                    new Status(IStatus.ERROR, AnalysisPlugin.getPluginID(), IStatus.ERROR, "", e)); //$NON-NLS-1$
            // don't do anything. Simply don't store the settings
        }
    }

    @Override
    protected void restoreDialog(IDialogSettings settings) {
        super.restoreDialog(settings);

        String setting = settings.get(WORKINGS_SET_SETTINGS);
        if (setting != null) {
            try {
                IMemento memento = XMLMemento.createReadRoot(new StringReader(setting));
                workingSetFilterActionGroup.restoreState(memento);
            } catch (WorkbenchException e) {
                StatusManager.getManager().handle(
                        new Status(IStatus.ERROR, AnalysisPlugin.getPluginID(), IStatus.ERROR, "", e)); //$NON-NLS-1$
                // don't do anything. Simply don't restore the settings
            }
        }

        addListFilter(workingSetFilter);

        applyFilter();
    }

    /**
     * We need to add the action for the working set.
     */
    @Override
    protected void fillViewMenu(IMenuManager menuManager) {
        super.fillViewMenu(menuManager);

        workingSetFilterActionGroup = new WorkingSetFilterActionGroup(getShell(), new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                String property = event.getProperty();

                if (WorkingSetFilterActionGroup.CHANGE_WORKING_SET.equals(property)) {

                    IWorkingSet workingSet = (IWorkingSet) event.getNewValue();

                    if (workingSet != null && !(workingSet.isAggregateWorkingSet() && workingSet.isEmpty())) {
                        workingSetFilter.setWorkingSet(workingSet);
                        setSubtitle(workingSet.getLabel());
                    } else {
                        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

                        if (window != null) {
                            IWorkbenchPage page = window.getActivePage();
                            workingSet = page.getAggregateWorkingSet();

                            if (workingSet.isAggregateWorkingSet() && workingSet.isEmpty()) {
                                workingSet = null;
                            }
                        }

                        workingSetFilter.setWorkingSet(workingSet);
                        setSubtitle(null);
                    }

                    scheduleRefresh();
                }
            }
        });

        menuManager.add(new Separator());
        workingSetFilterActionGroup.fillContextMenu(menuManager);
    }

    @Override
    protected Control createExtendedContentArea(Composite parent) {
        return null;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Control ret = super.createDialogArea(parent);

        NotifyViewCreated.notifyViewCreated(this);
        createdCallbacksForControls = callRecursively(onControlCreated, parent, new ArrayList());

        return ret;
    }

    /**
     * Calls the callback with the composite c and all of its children (recursively).
     */
    private List callRecursively(ICallbackWithListeners callback, Composite c, ArrayList controls) {
        try {
            for (Control child : c.getChildren()) {
                if (child instanceof Composite) {
                    callRecursively(callback, (Composite) child, controls);
                }
                if (child instanceof Text || child instanceof Table) {
                    controls.add(child);
                    callback.call(child);
                }
            }
        } catch (Throwable e) {
            Log.log(e);
        }
        return controls;
    }

    @Override
    public Object[] getResult() {
        Object[] result = super.getResult();

        if (result == null) {
            return null;
        }

        List<AdditionalInfoAndIInfo> resultToReturn = new ArrayList<AdditionalInfoAndIInfo>();

        for (int i = 0; i < result.length; i++) {
            if (result[i] instanceof AdditionalInfoAndIInfo) {
                resultToReturn.add((AdditionalInfoAndIInfo) result[i]);
            }
        }

        return resultToReturn.toArray(new AdditionalInfoAndIInfo[resultToReturn.size()]);
    }

    /**
     * Overridden to set the initial pattern (if null we have an exception, so, it must at least be empty)
     */
    @Override
    public int open() {
        if (getInitialPattern() == null) {
            setInitialPattern(selectedText == null ? "" : selectedText);
        } else {
            setInitialPattern("");
        }
        int ret = super.open();
        if (this.createdCallbacksForControls != null) {
            for (Object o : this.createdCallbacksForControls) {
                onControlDisposed.call(o);
            }
            this.createdCallbacksForControls = null;
        }
        return ret;
    }

    @Override
    public String getElementName(Object item) {
        AdditionalInfoAndIInfo info = (AdditionalInfoAndIInfo) item;
        return info.info.getName();
    }

    @Override
    protected IStatus validateItem(Object item) {
        return Status.OK_STATUS;
    }

    @Override
    protected ItemsFilter createFilter() {
        return new InfoFilter();
    }

    /**
     * Sets the elements we should work on (must be set before open())
     */
    public void setElements(List<AbstractAdditionalTokensInfo> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    protected Comparator<AdditionalInfoAndIInfo> getItemsComparator() {
        return new Comparator<AdditionalInfoAndIInfo>() {

            /*
             * (non-Javadoc)
             * 
             * @see java.util.Comparator#compare(java.lang.Object,
             *      java.lang.Object)
             */
            @Override
            public int compare(AdditionalInfoAndIInfo resource1, AdditionalInfoAndIInfo resource2) {
                Collator collator = Collator.getInstance();
                String s1 = resource1.info.getName();
                String s2 = resource2.info.getName();
                int comparability = collator.compare(s1, s2);
                //same name
                if (comparability == 0) {
                    String p1 = resource1.info.getDeclaringModuleName();
                    String p2 = resource2.info.getDeclaringModuleName();
                    if (p1 == null && p2 == null) {
                        return 0;
                    }
                    if (p1 != null && p2 == null) {
                        return -1;
                    }
                    if (p1 == null && p2 != null) {
                        return 1;
                    }
                    return p1.compareTo(p2);
                }

                return comparability;
            }
        };
    }

    /**
     * This is the place where we put all the info in the content provider. Note that here we must add
     * ALL the info -- later, we'll filter it based on the active working set.
     */
    @Override
    protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter,
            IProgressMonitor progressMonitor) throws CoreException {
        if (itemsFilter instanceof InfoFilter) {
            if (progressMonitor != null) {
                progressMonitor.beginTask("Searching...", this.additionalInfo.size());
            }

            for (AbstractAdditionalTokensInfo additionalInfo : this.additionalInfo) {
                if (progressMonitor != null) {
                    if (progressMonitor.isCanceled()) {
                        return;
                    } else {
                        progressMonitor.worked(1);
                    }
                }
                Collection<IInfo> allTokens = new HashSet<IInfo>(additionalInfo.getAllTokens()); //no duplicates
                for (IInfo iInfo : allTokens) {
                    contentProvider.add(new AdditionalInfoAndIInfo(additionalInfo, iInfo), itemsFilter);
                }

                //Also show to the user the modules available as globals (2.2.3)
                IModulesManager modulesManager = null;
                try {
                    if (additionalInfo instanceof AdditionalProjectInterpreterInfo) {
                        AdditionalProjectInterpreterInfo projectInterpreterInfo = (AdditionalProjectInterpreterInfo) additionalInfo;
                        IProject project = projectInterpreterInfo.getProject();
                        PythonNature nature = PythonNature.getPythonNature(project);
                        if (nature != null) {
                            ICodeCompletionASTManager astManager = nature.getAstManager();
                            if (astManager != null) {
                                modulesManager = astManager.getModulesManager();
                            }

                        }
                    } else if (additionalInfo instanceof AdditionalSystemInterpreterInfo) {
                        AdditionalSystemInterpreterInfo systemInterpreterInfo = (AdditionalSystemInterpreterInfo) additionalInfo;
                        IInterpreterInfo defaultInterpreterInfo = systemInterpreterInfo.getManager()
                                .getDefaultInterpreterInfo(false);
                        modulesManager = defaultInterpreterInfo.getModulesManager();
                    }
                } catch (Throwable e) {
                    Log.log(e);
                }

                if (modulesManager != null) {
                    SortedMap<ModulesKey, ModulesKey> allDirectModulesStartingWith = modulesManager
                            .getAllDirectModulesStartingWith("");
                    Collection<ModulesKey> values = allDirectModulesStartingWith.values();
                    for (ModulesKey modulesKey : values) {
                        contentProvider.add(new AdditionalInfoAndIInfo(additionalInfo,
                                new ModInfo(modulesKey.name, modulesManager.getNature())),
                                itemsFilter);
                    }
                }
            }
        }

        if (progressMonitor != null) {
            progressMonitor.done();
        }

    }

    /**
     * Viewer filter which filters resources due to current working set
     */
    private class CustomWorkingSetFilter extends ViewerFilter {

        private ResourceWorkingSetFilter resourceWorkingSetFilter = new ResourceWorkingSetFilter();

        public void setWorkingSet(IWorkingSet workingSet) {
            resourceWorkingSetFilter.setWorkingSet(workingSet);
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (element instanceof AdditionalInfoAndIInfo) {
                AdditionalInfoAndIInfo info = (AdditionalInfoAndIInfo) element;
                if (info.additionalInfo instanceof AdditionalProjectInterpreterInfo) {
                    AdditionalProjectInterpreterInfo projectInterpreterInfo = (AdditionalProjectInterpreterInfo) info.additionalInfo;
                    return resourceWorkingSetFilter.select(viewer, parentElement, projectInterpreterInfo.getProject());
                }
            }
            return resourceWorkingSetFilter.select(viewer, parentElement, element);
        }
    }

    /**
     * Filters the info based on the pattern (considers each dot as a new scope in the pattern.)
     */
    protected class InfoFilter extends ItemsFilter {

        private String initialPattern;

        public InfoFilter() {
            super();
            //We have to get the actual text from the control, because the 
            Text pattern = (Text) getPatternControl();
            String stringPattern = ""; //$NON-NLS-1$
            if (pattern != null && !pattern.getText().equals("*")) { //$NON-NLS-1$
                stringPattern = pattern.getText();
            }
            this.initialPattern = stringPattern;
        }

        /**
         * Must have a valid name.
         */
        @Override
        public boolean isConsistentItem(Object item) {
            if (!(item instanceof AdditionalInfoAndIInfo)) {
                return false;
            }
            AdditionalInfoAndIInfo iInfo = (AdditionalInfoAndIInfo) item;
            if (iInfo.info.getName() != null) {
                return true;
            }
            return false;
        }

        /**
         * We must override it so that the results are properly updating according to the scopes in the pattern
         * (if we only returned false it'd also work, but it'd need to traverse all the items at each step).
         */
        @Override
        public boolean isSubFilter(ItemsFilter filter) {
            if (!(filter instanceof InfoFilter)) {
                return false;
            }

            return MatchHelper.isSubFilter(this.initialPattern, ((InfoFilter) filter).initialPattern);
        }

        /**
         * Override so that we consider scopes.
         */
        @Override
        public boolean equalsFilter(ItemsFilter filter) {
            if (!(filter instanceof InfoFilter)) {
                return false;
            }
            return MatchHelper.equalsFilter(this.initialPattern, ((InfoFilter) filter).initialPattern);
        }

        /**
         * Overridden to consider each dot as a new scope in the pattern (and match according to modules)
         */
        @Override
        public boolean matchItem(Object item) {
            if (!(item instanceof AdditionalInfoAndIInfo)) {
                return false;
            }
            AdditionalInfoAndIInfo info = (AdditionalInfoAndIInfo) item;
            return MatchHelper.matchItem(patternMatcher, info.info);
        }

    }

    /**
     * Used to store/restore the selections.
     */
    private class InfoSelectionHistory extends SelectionHistory {

        public InfoSelectionHistory() {
        }

        @Override
        protected Object restoreItemFromMemento(IMemento element) {
            InfoFactory infoFactory = new InfoFactory();
            AdditionalInfoAndIInfo resource = infoFactory.createElement(element);
            if (resource != null) {
                if (resource.additionalInfo instanceof AdditionalSystemInterpreterInfo) {
                    AdditionalInfoAndIInfo found = checkAdditionalInfo(resource, resource.info.getName(),
                            resource.additionalInfo);
                    if (found != null) {
                        return found;
                    }

                } else if (resource.additionalInfo instanceof AdditionalProjectInterpreterInfo) {
                    AdditionalProjectInterpreterInfo projectInterpreterInfo = (AdditionalProjectInterpreterInfo) resource.additionalInfo;
                    IProject project = projectInterpreterInfo.getProject();
                    if (project != null) {
                        List<IPythonNature> natures = new ArrayList<IPythonNature>();
                        PythonNature n = PythonNature.getPythonNature(project);
                        if (n != null) {
                            natures.add(n);
                            try {
                                List<Tuple<AbstractAdditionalTokensInfo, IPythonNature>> additionalInfoAndNature = AdditionalProjectInterpreterInfo
                                        .getAdditionalInfoAndNature(n, true, false);

                                for (Tuple<AbstractAdditionalTokensInfo, IPythonNature> tuple : additionalInfoAndNature) {
                                    AdditionalInfoAndIInfo found = checkAdditionalInfo(resource,
                                            resource.info.getName(), tuple.o1);
                                    if (found != null) {
                                        return found;
                                    }
                                }

                            } catch (Exception e) {
                                Log.log(e);
                            }
                        }
                    }

                }

                //                for(IPythonNature pythonNature:natures){
                //                    //Try to find in one of the natures... if we don't find it, return null, as that means
                //                    //it doesn't exist anymore!
                //                    ICodeCompletionASTManager astManager = pythonNature.getAstManager();
                //                    if(astManager == null){
                //                        continue;
                //                    }
                //                    List<ItemPointer> pointers = new ArrayList<ItemPointer>();
                //                    AnalysisPlugin.getDefinitionFromIInfo(pointers, astManager, pythonNature, resource.info, completionCache);
                //                    if(pointers.size() > 0){
                //                        return resource;
                //                    }
                //                }
            }

            return null;
        }

        private AdditionalInfoAndIInfo checkAdditionalInfo(AdditionalInfoAndIInfo resource, String name,
                AbstractAdditionalTokensInfo additionalInfoToSearch) {
            Collection<IInfo> tokensEqualTo = additionalInfoToSearch.getTokensEqualTo(name,
                    AbstractAdditionalTokensInfo.TOP_LEVEL | AbstractAdditionalTokensInfo.INNER);
            for (IInfo iInfo : tokensEqualTo) {
                if (iInfo.equals(resource.info)) {
                    return resource;
                }
            }
            return null;
        }

        @Override
        protected void storeItemToMemento(Object item, IMemento element) {
            AdditionalInfoAndIInfo resource = (AdditionalInfoAndIInfo) item;
            InfoFactory infoFactory = new InfoFactory(resource);
            infoFactory.saveState(element);
        }

    }

    @Override
    public ICallbackWithListeners getOnControlCreated() {
        return onControlCreated;
    }

    @Override
    public ICallbackWithListeners getOnControlDisposed() {
        return onControlDisposed;
    }

}
