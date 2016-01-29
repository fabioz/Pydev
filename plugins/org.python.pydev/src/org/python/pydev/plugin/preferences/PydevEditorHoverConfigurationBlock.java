package org.python.pydev.plugin.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
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

    // Data structure to hold the values which are edited by the user
    private static class HoverConfig {

        private String fModifierString;
        private boolean fIsEnabled;
        private int fStateMask;

        private HoverConfig(String modifier, int stateMask, boolean enabled) {
            fModifierString = modifier;
            fIsEnabled = enabled;
            fStateMask = stateMask;
        }
    }

    private class JavaEditorTextHoverDescriptorLabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            switch (columnIndex) {
                case ENABLED_PROP:
                    return ((PyEditorTextHoverDescriptor) element).getLabel();

                case MODIFIER_PROP:
                    TableItem item = (TableItem) fHoverTableViewer.testFindItem(element);
                    int index = fHoverTable.indexOf(item);
                    return fHoverConfigs[index].fModifierString;

                default:
                    break;
            }

            return null;
        }

        public void addListener(ILabelProviderListener listener) {
        }

        public void dispose() {
        }

        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
        }
    }

    private class JavaEditorTextHoverDescriptorContentProvider implements IStructuredContentProvider {

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // Do nothing since the viewer listens to resource deltas
        }

        public void dispose() {
        }

        public Object[] getElements(Object element) {
            return (Object[]) element;
        }
    }

    private OverlayPreferenceStore fStore;
    private HoverConfig[] fHoverConfigs;
    private Text fModifierEditor;
    private Table fHoverTable;
    private TableViewer fHoverTableViewer;
    private TableColumn fNameColumn;
    private TableColumn fModifierColumn;
    private Text fDescription;

    private PreferencePage fMainPreferencePage;

    private StatusInfo fStatus;

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

    public PydevEditorHoverConfigurationBlock(PreferencePage mainPreferencePage, OverlayPreferenceStore store) {
        Assert.isNotNull(mainPreferencePage);
        Assert.isNotNull(store);
        fMainPreferencePage = mainPreferencePage;
        fStore = store;
        fStore.addKeys(createOverlayStoreKeys());
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
    public Control createControl(Composite parent) {

        ScrolledPageContent scrolled = new ScrolledPageContent(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);

        Composite hoverComposite = new Composite(scrolled, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        hoverComposite.setLayout(layout);

        String rollOverLabel = PyEditorMessages.PyEditorHoverConfigurationBlock_annotationRollover;
        addCheckBox(hoverComposite, rollOverLabel, PyHoverPreferencesPage.EDITOR_ANNOTATION_ROLL_OVER, 0);

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
                SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
        fHoverTable.setHeaderVisible(true);
        fHoverTable.setLinesVisible(true);

        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = SWTUtil.getTableHeightHint(fHoverTable, 10);
        gd.horizontalSpan = 2;
        gd.widthHint = new PixelConverter(parent).convertWidthInCharsToPixels(30);
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

        fNameColumn = new TableColumn(fHoverTable, SWT.NONE);
        fNameColumn.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_nameColumnTitle);
        fNameColumn.setResizable(true);

        fModifierColumn = new TableColumn(fHoverTable, SWT.NONE);
        fModifierColumn.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_modifierColumnTitle);
        fModifierColumn.setResizable(true);

        fHoverTableViewer = new CheckboxTableViewer(fHoverTable);
        fHoverTableViewer.setUseHashlookup(true);
        fHoverTableViewer.setContentProvider(new JavaEditorTextHoverDescriptorContentProvider());
        fHoverTableViewer.setLabelProvider(new JavaEditorTextHoverDescriptorLabelProvider());

        ((CheckboxTableViewer) fHoverTableViewer).addCheckStateListener(new ICheckStateListener() {
            /*
             * @see org.eclipse.jface.viewers.ICheckStateListener#checkStateChanged(org.eclipse.jface.viewers.CheckStateChangedEvent)
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

        // Text field for modifier string
        label = new Label(hoverComposite, SWT.LEFT);
        label.setText(PyEditorMessages.PyEditorHoverConfigurationBlock_keyModifier);
        fModifierEditor = new Text(hoverComposite, SWT.BORDER);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        fModifierEditor.setLayoutData(gd);

        fModifierEditor.addKeyListener(new KeyListener() {
            private boolean isModifierCandidate;

            public void keyPressed(KeyEvent e) {
                isModifierCandidate = e.keyCode > 0 && e.character == 0 && e.stateMask == 0;
            }

            public void keyReleased(KeyEvent e) {
                if (isModifierCandidate && e.stateMask > 0 && e.stateMask == e.stateMask && e.character == 0) {// && e.time -time < 1000) {
                    String text = fModifierEditor.getText();
                    Point selection = fModifierEditor.getSelection();
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
                        fModifierEditor.insert(insertString);
                    }
                }
            }
        });

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

        initialize();

        scrolled.setContent(hoverComposite);
        final Point size = hoverComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        scrolled.setMinSize(size.x, size.y);

        Dialog.applyDialogFont(scrolled);

        return scrolled;

    }

    private void addColumnLayoutData(TableLayoutComposite layouter) {
        layouter.addColumnData(new ColumnWeightData(40, true));
        layouter.addColumnData(new ColumnWeightData(60, true));
    }

    private PyEditorTextHoverDescriptor[] getContributedHovers() {
        return PydevPlugin.getDefault().getPyEditorTextHoverDescriptors();
    }

    public void initialize() {
        PyEditorTextHoverDescriptor[] hoverDescs = getContributedHovers();
        fHoverConfigs = new HoverConfig[hoverDescs.length];
        for (int i = 0; i < hoverDescs.length; i++) {
            fHoverConfigs[i] = new HoverConfig(hoverDescs[i].getModifierString(), hoverDescs[i].getStateMask(),
                    hoverDescs[i].isEnabled());
        }

        fHoverTableViewer.setInput(hoverDescs);

        initializeFields();
    }

    void initializeFields() {
        fModifierEditor.setEnabled(false);

        Iterator<Button> e = fCheckBoxes.keySet().iterator();
        while (e.hasNext()) {
            Button b = e.next();
            String key = fCheckBoxes.get(b);
            b.setSelection(fStore.getBoolean(key));
        }

        for (int i = 0; i < fHoverConfigs.length; i++) {
            fHoverTable.getItem(i).setChecked(fHoverConfigs[i].fIsEnabled);
        }
        fHoverTableViewer.refresh();
    }

    public void performOk() {
        StringBuffer buf = new StringBuffer();
        StringBuffer maskBuf = new StringBuffer();
        for (int i = 0; i < fHoverConfigs.length; i++) {
            buf.append(getContributedHovers()[i].getId());
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

            maskBuf.append(getContributedHovers()[i].getId());
            maskBuf.append(PyEditorTextHoverDescriptor.VALUE_SEPARATOR);
            maskBuf.append(fHoverConfigs[i].fStateMask);
            maskBuf.append(PyEditorTextHoverDescriptor.VALUE_SEPARATOR);
        }
        fStore.setValue(PyHoverPreferencesPage.EDITOR_TEXT_HOVER_MODIFIERS, buf.toString());
        fStore.setValue(PyHoverPreferencesPage.EDITOR_TEXT_HOVER_MODIFIER_MASKS, maskBuf.toString());

        PydevPlugin.getDefault().resetPyEditorTextHoverDescriptors();
    }

    public void performDefaults() {
        restoreFromPreferences();
        initializeFields();
        updateStatus(null);
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

        String compiledTextHoverModifierMasks = PydevPlugin.getDefault().getPreferenceStore()
                .getString(PyHoverPreferencesPage.EDITOR_TEXT_HOVER_MODIFIER_MASKS);

        tokenizer = new StringTokenizer(compiledTextHoverModifierMasks, PyEditorTextHoverDescriptor.VALUE_SEPARATOR);
        HashMap<String, String> idToModifierMask = new HashMap<String, String>(tokenizer.countTokens() / 2);

        while (tokenizer.hasMoreTokens()) {
            String id = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens()) {
                idToModifierMask.put(id, tokenizer.nextToken());
            }
        }

        for (int i = 0; i < fHoverConfigs.length; i++) {
            String modifierString = idToModifier.get(getContributedHovers()[i].getId());
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
                            .parseInt(idToModifierMask.get(getContributedHovers()[i].getId()));
                } catch (NumberFormatException ex) {
                    fHoverConfigs[i].fStateMask = -1;
                }
            }
        }
    }

    private void handleModifierModified() {
        int i = fHoverTable.getSelectionIndex();
        if (i == -1) {
            return;
        }

        String modifiers = fModifierEditor.getText();
        fHoverConfigs[i].fModifierString = modifiers;
        fHoverConfigs[i].fStateMask = PyEditorTextHoverDescriptor.computeStateMask(modifiers);

        // update table
        fHoverTableViewer.refresh(getContributedHovers()[i]);

        updateStatus(fHoverConfigs[i]);
    }

    private void handleHoverListSelection() {
        int i = fHoverTable.getSelectionIndex();

        if (i == -1) {
            if (fHoverTable.getSelectionCount() == 0) {
                fModifierEditor.setEnabled(false);
            }
            return;
        }

        boolean enabled = fHoverConfigs[i].fIsEnabled;
        fModifierEditor.setEnabled(enabled);
        fModifierEditor.setText(fHoverConfigs[i].fModifierString);
        String description = getContributedHovers()[i].getDescription();
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
                } else if (stateMasks.containsKey(stateMask)) {
                    fStatus = new StatusInfo(IStatus.ERROR,
                            Messages.format(PyEditorMessages.PyEditorHoverConfigurationBlock_duplicateModifier,
                                    new String[] { label, stateMasks.get(stateMask) }));
                } else {
                    stateMasks.put(stateMask, label);
                }
            }
            i++;
        }

        fMainPreferencePage.setValid(fStatus.isOK());
        StatusUtil.applyToStatusLine(fMainPreferencePage, fStatus);
    }

    private Button addCheckBox(Composite parent, String label, String key, int indentation) {
        Button checkBox = new Button(parent, SWT.CHECK);
        checkBox.setText(label);

        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalIndent = indentation;
        gd.horizontalSpan = 2;
        checkBox.setLayoutData(gd);
        checkBox.addSelectionListener(fCheckBoxListener);

        fCheckBoxes.put(checkBox, key);

        return checkBox;
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
    public void dispose() {
        // nothing to dispose
    }
}
