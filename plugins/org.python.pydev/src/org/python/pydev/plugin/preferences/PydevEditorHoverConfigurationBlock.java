package org.python.pydev.plugin.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.editor.hover.DefaultPydevCombiningHover;
import org.python.pydev.editor.hover.PyEditorTextHoverDescriptor;
import org.python.pydev.editor.hover.PyHoverPreferencesPage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.StatusInfo;
import org.python.pydev.plugin.preferences.OverlayPreferenceStore.OverlayKey;
import org.python.pydev.utils.Messages;
import org.python.pydev.utils.PyEditorMessages;

/**
 * Configures Java Editor hover preferences.
 *
 * @since 2.1
 */
public class PydevEditorHoverConfigurationBlock implements IPreferenceConfigurationBlock {

    private static final String DELIMITER = PyEditorMessages.PyEditorHoverConfigurationBlock_delimiter;

    private static final int ENABLED_PROP = 0;
    private static final int MODIFIER_PROP = 1;
    private static final int PRIORITY_PROP = 2;
    private static final int PREEMPT_PROP = 3;

    // Data structure to hold the values which are edited by the user
    private static class HoverConfig {

        private String fId;
        private String fModifierString;
        private boolean fIsEnabled;
        private int fStateMask;
        private Integer fPriority;
        private boolean fPreempt;

        private HoverConfig(String id, String modifier, int stateMask, boolean enabled, int priority, boolean preempt) {
            fId = id;
            fModifierString = modifier;
            fIsEnabled = enabled;
            fStateMask = stateMask;
            fPriority = priority;
            fPreempt = preempt;
        }
    }

    private class PyEditorTextHoverDescriptorLabelProvider extends EmulatedNativeCheckBoxLabelProvider {

        private TableViewer viewer;

        public PyEditorTextHoverDescriptorLabelProvider(ColumnViewer viewer) {
            super(viewer);
            this.viewer = (TableViewer) viewer;
        }

        @Override
        public void addListener(ILabelProviderListener listener) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        @Override
        public void removeListener(ILabelProviderListener listener) {
        }

        @Override
        public void update(ViewerCell cell) {
            switch (cell.getColumnIndex()) {
                case ENABLED_PROP:
                    cell.setText(((PyEditorTextHoverDescriptor) cell.getElement()).getLabel());
                    break;
                case MODIFIER_PROP:
                    TableItem item = (TableItem) fHoverTableViewer.testFindItem(cell.getElement());
                    int index = fHoverTable.indexOf(item);
                    cell.setText(fHoverConfigs[convertIndex(index)].fModifierString);
                    break;
                case PRIORITY_PROP:
                    cell.setText(String.valueOf(((PyEditorTextHoverDescriptor) cell.getElement()).fPriority));
                    break;
                case PREEMPT_PROP:
                    cell.setImage(getImage(cell.getElement()));
                    break;
                default:
                    break;
            }

        }

        @Override
        protected boolean isChecked(Object element) {
            return ((PyEditorTextHoverDescriptor) element).fPreempt;
        }
    }

    private class PyEditorTextHoverDescriptorContentProvider implements IStructuredContentProvider {

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // Do nothing since the viewer listens to resource deltas
        }

        public void dispose() {
        }

