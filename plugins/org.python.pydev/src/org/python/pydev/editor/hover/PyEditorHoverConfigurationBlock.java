/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mark Leone - Modifications for PyDev
 *******************************************************************************/
package org.python.pydev.editor.hover;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
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
import org.eclipse.ui.progress.UIJob;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.StatusInfo;
import org.python.pydev.plugin.preferences.IPreferenceConfigurationBlock;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.ui.EmulatedNativeCheckBoxLabelProvider;
import org.python.pydev.ui.ScrolledPageContent;
import org.python.pydev.ui.TableLayoutComposite;
import org.python.pydev.utils.Messages;
import org.python.pydev.utils.PyEditorMessages;
import org.python.pydev.utils.SWTUtil;
import org.python.pydev.utils.StatusUtil;

/**
 * Configures PyDev Editor hover preferences.
 */
public class PyEditorHoverConfigurationBlock implements IPreferenceConfigurationBlock {

    private static final String DELIMITER = PyEditorMessages.PyEditorHoverConfigurationBlock_delimiter;

    private static final int ENABLED_PROP = 0;
    private static final int PRIORITY_PROP = 1;
    private static final int MODIFIER_PROP = 2;
    private static final int PREEMPT_PROP = 3;

    private String priorityHelpStr = " (Lowest number is highest priority)";

    private class PyEditorTextHoverDescriptorLabelProvider extends EmulatedNativeCheckBoxLabelProvider {

        public PyEditorTextHoverDescriptorLabelProvider(ColumnViewer viewer) {
            super(viewer);
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
                case PRIORITY_PROP:
                    cell.setText(String.valueOf(((PyEditorTextHoverDescriptor) cell.getElement()).getPriority()));
                    break;
                case MODIFIER_PROP:
                    TableItem item = (TableItem) fHoverTableViewer.testFindItem(cell.getElement());
                    int index = fHoverTable.indexOf(item);
                    cell.setText(fHoverDescs[index].fModifierString);
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
            return ((PyEditorTextHoverDescriptor) element).isPreempt();
        }
    }

    private class PyEditorTextHoverDescriptorContentProvider implements IStructuredContentProvider {

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // Do nothing since the viewer listens to resource deltas
        }

        @Override
        public void dispose() {
        }

