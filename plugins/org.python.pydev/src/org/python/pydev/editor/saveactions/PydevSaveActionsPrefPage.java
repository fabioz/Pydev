package org.python.pydev.editor.saveactions;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.SystemUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.tooltips.presenter.AbstractTooltipInformationPresenter;
import org.python.pydev.core.tooltips.presenter.ToolTipPresenterHandler;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.utils.LinkFieldEditor;


/**
 * Preference page for Pydev editor {@code Save Actions}.
 * Save actions are actions performed on file buffers whenever
 * a file resource is saved.
 * 
 * @author André Berg
 * @version 0.1
 */
public class PydevSaveActionsPrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private class PydevSaveActionsPageLinkListener implements SelectionListener {

        public PydevSaveActionsPageLinkListener() {
        }

        public void widgetSelected(SelectionEvent e) {
            try {
                URL url = new URL("http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html");
                SystemUtils.openWebpageInEclipse(url, "SimpleDateFormat Java Docs");
            } catch (MalformedURLException e1) {
                Log.log(e1.getMessage());
            }
        }

        public void widgetDefaultSelected(SelectionEvent e) {
        }

    }

    private StringFieldEditor dateFormatEditor;
    private PydevDateFieldNameEditor fieldNameEditor;
    private BooleanFieldEditor enableDateFieldActionEditor;
    private LinkFieldEditor dateFormatHelpLinkEditor;
    
    private static final String enableDateFieldActionEditorTooltipFormat = ""+
            "Parses the file being saved for a module level\n" +
            "field with name and value as defined by the custom\n" +
            "name and date format below and updates it to the\n" +
            "current date.";
    
    private final IPreferenceStore prefStore;
    private ToolTipPresenterHandler tooltipPresenter;

    
    public PydevSaveActionsPrefPage() {
        super(GRID);
        final IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        prefStore = store;
        setDescription("Save actions are run whenever a file is saved.\n");
        setPreferenceStore(store);
        setDefaults();
    }

    private void setDefaults() {
        prefStore.setDefault(ENABLE_DATE_FIELD_ACTION, DEFAULT_ENABLE_DATE_FIELD_ACTION);
        prefStore.setDefault(DATE_FIELD_NAME, DEFAULT_DATE_FIELD_NAME);
        prefStore.setDefault(DATE_FIELD_FORMAT, DEFAULT_DATE_FIELD_FORMAT);
    }

    public static final String ENABLE_DATE_FIELD_ACTION = "ENABLE_DATE_FIELD_ACTION";
    public static final boolean DEFAULT_ENABLE_DATE_FIELD_ACTION = false;

    public static final String DATE_FIELD_FORMAT = "DATE_FIELD_FORMAT";
    public static final String DEFAULT_DATE_FIELD_FORMAT = "yyyy-MM-dd";
    
    public static final String DATE_FIELD_NAME = "DATE_FIELD_NAME";
    public static final String DEFAULT_DATE_FIELD_NAME = "__updated__";

    @Override
    protected void createFieldEditors() {
        
        IInformationPresenter presenter = new AbstractTooltipInformationPresenter() {
            @Override
            protected void onUpdatePresentation(String hoverInfo, TextPresentation presentation) {
            }
            @Override
            protected void onHandleClick(Object data) {
            }
        };

        final Composite p = getFieldEditorParent();
        tooltipPresenter = new ToolTipPresenterHandler(p.getShell(), presenter,
                "Tip: Click link to open SimpleDateFormat Java docs online.");
        
        // Enable date field action editor (boolean)
        
        final String fieldName = prefStore.getString(DATE_FIELD_NAME);
        final String enableDateFieldActionEditorTooltip = 
                String.format(enableDateFieldActionEditorTooltipFormat, fieldName);
        
        enableDateFieldActionEditor = 
                new BooleanFieldEditor(ENABLE_DATE_FIELD_ACTION, "Update date field?", p);
        
        enableDateFieldActionEditor.getDescriptionControl(p).setToolTipText(enableDateFieldActionEditorTooltip);
        addField(enableDateFieldActionEditor);
        
        // Date field name editor (string)
        
        fieldNameEditor = new PydevDateFieldNameEditor(DATE_FIELD_NAME, "Date field name:", PydevDateFieldNameEditor.UNLIMITED, p);
        fieldNameEditor.getTextControl(p).setToolTipText(String.format("Default is %s", DEFAULT_DATE_FIELD_NAME));
        fieldNameEditor.setEmptyStringAllowed(false);
        //fieldNameEditor.setValidateStrategy(PydevDateFieldNameEditor.VALIDATE_ON_FOCUS_LOST);
        fieldNameEditor.setEnabled(prefStore.getBoolean(ENABLE_DATE_FIELD_ACTION), p);
        addField(fieldNameEditor);
        
        // Date format editor (string)
        
        dateFormatEditor = new StringFieldEditor(DATE_FIELD_FORMAT, "Date field format:", StringFieldEditor.UNLIMITED, p);
        dateFormatEditor.getTextControl(p).setToolTipText("Uses Java's SimpleDateFormat tokens.");
        dateFormatEditor.setEmptyStringAllowed(false);
        //dateFormatEditor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_FOCUS_LOST);
        dateFormatEditor.setEnabled(prefStore.getBoolean(ENABLE_DATE_FIELD_ACTION), p);
        addField(dateFormatEditor);
        
        // Token help editor (link)
        
        final String dateFormatHelpLinkTooltip = "" +
        		"All tokens from Java's SimpleDateFormat class\n" +
        		"are supported. The most common ones are:\n" +
        		"\n" +
        		"y\t\tYear\n" +
        		"M\t\tMonth in year\n" +
        		"d\t\tDay in month\n" +
        		"E\t\tDay name in week\n" +
        		"H\t\tHour in day (0-23)\n" +
        		"h\t\tHour in am/pm (1-12)\n" +
        		"m\t\tMinute in hour\n" +
        		"s\t\tSecond in minute\n" +
        		"\n" +
        		"Enclose literal characters in single quotes.";
                
        dateFormatHelpLinkEditor = 
                new LinkFieldEditor("link_dateFormat", "<a>Supported tokens</a>", p, 
                        new PydevSaveActionsPrefPage.PydevSaveActionsPageLinkListener(), 
                            dateFormatHelpLinkTooltip, tooltipPresenter);
        addField(dateFormatHelpLinkEditor);
        
    }

    public void init(IWorkbench workbench) {
    }
        
    public static boolean getDateFieldActionEnabled() {
        return PydevPrefs.getPreferences().getBoolean(ENABLE_DATE_FIELD_ACTION);
    }
    
    public static String getDateFieldName() {
        final String fieldName = PydevPrefs.getPreferences().getString(DATE_FIELD_NAME);
        if (fieldName.isEmpty()) {
            return DEFAULT_DATE_FIELD_NAME;
        }
        return fieldName;
    }

    public static String getDateFieldFormat() {
        final String fieldName = PydevPrefs.getPreferences().getString(DATE_FIELD_FORMAT);
        if (fieldName.isEmpty()) {
            return DEFAULT_DATE_FIELD_FORMAT;
        }
        return fieldName;    }

    protected void performDefaults() {
        final Composite p = getFieldEditorParent();
        enableDateFieldActionEditor.loadDefault();
        dateFormatEditor.loadDefault();
        fieldNameEditor.loadDefault();
        dateFormatEditor.setEnabled(false, p);
        fieldNameEditor.setEnabled(false, p);
        super.updateApplyButton();
    }
    
    private void updateDateFieldStringEditorState() {
        final boolean val = enableDateFieldActionEditor.getBooleanValue();
        final Composite p = getFieldEditorParent();
        dateFormatEditor.setEnabled(val, p);
        fieldNameEditor.setEnabled(val, p);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (enableDateFieldActionEditor.equals(event.getSource())) {
            updateDateFieldStringEditorState();
        }
        setValid((dateFormatEditor.isValid() && fieldNameEditor.isValid()));
        updatePageButtons();
    }

    private void updatePageButtons() {
        final boolean valid = isValid();
        final Button defaultButton = getShell().getDefaultButton();
        if (!valid) {
            getApplyButton().setEnabled(false);
            if (defaultButton != null) {
                defaultButton.setEnabled(false);
            }
        } else {
            getApplyButton().setEnabled(true);
            if (defaultButton != null) {
                defaultButton.setEnabled(true);
            }
        }
    }
}