        public Object[] getElements(Object element) {
            return (Object[]) element;
        }
    }

    private class HoverTableEditingSupport extends EditingSupport {

        private int column;
        CellEditor editor;
        private TableViewer viewer;

        public HoverTableEditingSupport(final TableViewer viewer, int column) {
            super(viewer);
            this.viewer = viewer;
            this.column = column;
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            if (this.column == PRIORITY_PROP) {
                this.editor = new TextCellEditor(this.viewer.getTable());
                editor.setValidator(new ICellEditorValidator() {

                    @Override
                    public String isValid(Object value) {
                        boolean valid = true;
                        if (!"".equals(value)) {
                            try {
                                int val = Integer.parseInt((String) value);
                                if (val < 0) {
                                    valid = false;
                                }
                            } catch (NumberFormatException | ClassCastException e) {
                                valid = false;
                            }
                        }
                        editor.getControl()
                                .setBackground(valid ? null : Display.getDefault().getSystemColor(SWT.COLOR_RED));
                        return (valid ? null : "non-negative integer required");
                    }
                });
                ((Text) ((TextCellEditor) editor).getControl()).selectAll();
            } else if (this.column == PREEMPT_PROP) {
                this.editor = new CheckboxCellEditor(viewer.getTable());
            }
            return this.editor;
        }

        @Override
        protected boolean canEdit(Object element) {
            return true;
        }

        @Override
        protected Object getValue(Object element) {
            PyEditorTextHoverDescriptor descr = (PyEditorTextHoverDescriptor) element;
            if (descr != null /*&& editor.isValueValid()*/) {
                switch (this.column) {
                    case PRIORITY_PROP:
                        return String.valueOf(descr.getPriority());
                    case PREEMPT_PROP:
                        return descr.isPreempt();
                    default:
                }

            }
            return "";
        }

        @Override
        protected void setValue(Object element, Object value) {
            switch (this.column) {
                case PRIORITY_PROP:
                    ((PyEditorTextHoverDescriptor) element).fPriority = Integer.parseInt((String) value);
                    handleSetPriority(Integer.parseInt((String) value));
                    break;
                case PREEMPT_PROP:
                    ((PyEditorTextHoverDescriptor) element).fPreempt = !((PyEditorTextHoverDescriptor) element).fPreempt;
                    handleSetPreempt((Boolean) value);
                    break;
                default:
            }
            this.editor.dispose();
        }

    }

    private OverlayPreferenceStore fStore;
    private HoverConfig[] fHoverConfigs;
    private Text fModifierEditor;
    private Table fHoverTable;
    private CheckboxTableViewer fHoverTableViewer;
    private TableColumn fNameColumn;
    private TableColumn fModifierColumn;
    private TableColumn fPriorityColumn;
    private TableColumn fPreemptColumn;
    private Text fDescription;

    private PreferencePage fMainPreferencePage;

    private StatusInfo fStatus;

    private class KeyModifierListener implements KeyListener {

        private Text editor;

        private KeyModifierListener(Text editor) {
            this.editor = editor;
        }

        private boolean isModifierCandidate;

        public void keyPressed(KeyEvent e) {
            isModifierCandidate = e.keyCode > 0 && e.character == 0 && e.stateMask == 0;
        }

        public void keyReleased(KeyEvent e) {
            if (isModifierCandidate && e.stateMask > 0 && e.stateMask == e.stateMask && e.character == 0) {// && e.time -time < 1000) {
                String text = editor.getText();
                Point selection = editor.getSelection();
                int i = selection.x - 1;
                while (i > -1 && Character.isWhitespace(text.charAt(i))) {
                    i--;
                }
                boolean needsPrefixDelimiter = i > -1 && !String.valueOf(text.charAt(i)).equals(DELIMITER);

                i = selection.y;
                while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
                    i++;
                }
                boolean needsPostfixDelimiter = i < text.length()
                        && !String.valueOf(text.charAt(i)).equals(DELIMITER);

                String insertString;

                if (needsPrefixDelimiter && needsPostfixDelimiter) {
                    insertString = Messages.format(
                            PyEditorMessages.PyEditorHoverConfigurationBlock_insertDelimiterAndModifierAndDelimiter,
                            new String[] { Action.findModifierString(e.stateMask) });
                } else if (needsPrefixDelimiter) {
                    insertString = Messages.format(
                            PyEditorMessages.PyEditorHoverConfigurationBlock_insertDelimiterAndModifier,
                            new String[] { Action.findModifierString(e.stateMask) });
                } else if (needsPostfixDelimiter) {
                    insertString = Messages.format(
                            PyEditorMessages.PyEditorHoverConfigurationBlock_insertModifierAndDelimiter,
                            new String[] { Action.findModifierString(e.stateMask) });
                } else {
                    insertString = Action.findModifierString(e.stateMask);
                }

                if (insertString != null) {
                    editor.insert(insertString);
                }
            }
        }
    };

    private Map<Button, String> fCheckBoxes = new HashMap<Button, String>();
    private SelectionListener fCheckBoxListener = new SelectionListener() {
        public void widgetDefaultSelected(SelectionEvent e) {
            Button button = (Button) e.widget;
            fStore.setValue(fCheckBoxes.get(button), button.getSelection());
        }

        public void widgetSelected(SelectionEvent e) {
            Button button = (Button) e.widget;
            fStore.setValue(fCheckBoxes.get(button), button.getSelection());
        }
    };

    private int fPremptColWidth;

    private Button combineHovers;

    private Text combiningHoverModifierEditor;

    private Button showDocstrings;

    private Button debugShowVars;

    private Button useFirstHover;

    private Label fCombiningHoverLabel;

    public PydevEditorHoverConfigurationBlock(PreferencePage mainPreferencePage, OverlayPreferenceStore store) {
        Assert.isNotNull(mainPreferencePage);
        Assert.isNotNull(store);
        fMainPreferencePage = mainPreferencePage;
        fStore = store;
        fStore.addKeys(createOverlayStoreKeys());
    }

    public HoverConfig getHoverConfigById(String id) {
        for (HoverConfig config : fHoverConfigs) {
            if (config.fId.equals(id)) {
                return config;
            }
        }
        return null;
    }

    private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {

        ArrayList<OverlayKey> overlayKeys = new ArrayList<OverlayKey>();

        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN,
                PyHoverPreferencesPage.EDITOR_ANNOTATION_ROLL_OVER));

        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,
                PyHoverPreferencesPage.EDITOR_TEXT_HOVER_MODIFIERS));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,
                PyHoverPreferencesPage.EDITOR_TEXT_HOVER_MODIFIER_MASKS));

        OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
        overlayKeys.toArray(keys);
        return keys;
    }

    /**
     * Creates page for hover preferences.
     *
     * @param parent the parent composite
     * @return the control for the preference page
     */
    public Control createControl(final Composite parent) {

        Composite comp = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(comp);
        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(comp);
        Composite buttonComp = new Composite(comp, SWT.BORDER);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(buttonComp);
        useFirstHover = new Button(buttonComp, SWT.RADIO | SWT.BORDER);
        useFirstHover.setText("Use highest priority Hover");
        GridDataFactory.fillDefaults().applyTo(useFirstHover);
        combineHovers = new Button(buttonComp, SWT.RADIO | SWT.BORDER);
        combineHovers.setText("Combine Hovers");
        GridDataFactory.fillDefaults().applyTo(combineHovers);
        combineHovers.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                combiningHoverModifierEditor.setEnabled(combineHovers.getSelection());
                fCombiningHoverLabel.setEnabled(combineHovers.getSelection());
                showPreemptColumn(combineHovers.getSelection());
                fHoverConfigs[0].fIsEnabled = combineHovers.getSelection();
            }
        });

        fCombiningHoverLabel = new Label(comp, SWT.PUSH);
        fCombiningHoverLabel.setText("Combined Hovers Key Modifier:");
        combiningHoverModifierEditor = new Text(comp, SWT.BORDER);
        combiningHoverModifierEditor.addKeyListener(new KeyModifierListener(combiningHoverModifierEditor));
        combiningHoverModifierEditor.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                handleModifierModified();
            }
        });
        GridDataFactory.fillDefaults().grab(true, false).applyTo(combiningHoverModifierEditor);

        ScrolledPageContent scrolled = new ScrolledPageContent(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);

        Composite hoverComposite = new Composite(scrolled, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        hoverComposite.setLayout(layout);

        // Multiple vertical ruler icons not supported in PyDev?
        // String rollOverLabel = PyEditorMessages.PyEditorHoverConfigurationBlock_annotationRollover;
        // addCheckBox(hoverComposite, rollOverLabel, PyHoverPreferencesPage.EDITOR_ANNOTATION_ROLL_OVER, 0);

        addFiller(hoverComposite);

        Label label = new Label(hoverComposite, SWT.NONE);
        label.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_hoverPreferences);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.BEGINNING;
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);

        TableLayoutComposite layouter = new TableLayoutComposite(hoverComposite, SWT.NONE);
        addColumnLayoutData(layouter);

        // Hover table
        fHoverTable = new Table(layouter,
                SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK);
        fHoverTable.setHeaderVisible(true);
        fHoverTable.setLinesVisible(true);

        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = SWTUtil.getTableHeightHint(fHoverTable, 10);
        gd.horizontalSpan = 2;
        gd.widthHint = new PixelConverter(parent).convertWidthInCharsToPixels(100);
        layouter.setLayoutData(gd);

        fHoverTable.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                handleHoverListSelection();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        TableLayout tableLayout = new TableLayout();
        fHoverTable.setLayout(tableLayout);
        fHoverTableViewer = new CheckboxTableViewer(fHoverTable);

        TableViewerColumn nameColumnViewer = new TableViewerColumn(fHoverTableViewer, SWT.NONE);
        nameColumnViewer.setLabelProvider(new PyEditorTextHoverDescriptorLabelProvider(fHoverTableViewer));
        fNameColumn = nameColumnViewer.getColumn();
        fNameColumn.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_nameColumnTitle);
        fNameColumn.setResizable(true);

        TableViewerColumn modifierColumnViewer = new TableViewerColumn(fHoverTableViewer, SWT.NONE);
        modifierColumnViewer.setLabelProvider(new PyEditorTextHoverDescriptorLabelProvider(fHoverTableViewer));
        fModifierColumn = modifierColumnViewer.getColumn();
        fModifierColumn.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_modifierColumnTitle);
        fModifierColumn.setResizable(true);

        TableViewerColumn priorityViewerColumn = new TableViewerColumn(fHoverTableViewer, SWT.NONE);
        priorityViewerColumn.setEditingSupport(new HoverTableEditingSupport(fHoverTableViewer, PRIORITY_PROP));
        priorityViewerColumn.setLabelProvider(new PyEditorTextHoverDescriptorLabelProvider(fHoverTableViewer));
        fPriorityColumn = priorityViewerColumn.getColumn();
        fPriorityColumn.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_priorityColumnTitle);
        fPriorityColumn.setResizable(true);

        TableViewerColumn preemptViewerColumn = new TableViewerColumn(fHoverTableViewer, SWT.NONE);
        preemptViewerColumn.setEditingSupport(new HoverTableEditingSupport(fHoverTableViewer, PREEMPT_PROP));
        preemptViewerColumn.setLabelProvider(new PyEditorTextHoverDescriptorLabelProvider(fHoverTableViewer));

        fPreemptColumn = preemptViewerColumn.getColumn();
        fPreemptColumn.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_preemptColumnTitle);
        fPreemptColumn.setResizable(true);

        fHoverTableViewer.setUseHashlookup(true);
        fHoverTableViewer.setContentProvider(new PyEditorTextHoverDescriptorContentProvider());

        fHoverTableViewer.addCheckStateListener(new ICheckStateListener() {
            /*
             * @see org.eclipse.jface.viewers.ICheckStateListener#checkStateChanged(org.eclipse.jface.viewers.Che((CheckboxTreeViewer) fHoverTableViewer)nt)
             */
            public void checkStateChanged(CheckStateChangedEvent event) {
                String id = ((PyEditorTextHoverDescriptor) event.getElement()).getId();
                if (id == null) {
                    return;
                }
                PyEditorTextHoverDescriptor[] descriptors = getContributedHovers();
                HoverConfig hoverConfig = null;
                int i = 0, length = fHoverConfigs.length;
                while (i < length) {
                    if (id.equals(descriptors[i].getId())) {
                        hoverConfig = fHoverConfigs[i];
                        hoverConfig.fIsEnabled = event.getChecked();
                        fModifierEditor.setEnabled(event.getChecked());
                        fHoverTableViewer.setSelection(new StructuredSelection(descriptors[i]));
                    }
                    i++;
                }
                handleHoverListSelection();
                updateStatus(hoverConfig);
            }
        });

        fHoverTableViewer.setSorter(new ViewerSorter() {

            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                return ((PyEditorTextHoverDescriptor) e1).fPriority
                        .compareTo(((PyEditorTextHoverDescriptor) e2).fPriority);
            }
        });

        //Don't show combining hover
        fHoverTableViewer.addFilter(new ViewerFilter() {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return !((PyEditorTextHoverDescriptor) element).getId().equals(
                        DefaultPydevCombiningHover.ID_DEFAULT_COMBINING_HOVER);
            }

        });

        // Text field for modifier string
        label = new Label(hoverComposite, SWT.LEFT);
        label.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_keyModifier);
        fModifierEditor = new Text(hoverComposite, SWT.BORDER);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        fModifierEditor.setLayoutData(gd);

        fModifierEditor.addKeyListener(new KeyModifierListener(fModifierEditor));

        fModifierEditor.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                handleModifierModified();
            }
        });

        // Description
        Label descriptionLabel = new Label(hoverComposite, SWT.LEFT);
        descriptionLabel.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_description);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        descriptionLabel.setLayoutData(gd);
        fDescription = new Text(hoverComposite, SWT.LEFT | SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER);
        SWTUtil.fixReadonlyTextBackground(fDescription);
        gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        fDescription.setLayoutData(gd);

        showDocstrings = new Button(parent, SWT.CHECK);
        showDocstrings.setText("Show docstrings");
        showDocstrings.setSelection(PyHoverPreferencesPage.getShowDocstringOnHover());
        debugShowVars = new Button(parent, SWT.CHECK);
        debugShowVars.setText("Show variables values while debugging");
        debugShowVars.setSelection(PyHoverPreferencesPage.getShowValuesWhileDebuggingOnHover());

        scrolled.setContent(hoverComposite);
        final Point size = hoverComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        scrolled.setMinSize(size.x, size.y);

        Dialog.applyDialogFont(scrolled);

        initialize();

        return scrolled;

    }

    private void addColumnLayoutData(TableLayoutComposite layouter) {
        layouter.addColumnData(new ColumnWeightData(35, true));
        layouter.addColumnData(new ColumnWeightData(35, true));
        layouter.addColumnData(new ColumnWeightData(15, true));
        layouter.addColumnData(new ColumnWeightData(15, true));
    }

    private PyEditorTextHoverDescriptor[] getContributedHovers() {
        return getContributedHovers(false);
    }

    private PyEditorTextHoverDescriptor[] getContributedHovers(boolean useRegisteredExtensionPointValues) {
        return PydevPlugin.getDefault().getPyEditorTextHoverDescriptors(useRegisteredExtensionPointValues);
    }

    public void initialize() {
        PyEditorTextHoverDescriptor[] hoverDescs = getContributedHovers();
        fHoverConfigs = new HoverConfig[hoverDescs.length];
        for (int i = 0; i < hoverDescs.length; i++) {
            fHoverConfigs[i] = new HoverConfig(hoverDescs[i].getId(), hoverDescs[i].getModifierString(),
                    hoverDescs[i].getStateMask(),
                    hoverDescs[i].isEnabled(), hoverDescs[i].getPriority(), hoverDescs[i].isPreempt());
        }

        fHoverTableViewer.setInput(hoverDescs);

        initializeFields();
    }

    void initializeFields() {
        fModifierEditor.setEnabled(false);

        //        Iterator<Button> e = fCheckBoxes.keySet().iterator();
        //        while (e.hasNext()) {
        //            Button b = e.next();
        //            String key = fCheckBoxes.get(b);
        //            b.setSelection(fStore.getBoolean(key));
        //        }

        combineHovers.setSelection(PyHoverPreferencesPage.getCombineHoverInfo());
        useFirstHover.setSelection(!PyHoverPreferencesPage.getCombineHoverInfo());
        combiningHoverModifierEditor.setEnabled(PyHoverPreferencesPage.getCombineHoverInfo());
        fCombiningHoverLabel.setEnabled(PyHoverPreferencesPage.getCombineHoverInfo());

        //skip first hover config (combining hover) which is filtered from viewer
        for (int i = 1; i < fHoverConfigs.length; i++) {
            fHoverTable.getItem(i - 1).setChecked(fHoverConfigs[i].fIsEnabled);
        }
        fHoverTableViewer.refresh();
        combiningHoverModifierEditor.setText(fHoverConfigs[0].fModifierString);
    }

    public void performOk() {
        StringBuffer buf = new StringBuffer();
        StringBuffer maskBuf = new StringBuffer();
        StringBuffer priorityBuf = new StringBuffer();
        StringBuffer preemptBuf = new StringBuffer();
        PyEditorTextHoverDescriptor[] hoverDescripters = (PyEditorTextHoverDescriptor[]) fHoverTableViewer.getInput();
        Arrays.sort(hoverDescripters, new Comparator<PyEditorTextHoverDescriptor>() {

            @Override
            public int compare(PyEditorTextHoverDescriptor o1, PyEditorTextHoverDescriptor o2) {
                return o1.fPriority.compareTo(o2.fPriority);
            }

        });
        for (int i = 0; i < hoverDescripters.length; i++) {
            buf.append(hoverDescripters[i].getId());
            buf.append(PyEditorTextHoverDescriptor.VALUE_SEPARATOR);
            if (!fHoverConfigs[i].fIsEnabled) {
                buf.append(PyEditorTextHoverDescriptor.DISABLED_TAG);
            }
            String modifier = fHoverConfigs[i].fModifierString;
            if (modifier == null || modifier.length() == 0) {
                modifier = PyEditorTextHoverDescriptor.NO_MODIFIER;
            }
            buf.append(modifier);
            buf.append(PyEditorTextHoverDescriptor.VALUE_SEPARATOR);

            maskBuf.append(hoverDescripters[i].getId());
            maskBuf.append(PyEditorTextHoverDescriptor.VALUE_SEPARATOR);
            maskBuf.append(fHoverConfigs[i].fStateMask);
            maskBuf.append(PyEditorTextHoverDescriptor.VALUE_SEPARATOR);

            priorityBuf.append(hoverDescripters[i].getId());
            priorityBuf.append(PyEditorTextHoverDescriptor.VALUE_SEPARATOR);
            priorityBuf.append(fHoverConfigs[i].fPriority);
            priorityBuf.append(PyEditorTextHoverDescriptor.VALUE_SEPARATOR);

            preemptBuf.append(hoverDescripters[i].getId());
            preemptBuf.append(PyEditorTextHoverDescriptor.VALUE_SEPARATOR);
            preemptBuf.append(fHoverConfigs[i].fPreempt);
            preemptBuf.append(PyEditorTextHoverDescriptor.VALUE_SEPARATOR);
        }
        fStore.setValue(PyHoverPreferencesPage.EDITOR_TEXT_HOVER_MODIFIERS, buf.toString());
        fStore.setValue(PyHoverPreferencesPage.EDITOR_TEXT_HOVER_MODIFIER_MASKS, maskBuf.toString());
        PydevPrefs.getPreferenceStore().setValue(PyHoverPreferencesPage.EDITOR_TEXT_HOVER_PRORITIES,
                priorityBuf.toString());
        PydevPrefs.getPreferenceStore().setValue(PyHoverPreferencesPage.EDITOR_TEXT_HOVER_PREEMPTS,
                preemptBuf.toString());

        PydevPrefs.getPreferenceStore().setValue(PyHoverPreferencesPage.COMBINE_HOVER_INFO,
                combineHovers.getSelection());
        PydevPrefs.getPreferenceStore().setValue(PyHoverPreferencesPage.SHOW_DOCSTRING_ON_HOVER,
                showDocstrings.getSelection());
        PydevPrefs.getPreferenceStore()
                .setValue(PyHoverPreferencesPage.SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER, debugShowVars.getSelection());

        PydevPlugin.getDefault().resetPyEditorTextHoverDescriptors();
    }

    public void performDefaults() {
        restoreFromPreferences();
        initializeFields();
        updateStatus(null);
        if (!fHoverTableViewer.getSelection().isEmpty()) {
            handleHoverListSelection();
        }
    }

    private void restoreFromPreferences() {
        String compiledTextHoverModifiers = fStore.getString(PyHoverPreferencesPage.EDITOR_TEXT_HOVER_MODIFIERS);

        StringTokenizer tokenizer = new StringTokenizer(compiledTextHoverModifiers,
                PyEditorTextHoverDescriptor.VALUE_SEPARATOR);
        HashMap<String, String> idToModifier = new HashMap<String, String>(tokenizer.countTokens() / 2);

        while (tokenizer.hasMoreTokens()) {
            String id = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens()) {
                idToModifier.put(id, tokenizer.nextToken());
            }
        }

        String compiledTextHoverModifierMasks = fStore
                .getString(PyHoverPreferencesPage.EDITOR_TEXT_HOVER_MODIFIER_MASKS);

        tokenizer = new StringTokenizer(compiledTextHoverModifierMasks, PyEditorTextHoverDescriptor.VALUE_SEPARATOR);
        HashMap<String, String> idToModifierMask = new HashMap<String, String>(tokenizer.countTokens() / 2);

        while (tokenizer.hasMoreTokens()) {
            String id = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens()) {
                idToModifierMask.put(id, tokenizer.nextToken());
            }
        }

        String compiledTextHoverPriorities = PydevPrefs.getPreferenceStore()
                .getString(PyHoverPreferencesPage.EDITOR_TEXT_HOVER_PRORITIES);

        tokenizer = new StringTokenizer(compiledTextHoverPriorities, PyEditorTextHoverDescriptor.VALUE_SEPARATOR);
        HashMap<String, String> idToPriority = new HashMap<String, String>(tokenizer.countTokens() / 2);

        while (tokenizer.hasMoreTokens()) {
            String id = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens()) {
                idToPriority.put(id, tokenizer.nextToken());
            }
        }

        String compiledTextHoverPreempts = PydevPrefs.getPreferenceStore()
                .getString(PyHoverPreferencesPage.EDITOR_TEXT_HOVER_PREEMPTS);

        tokenizer = new StringTokenizer(compiledTextHoverPreempts, PyEditorTextHoverDescriptor.VALUE_SEPARATOR);
        HashMap<String, String> idToPreempt = new HashMap<String, String>(tokenizer.countTokens() / 2);

        while (tokenizer.hasMoreTokens()) {
            String id = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens()) {
                idToPreempt.put(id, tokenizer.nextToken());
            }
        }
        for (int i = 0; i < fHoverConfigs.length; i++) {
            // There is no extension point field for these values, so restore from preferences
            String modifierString = idToModifier.get(getContributedHovers(true)[i].getId());
            boolean enabled = true;
            if (modifierString == null) {
                modifierString = PyEditorTextHoverDescriptor.DISABLED_TAG;
            }

            if (modifierString.startsWith(PyEditorTextHoverDescriptor.DISABLED_TAG)) {
                enabled = false;
                modifierString = modifierString.substring(1);
            }

            if (modifierString.equals(PyEditorTextHoverDescriptor.NO_MODIFIER)) {
                modifierString = ""; //$NON-NLS-1$
            }

            fHoverConfigs[i].fModifierString = modifierString;
            fHoverConfigs[i].fIsEnabled = enabled;
            fHoverConfigs[i].fStateMask = PyEditorTextHoverDescriptor.computeStateMask(modifierString);
            if (fHoverConfigs[i].fStateMask == -1) {
                try {
                    fHoverConfigs[i].fStateMask = Integer
                            .parseInt(idToModifierMask.get(getContributedHovers(true)[i].getId()));
                } catch (NumberFormatException ex) {
                    fHoverConfigs[i].fStateMask = -1;
                }
            }

            // Restore values from extension point registration, ignoring preference overrides
            fHoverConfigs[i].fPriority = getContributedHovers(true)[i].fPriority;
            fHoverConfigs[i].fPreempt = getContributedHovers(true)[i].fPreempt;
            initialize();
            fHoverTableViewer.setInput(getContributedHovers(true));
        }
    }

    private void handleModifierModified() {
        int i = fHoverTable.getSelectionIndex();
        HoverConfig hoverConfig = null;
        Text editor = null;
        if (i == -1) {
            hoverConfig = fHoverConfigs[0];
            editor = combiningHoverModifierEditor;
        } else {
            hoverConfig = fHoverConfigs[convertIndex(i)];
            editor = fModifierEditor;
        }

        String modifiers = editor.getText();
        hoverConfig.fModifierString = modifiers;
        hoverConfig.fStateMask = PyEditorTextHoverDescriptor.computeStateMask(modifiers);

        // update table
        if (!fHoverTableViewer.isCellEditorActive() && i >= 0) {
            fHoverTableViewer.refresh(getContributedHovers()[i]);
        }

        updateStatus(hoverConfig);
    }

    private void handleSetPriority(Integer priority) {
        int i = fHoverTable.getSelectionIndex();
        if (i == -1) {
            return;
        }
        HoverConfig hoverConfig = fHoverConfigs[convertIndex(i)];
        hoverConfig.fPriority = priority;

        /**
         * Ensure hover configs and hover descriptors are in same order
         */
        fHoverTableViewer.update(getContributedHovers()[i], null);
        Arrays.sort(fHoverConfigs, new Comparator<HoverConfig>() {

            @Override
            public int compare(HoverConfig o1, HoverConfig o2) {
                return o1.fPriority.compareTo(o2.fPriority);
            }

        });
        fHoverTableViewer.refresh();

        updateStatus(hoverConfig);
    }

    public void handleSetPreempt(Boolean preempt) {
        int i = fHoverTable.getSelectionIndex();
        if (i == -1) {
            return;
        }

        HoverConfig hoverConfig = fHoverConfigs[convertIndex(i)];
        hoverConfig.fPreempt = preempt;

        // update table
        fHoverTableViewer.refresh(((IStructuredSelection) fHoverTableViewer.getSelection()).getFirstElement());

        updateStatus(hoverConfig);
    }

    private void handleHoverListSelection() {
        int i = fHoverTable.getSelectionIndex();

        if (i == -1) {
            if (fHoverTable.getSelectionCount() == 0) {
                fModifierEditor.setEnabled(false);
            }
            return;
        }
        HoverConfig hoverConfig = fHoverConfigs[convertIndex(i)];
        boolean enabled = hoverConfig.fIsEnabled;
        fModifierEditor.setEnabled(enabled);
        fModifierEditor.setText(hoverConfig.fModifierString);
        String description = getContributedHovers()[convertIndex(i)].getDescription();
        if (description == null) {
            description = ""; //$NON-NLS-1$
        }
        fDescription.setText(description);
    }

    IStatus getStatus() {
        if (fStatus == null) {
            fStatus = new StatusInfo();
        }
        return fStatus;
    }

    private void updateStatus(HoverConfig hoverConfig) {
        if (hoverConfig != null && hoverConfig.fIsEnabled && hoverConfig.fStateMask == -1) {
            fStatus = new StatusInfo(IStatus.ERROR,
                    Messages.format(PyEditorMessages.PyEditorHoverConfigurationBlock_modifierIsNotValid,
                            hoverConfig.fModifierString));
        } else {
            fStatus = new StatusInfo();
        }

        int i = 0;
        HashMap<Integer, String> stateMasks = new HashMap<Integer, String>(fHoverConfigs.length);
        while (fStatus.isOK() && i < fHoverConfigs.length) {
            if (fHoverConfigs[i].fIsEnabled) {
                String label = getContributedHovers()[i].getLabel();
                Integer stateMask = new Integer(fHoverConfigs[i].fStateMask);
                if (fHoverConfigs[i].fStateMask == -1) {
                    fStatus = new StatusInfo(IStatus.ERROR,
                            Messages.format(
                                    PyEditorMessages.PyEditorHoverConfigurationBlock_modifierIsNotValidForHover,
                                    new String[] { fHoverConfigs[i].fModifierString, label }));
                    //                } else if (stateMasks.containsKey(stateMask)) {
                    //                    fStatus = new StatusInfo(IStatus.ERROR,
                    //                            Messages.format(PyEditorMessages.PyEditorHoverConfigurationBlock_duplicateModifier,
                    //                                    new String[] { label, stateMasks.get(stateMask) }));
                } else {
                    stateMasks.put(stateMask, label);
                }
            }
            i++;
        }

        fMainPreferencePage.setValid(fStatus.isOK());
        StatusUtil.applyToStatusLine(fMainPreferencePage, fStatus);
    }

    /*private Button addCheckBox(Composite parent, String label, String key, int indentation) {
        Button checkBox = new Button(parent, SWT.CHECK);
        checkBox.setText(label);
    
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalIndent = indentation;
        gd.horizontalSpan = 2;
        checkBox.setLayoutData(gd);
        checkBox.addSelectionListener(fCheckBoxListener);
    
        fCheckBoxes.put(checkBox, key);
    
        return checkBox;
    }*/

    private void addFiller(Composite composite) {
        PixelConverter pixelConverter = new PixelConverter(composite);
        Label filler = new Label(composite, SWT.LEFT);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        gd.heightHint = pixelConverter.convertHeightInCharsToPixels(1) / 2;
        filler.setLayoutData(gd);
    }

    /*
     * @see DialogPage#dispose()
     */
    public void dispose() {
        // nothing to dispose
    }

    public void showPreemptColumn(boolean show) {
        if (fPreemptColumn.getWidth() != 0) {
            fPremptColWidth = fPreemptColumn.getWidth();
        }
        fPreemptColumn.setWidth(show ? fPremptColWidth : 0);
    }

    /*
     * Because fHoverConfigs contains a record for the combining hover, and
     * this item is excluded from fHoverTable by the viewer filter, we adjust
     * the index when mapping from a table item to an item in fHoverConfigs.
     */
    private int convertIndex(int index) {
        //verify item is filtered, just to be sure
        if (fHoverTableViewer != null && fHoverTableViewer.getInput() != null && fHoverTable != null) {
            if (((Object[]) fHoverTableViewer.getInput()).length == fHoverTable.getItemCount() + 1) {
                return index + 1;
            }
        }
        return index;
    }
}