        @Override
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
                                if (val <= 0) {
                                    valid = false;
                                }
                            } catch (NumberFormatException | ClassCastException e) {
                                valid = false;
                            }
                        }
                        editor.getControl()
                                .setBackground(valid ? null : Display.getDefault().getSystemColor(SWT.COLOR_RED));
                        return (valid ? null : "positive integer required");
                    }
                });
                ((Text) ((TextCellEditor) editor).getControl()).selectAll();
            } else if (this.column == PREEMPT_PROP) {
                this.editor = new CheckboxCellEditor(viewer.getTable());
            }
            return this.editor;
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.EditingSupport#canEdit(java.lang.Object)
         */
        @Override
        protected boolean canEdit(Object element) {
            return true;
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.EditingSupport#getValue(java.lang.Object)
         */
        @Override
        protected Object getValue(Object element) {
            PyEditorTextHoverDescriptor descr = (PyEditorTextHoverDescriptor) element;
            if (descr != null) {
                switch (this.column) {
                    case PRIORITY_PROP:
                        fTableLabel.setText(
                                PyEditorMessages.PyEditorHoverConfigurationBlock_hoverPreferences + priorityHelpStr);
                        fTableLabel.setStyleRange(priorityHelpRange);
                        fTableLabel.getParent().layout();
                        return String.valueOf(descr.getPriority());
                    case PREEMPT_PROP:
                        return descr.isPreempt();
                    default:
                }

            }
            return "";
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.EditingSupport#setValue(java.lang.Object, java.lang.Object)
         */
        @Override
        protected void setValue(Object element, Object value) {
            switch (this.column) {
                case PRIORITY_PROP:
                    ((PyEditorTextHoverDescriptor) element).setPriority(Integer.parseInt((String) value));
                    handleSetPriority(Integer.parseInt((String) value));
                    fTableLabel.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_hoverPreferences);
                    fTableLabel.setStyleRange(null);
                    fTableLabel.getParent().layout();
                    break;
                case PREEMPT_PROP:
                    ((PyEditorTextHoverDescriptor) element)
                            .setIsPreempt(!((PyEditorTextHoverDescriptor) element).isPreempt());
                    handleSetPreempt((Boolean) value);
                    break;
                default:
            }
            this.editor.dispose();
        }

    }

    private Text fModifierEditor;
    private Table fHoverTable;
    private CheckboxTableViewer fHoverTableViewer;
    private TableColumn fNameColumn;
    TableColumn fModifierColumn;
    private TableColumn fPriorityColumn;
    TableColumn fPreemptColumn;
    private int[] fColWidths = new int[] { 35, 15, 35, 15 };
    private Map<TableColumn, Integer> fColWidthsMap = new HashMap<TableColumn, Integer>();
    private Text fDescription;

    private StyleRange priorityHelpRange = null;

    private PreferencePage fMainPreferencePage;

    public static StatusInfo fStatus;

    private class KeyModifierListener implements KeyListener {

        private Text editor;

        private KeyModifierListener(Text editor) {
            this.editor = editor;
        }

        private boolean isModifierCandidate;

        @Override
        public void keyPressed(KeyEvent e) {
            isModifierCandidate = e.keyCode > 0 && e.character == 0 && e.stateMask == 0;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (isModifierCandidate && e.stateMask > 0 && e.stateMask == e.stateMask && e.character == 0) {
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

    private Button fCombineHovers;

    private Text fCombiningHoverModifierEditor;

    private Button fDebugShowVars;

    private Button fUseFirstHover;

    private Label fCombiningHoverLabel;

    private Button fUseHoverDivider;

    private Composite fComp;

    private GridData fDisabledLabelGridData;

    private Composite fButtonComp;

    private PyEditorTextHoverDescriptor[] fHoverDescs;

    private PyEditorTextHoverDescriptor fCombiningHover;

    private Label fModifierFieldLabel;

    private StyledText fTableLabel;

    public PyEditorHoverConfigurationBlock(PreferencePage mainPreferencePage) {
        Assert.isNotNull(mainPreferencePage);
        fMainPreferencePage = mainPreferencePage;
    }

    /**
     * Creates page for hover preferences.
     *
     * @param parent the parent composite
     * @return the control for the preference page
     */
    @Override
    public Control createControl(final Composite parent) {

        priorityHelpRange = new StyleRange(
                PyEditorMessages.PyEditorHoverConfigurationBlock_hoverPreferences.length(),
                priorityHelpStr.length(),
                parent.getDisplay().getSystemColor(SWT.COLOR_BLUE), null);

        fComp = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(fComp);
        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(fComp);

        fButtonComp = new Composite(fComp, SWT.BORDER);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(fButtonComp);
        fUseFirstHover = new Button(fButtonComp, SWT.RADIO | SWT.BORDER);
        fUseFirstHover.setText("Use highest priority Hover");
        GridDataFactory.fillDefaults().applyTo(fUseFirstHover);
        fCombineHovers = new Button(fButtonComp, SWT.RADIO | SWT.BORDER);
        fCombineHovers.setText("Combine Hovers");
        GridDataFactory.fillDefaults().applyTo(fCombineHovers);
        fCombineHovers.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fCombiningHoverModifierEditor.setEnabled(fCombineHovers.getSelection());
                fCombiningHoverLabel.setEnabled(fCombineHovers.getSelection());
                showColumn(fPreemptColumn, fCombineHovers.getSelection());
                showColumn(fModifierColumn, !fCombineHovers.getSelection());
                fModifierFieldLabel.setEnabled(!fCombineHovers.getSelection());
            }
        });

        fCombiningHoverLabel = new Label(fComp, SWT.PUSH);
        fCombiningHoverLabel.setText("Combined Hovers Key Modifier:");
        fCombiningHoverModifierEditor = new Text(fComp, SWT.BORDER);
        fCombiningHoverModifierEditor.addKeyListener(new KeyModifierListener(fCombiningHoverModifierEditor));
        fCombiningHoverModifierEditor.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                handleModifierModified(fCombiningHoverModifierEditor);
            }
        });
        GridDataFactory.fillDefaults().grab(true, false).applyTo(fCombiningHoverModifierEditor);

        ScrolledPageContent scrolled = new ScrolledPageContent(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);

        Composite hoverComposite = new Composite(scrolled, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        hoverComposite.setLayout(layout);

        addFiller(hoverComposite);

        fTableLabel = new StyledText(hoverComposite, SWT.NONE);
        fTableLabel.setEditable(false);
        fTableLabel.setEnabled(false);
        fTableLabel.setBackground(fTableLabel.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        fTableLabel.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_hoverPreferences);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.BEGINNING;
        gd.horizontalSpan = 2;
        fTableLabel.setLayoutData(gd);

        TableLayoutComposite layouter = new TableLayoutComposite(hoverComposite, SWT.NONE);
        addColumnLayoutData(layouter, fColWidths);

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
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleHoverListSelection();
            }

            @Override
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

        TableViewerColumn priorityViewerColumn = new TableViewerColumn(fHoverTableViewer, SWT.NONE);
        priorityViewerColumn.setEditingSupport(new HoverTableEditingSupport(fHoverTableViewer, PRIORITY_PROP));
        priorityViewerColumn.setLabelProvider(new PyEditorTextHoverDescriptorLabelProvider(fHoverTableViewer));
        fPriorityColumn = priorityViewerColumn.getColumn();
        fPriorityColumn.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_priorityColumnTitle);
        fPriorityColumn.setResizable(true);

        TableViewerColumn modifierColumnViewer = new TableViewerColumn(fHoverTableViewer, SWT.NONE);
        modifierColumnViewer.setLabelProvider(new PyEditorTextHoverDescriptorLabelProvider(fHoverTableViewer));
        fModifierColumn = modifierColumnViewer.getColumn();
        fModifierColumn.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_modifierColumnTitle);
        fModifierColumn.setResizable(true);

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
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                String id = ((PyEditorTextHoverDescriptor) event.getElement()).getId();
                if (id == null) {
                    return;
                }
                PyEditorTextHoverDescriptor[] descriptors = fHoverDescs;
                PyEditorTextHoverDescriptor hover = null;
                int i = 0, length = fHoverDescs.length;
                while (i < length) {
                    if (id.equals(descriptors[i].getId())) {
                        hover = fHoverDescs[i];
                        hover.setIsEnabled(event.getChecked());
                        fModifierEditor.setEnabled(event.getChecked());
                        fHoverTableViewer.setSelection(new StructuredSelection(descriptors[i]));
                    }
                    i++;
                }
                handleHoverListSelection();
                updateStatus(hover);
            }
        });

        fHoverTableViewer.setSorter(new ViewerSorter() {

            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                return ((PyEditorTextHoverDescriptor) e1).getPriority()
                        .compareTo(((PyEditorTextHoverDescriptor) e2).getPriority());
            }
        });

        // Text field for modifier string
        fModifierFieldLabel = new Label(hoverComposite, SWT.LEFT);
        fModifierFieldLabel.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_keyModifier);
        fModifierEditor = new Text(hoverComposite, SWT.BORDER);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        fModifierEditor.setLayoutData(gd);

        fModifierEditor.addKeyListener(new KeyModifierListener(fModifierEditor));

        fModifierEditor.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                handleModifierModified(fModifierEditor);
            }
        });

        // Description
        Label label = new Label(hoverComposite, SWT.LEFT);
        label.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_description);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);
        fDescription = new Text(hoverComposite, SWT.LEFT | SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER);
        SWTUtil.fixReadonlyTextBackground(fDescription);
        gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        fDescription.setLayoutData(gd);

        fDebugShowVars = new Button(parent, SWT.CHECK);
        fDebugShowVars.setText("Show variables values while debugging");
        fDebugShowVars.setSelection(PyHoverPreferencesPage.getShowValuesWhileDebuggingOnHover());
        fUseHoverDivider = new Button(parent, SWT.CHECK);
        fUseHoverDivider.setText("Add divider between contributions when combining hovers");
        fUseHoverDivider.setSelection(PyHoverPreferencesPage.getUseHoverDelimiters());

        scrolled.setContent(hoverComposite);
        final Point size = hoverComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        scrolled.setMinSize(size.x, size.y);

        Dialog.applyDialogFont(scrolled);

        return fComp;

    }

    private void addColumnLayoutData(TableLayoutComposite layouter, int[] widths) {
        layouter.addColumnData(new ColumnWeightData(widths[0], true));
        layouter.addColumnData(new ColumnWeightData(widths[1], true));
        layouter.addColumnData(new ColumnWeightData(widths[2], true));
        layouter.addColumnData(new ColumnWeightData(widths[3], true));
    }

    private PyEditorTextHoverDescriptor[] getContributedHovers() {
        return PydevPlugin.getDefault().getPyEditorTextHoverDescriptors();
    }

    @Override
    public void initialize() {
        //need to do this asynchronously, or it has no effect
        new UIJob("Show/Hide Column") {

            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                showColumn(fPreemptColumn, PyHoverPreferencesPage.getCombineHoverInfo());
                showColumn(fModifierColumn, !PyHoverPreferencesPage.getCombineHoverInfo());
                fModifierFieldLabel.setEnabled(!fCombineHovers.getSelection());
                return Status.OK_STATUS;
            }

        }.schedule();
        doInit(true);
    }

    private void doInit(boolean refreshHovers) {
        if (refreshHovers) {
            fHoverDescs = getContributedHovers();
            fCombiningHover = PydevPlugin.getCombiningHoverDescriptor();
        }
        fHoverTableViewer.setInput(fHoverDescs);
        fHoverTableViewer.refresh();
        initializeFields();
    }

    void initializeFields() {
        fModifierEditor.setEnabled(false);

        fCombineHovers.setSelection(PyHoverPreferencesPage.getCombineHoverInfo());
        fUseFirstHover.setSelection(!PyHoverPreferencesPage.getCombineHoverInfo());
        fCombiningHoverModifierEditor.setEnabled(PyHoverPreferencesPage.getCombineHoverInfo());
        fCombiningHoverLabel.setEnabled(PyHoverPreferencesPage.getCombineHoverInfo());

        for (int i = 0; i < fHoverDescs.length; i++) {
            fHoverTable.getItem(i).setChecked(fHoverDescs[i].isEnabled());
        }
        fHoverTableViewer.refresh();
        fCombiningHoverModifierEditor.setText(fCombiningHover.fModifierString);
    }

    @Override
    public void performOk() {
        Integer modifierMask = 0;
        Integer priority = 0;
        Boolean preempt = false;
        PyEditorTextHoverDescriptor[] hoverDescripters = (PyEditorTextHoverDescriptor[]) fHoverTableViewer.getInput();
        sortHoverDescriptors();

        if (hoverDescripters != fHoverDescs) {
            Log.log("Expecting hoverDescripters to be == fHoverDescs");
        }
        //save preferences for configured hovers
        for (int i = 0; i < hoverDescripters.length; i++) {
            PyEditorTextHoverDescriptor hoverDesc = fHoverDescs[i];
            String modifier = hoverDesc.fModifierString;
            if (modifier == null || modifier.length() == 0) {
                modifier = PyEditorTextHoverDescriptor.NO_MODIFIER;
            }
            modifierMask = hoverDesc.fStateMask;
            priority = hoverDesc.getPriority();
            preempt = hoverDesc.isPreempt();

            Boolean enable = hoverDesc.isEnabled();
            String hoverDescId = hoverDesc.getId();
            PydevPrefs.getPreferenceStore().setValue(
                    PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER + hoverDescId, modifier);
            PydevPrefs.getPreferenceStore().setValue(
                    PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER_MASK + hoverDescId, modifierMask);
            PydevPrefs.getPreferenceStore().setValue(
                    PyHoverPreferencesPage.KEY_TEXT_HOVER_PRIORITY + hoverDescId, priority);
            PydevPrefs.getPreferenceStore().setValue(
                    PyHoverPreferencesPage.KEY_TEXT_HOVER_PREEMPT + hoverDescId, preempt);
            PydevPrefs.getPreferenceStore().setValue(
                    PyHoverPreferencesPage.KEY_TEXT_HOVER_ENABLE + hoverDescId, enable);
        }

        //save preferences for the combining hover
        PydevPrefs.getPreferenceStore().setValue(
                PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER + fCombiningHover.getId(),
                fCombiningHover.getModifierString());
        PydevPrefs.getPreferenceStore().setValue(
                PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER_MASK + fCombiningHover.getId(),
                fCombiningHover.fStateMask);
        PydevPrefs.getPreferenceStore().setValue(
                PyHoverPreferencesPage.KEY_TEXT_HOVER_ENABLE + fCombiningHover.getId(),
                PyHoverPreferencesPage.getCombineHoverInfo());

        //save general hover preferences
        PydevPlugin.getDefault().getPreferenceStore().setValue(PyHoverPreferencesPage.COMBINE_HOVER_INFO,
                fCombineHovers.getSelection());
        PydevPlugin.getDefault().getPreferenceStore().setValue(
                PyHoverPreferencesPage.SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER,
                fDebugShowVars.getSelection());
        PydevPlugin.getDefault().getPreferenceStore().setValue(PyHoverPreferencesPage.USE_HOVER_DIVIDER,
                fUseHoverDivider.getSelection());

        PydevPlugin.getDefault().resetPyEditorTextHoverDescriptors();
        PydevCombiningHover.installTextHovers();
    }

    /**
     * Sort the hover descriptors to match the order they appear in the viewer,
     * which has a viewer sorter that sorts by priority
     */
    private void sortHoverDescriptors() {
        Arrays.sort(fHoverDescs, new Comparator<PyEditorTextHoverDescriptor>() {

            @Override
            public int compare(PyEditorTextHoverDescriptor o1, PyEditorTextHoverDescriptor o2) {
                return o1.getPriority().compareTo(o2.getPriority());
            }

        });
    }

    @Override
    public void performCancel() {
        PydevPlugin.getDefault().resetPyEditorTextHoverDescriptors();
    }

    @Override
    public void performDefaults() {
        restoreFromPreferences();
        doInit(false);
        initializeFields();
        updateStatus(null);
        setcheckStates();
        if (!fHoverTableViewer.getSelection().isEmpty()) {
            handleHoverListSelection();
        }
    }

    private void setcheckStates() {
        for (PyEditorTextHoverDescriptor hover : fHoverDescs) {
            fHoverTableViewer.setChecked(hover, hover.isEnabled());
        }
    }

    /**
     * Computes the state mask for the given modifier string.
     *
     * @param modifiers the string with the modifiers, separated by '+', '-', ';', ',' or '.'
     * @return the state mask or -1 if the input is invalid
     */
    public int computeStateMask(String modifiers) {
        if (modifiers == null) {
            return -1;
        }

        if (modifiers.length() == 0) {
            return SWT.NONE;
        }

        int stateMask = 0;
        StringTokenizer modifierTokenizer = new StringTokenizer(modifiers, ",;.:+-* "); //$NON-NLS-1$
        while (modifierTokenizer.hasMoreTokens()) {
            int modifier = PyAction.findLocalizedModifier(modifierTokenizer.nextToken());
            if (modifier == 0 || (stateMask & modifier) == modifier) {
                return -1;
            }
            stateMask = stateMask | modifier;
        }
        return stateMask;
    }

    private void restoreFromPreferences() {

        //restore settings for contributed hovers
        for (int i = 0; i < fHoverDescs.length; i++) {
            String modifierString = PydevPrefs.getPreferenceStore()
                    .getDefaultString(PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER + fHoverDescs[i].getId());

            if (PyEditorTextHoverDescriptor.NO_MODIFIER.equals(modifierString)) {
                modifierString = ""; //$NON-NLS-1$
            }

            fHoverDescs[i].fModifierString = modifierString;

            fHoverDescs[i].fStateMask = computeStateMask(modifierString);
            if (fHoverDescs[i].fStateMask == -1) {
                try {
                    fHoverDescs[i].fStateMask = Integer
                            .parseInt(PydevPrefs.getPreferenceStore().getString(
                                    PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER_MASK + fHoverDescs[i].getId()));
                } catch (NumberFormatException ex) {
                    fHoverDescs[i].fStateMask = -1;
                }
            }

            fHoverDescs[i].setIsEnabled(PydevPlugin.getDefault().getPreferenceStore()
                    .getDefaultBoolean(PyHoverPreferencesPage.KEY_TEXT_HOVER_ENABLE + fHoverDescs[i].getId()));
            fHoverDescs[i].setPriority(PydevPlugin.getDefault().getPreferenceStore()
                    .getDefaultInt(PyHoverPreferencesPage.KEY_TEXT_HOVER_PRIORITY + fHoverDescs[i].getId()));
            fHoverDescs[i].setIsPreempt(PydevPlugin.getDefault().getPreferenceStore()
                    .getDefaultBoolean(PyHoverPreferencesPage.KEY_TEXT_HOVER_PREEMPT + fHoverDescs[i].getId()));
            fHoverTableViewer.refresh();
            sortHoverDescriptors();
        }

        //restore settings for combining hover
        String modifierString = PydevPrefs.getPreferenceStore().getDefaultString(
                PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER + fCombiningHover.getId());
        if (PyEditorTextHoverDescriptor.NO_MODIFIER.equals(modifierString)) {
            modifierString = ""; //$NON-NLS-1$
        }
        fCombiningHover.fModifierString = modifierString;
        //state mask
        fCombiningHover.fStateMask = PyEditorTextHoverDescriptor.computeStateMask(modifierString);
        if (fCombiningHover.fStateMask == -1) {
            // Fallback: use stored modifier masks
            try {
                fCombiningHover.fStateMask = Integer.parseInt(PydevPrefs.getPreferenceStore().getString(
                        PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER_MASK + fCombiningHover.getId()));
            } catch (NumberFormatException ex) {
                fCombiningHover.fStateMask = -1;
            }
            // Fix modifier string
            int stateMask = fCombiningHover.fStateMask;
            if (stateMask == -1) {
                fCombiningHover.fModifierString = ""; //$NON-NLS-1$
            } else {
                fCombiningHover.fModifierString = PyAction.getModifierString(stateMask);
            }
        }
    }

    private void handleModifierModified(Text source) {
        int i = fHoverTable.getSelectionIndex();
        PyEditorTextHoverDescriptor hover = null;
        Text editor = source;
        if (source == fCombiningHoverModifierEditor) {
            hover = PydevPlugin.getCombiningHoverDescriptor();
        } else {
            if (i < 0) {
                return;
            }
            hover = fHoverDescs[i];
        }

        String modifiers = editor.getText();
        hover.fModifierString = modifiers;
        hover.fStateMask = PyEditorTextHoverDescriptor.computeStateMask(modifiers);

        // update table
        if (!fHoverTableViewer.isCellEditorActive() && i >= 0) {
            fHoverTableViewer.refresh(fHoverDescs[i]);
        }

        updateStatus(hover);
    }

    private void handleSetPriority(Integer priority) {
        int i = fHoverTable.getSelectionIndex();
        if (i == -1) {
            return;
        }
        PyEditorTextHoverDescriptor hover = fHoverDescs[i];
        hover.setPriority(priority);

        /**
         * Ensure hover configs and hover descriptors are in same order
         */
        fHoverTableViewer.update(fHoverDescs[i], null);
        sortHoverDescriptors();
        fHoverTableViewer.refresh();

        updateStatus(hover);
    }

    public void handleSetPreempt(Boolean preempt) {
        int i = fHoverTable.getSelectionIndex();
        if (i == -1) {
            return;
        }

        PyEditorTextHoverDescriptor hover = fHoverDescs[i];
        hover.setIsPreempt(preempt);

        // update table
        fHoverTableViewer.refresh(((IStructuredSelection) fHoverTableViewer.getSelection()).getFirstElement());

        updateStatus(hover);
    }

    private void handleHoverListSelection() {
        int i = fHoverTable.getSelectionIndex();

        if (i == -1) {
            if (fHoverTable.getSelectionCount() == 0) {
                fModifierEditor.setEnabled(false);
            }
            return;
        }
        PyEditorTextHoverDescriptor hover = fHoverDescs[i];
        boolean enabled = hover.isEnabled();
        fModifierEditor.setEnabled(enabled && !fCombineHovers.getSelection());
        fModifierEditor.setText(hover.fModifierString);
        String description = fHoverDescs[i].getDescription();
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

    private void updateStatus(PyEditorTextHoverDescriptor hover) {
        if (hover != null && hover.isEnabled() && hover.fStateMask == -1) {
            fStatus = new StatusInfo(IStatus.ERROR,
                    Messages.format(PyEditorMessages.PyEditorHoverConfigurationBlock_modifierIsNotValid,
                            hover.fModifierString));
        } else {
            fStatus = new StatusInfo();
        }

        int i = 0;
        Map<Integer, String> stateMasks = new HashMap<Integer, String>(fHoverDescs.length);
        while (fStatus.isOK() && i < fHoverDescs.length) {
            if (fHoverDescs[i].isEnabled()) {
                String label = fHoverDescs[i].getLabel();
                Integer stateMask = new Integer(fHoverDescs[i].fStateMask);
                if (fHoverDescs[i].fStateMask == -1) {
                    fStatus = new StatusInfo(IStatus.ERROR,
                            Messages.format(
                                    PyEditorMessages.PyEditorHoverConfigurationBlock_modifierIsNotValidForHover,
                                    new String[] { fHoverDescs[i].fModifierString, label }));
                    /* The JDT implementation prohibits duplicate hovers with the same modifier.
                     * We permit duplicates because PyDev hovers have a priority attribute, and the highest
                     * priority hover with a given modifier will be selected at runtime.
                     */
                } else {
                    stateMasks.put(stateMask, label);
                }
            }
            i++;
        }

        fMainPreferencePage.setValid(fStatus.isOK());
        StatusUtil.applyToStatusLine(fMainPreferencePage, fStatus);
    }

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
    @Override
    public void dispose() {
        // nothing to dispose
    }

    public void showColumn(TableColumn column, boolean show) {
        if (column.getWidth() != 0) {
            fColWidthsMap.put(column, column.getWidth());
        }
        if (fColWidthsMap.get(column) != null) {
            column.setWidth(show ? fColWidthsMap.get(column) : 0);
        }
    }

}
