package org.python.pydev.shared_ui.field_editors;

import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.json.eclipsesource.JsonValue;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.utils.RunInUiThread;

public class JsonFieldEditor extends FieldEditor {

    /**
     * Text limit constant (value <code>-1</code>) indicating unlimited
     * text limit and width.
     */
    public static int UNLIMITED = -1;

    /**
     * Cached valid state.
     */
    private boolean isValid;

    /**
     * Old valid state
     */
    private boolean oldState;

    /**
     * Old text value.
     * @since 3.4 this field is protected.
     */
    protected String oldValue;

    /**
     * The text field, or <code>null</code> if none.
     */
    StyledText textField;

    /**
     * Height of text field in characters; initially 1.
     */
    private int heigthInChars = 80;

    /**
     * Width of text field in characters; initially unlimited.
     */
    private int widthInChars = UNLIMITED;

    /**
     * Text limit of text field in characters; initially unlimited.
     */
    private int textLimit = UNLIMITED;

    /**
     * The error message, or <code>null</code> if none.
     */
    private String errorMessage;

    /**
     * Store an additional JSON validation strategy callback.
     * 
     * <p>
     * Callback return type is Optional String.
     * Returns <code>Optional.empty()</code> if JSON is valid or returns an Optional String error message.
     * </p>
     */
    private ICallback<Optional<String>, JsonValue> additionalValidation = null;

    /**
     * The JsonFieldValidation instance
     */
    private final JsonFieldValidation fieldValidation = new JsonFieldValidation("JsonFieldValidation");

    /**
     * 
     * Create a Job that can check whether some content is valid by scheduling.
     *
     */
    private class JsonFieldValidation extends Job {
        /**
         * The default waiting time for the Job to run.
         */
        final private long scheduleDelay = 500;

        /**
         * The content that going to be checked.
         */
        private String content;

        /**
         * The error line and message keeper.
         */
        private Tuple<Integer, String> jsonError;

        public JsonFieldValidation(String name) {
            super(name);
        }

        /**
         * It schedule a string content validation check after <code>long scheduleDelay</code> default wait time.
         * 
         * @param content is the String that will get it's validation checked.
         */
        public void scheduleValidation(String content) {
            this.content = content;
            this.schedule(scheduleDelay);
        }

        /**
         * Checks whether the <code>String content</code> contains a valid JSON value, 
         * updating <code>Tuple jsonError</code>.
         */
        private void refreshState() {
            if (content == null) {
                jsonError = new Tuple<Integer, String>(-1, "JsonFieldEditor did not load properly.");
                return;
            }

            if (!checkTextFieldJSON()) {
                return;
            }

            if (additionalValidation != null) {
                Optional<String> ret = additionalValidation.call(JsonValue.readFrom(content));
                if (ret.isPresent()) {
                    jsonError = new Tuple<Integer, String>(-1, ret.get());
                }
            }
        }

        /**
         * Checks whether the <code>content</code> contains a valid JSON.
         * <p>
         * It stores the <code>getJSONError(String)</code> in field <code>Tuple jsonError</code>.
         * </p>
         *
         * @return <code>true</code> if the content is valid,
         *   and <code>false</code> if it is invalid.
         */
        private boolean checkTextFieldJSON() {
            jsonError = getJSONError(content);
            if (jsonError == null) {
                return true;
            }
            return false;
        }

        /**
         * @param str string to validates JSON.
         * @return returns <code>Tuple<Integer, Integer>(line,column)</code> 
         * specifying that JSON has an error and it is in returned line and column,
         *   and <code>null</code> if JSON is valid
         */
        private Tuple<Integer, String> getJSONError(String str) {
            try {
                JsonValue.readFrom(str);
            } catch (org.python.pydev.json.eclipsesource.ParseException e) {
                return new Tuple<Integer, String>(e.getLine(), e.getMessage());
            }
            return null;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (textField == null || textField.isDisposed()) {
                return null;
            }
            refreshState();
            RunInUiThread.async(new Runnable() {
                @Override
                public void run() {
                    if (textField == null || textField.isDisposed()) {
                        return;
                    }
                    handleError(content, jsonError);
                }
            });
            return new Status(IStatus.OK, this.getClass(), "JsonField validated.");
        }
    }

    /**
     * Creates a new string field editor
     */
    protected JsonFieldEditor() {
    }

    /**
     * Creates a JSON field editor. Use the method <code>setTextLimit</code> to
     * limit the text.
     *
     * @param name          the name of the preference this field editor works on
     * @param labelText     the label text of the field editor
     * @param widthInChars  the width of the text input field in characters, or
     *                      <code>UNLIMITED</code> for no limit
     * @param heigthInChars the height of the text input field in characters.
     * @param parent        the parent of the field editor's control
     * @since 3.17
     */
    public JsonFieldEditor(String name, String labelText, int widthInChars, int heigthInChars,
            Composite parent) {
        init(name, labelText);
        this.widthInChars = widthInChars;
        this.heigthInChars = heigthInChars;
        isValid = false;
        oldState = false;
        createControl(parent);
    }

    /**
     * Creates a JSON field editor.
     * Use the method <code>setTextLimit</code> to limit the text.
     *
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param width the width of the text input field in characters,
     *  or <code>UNLIMITED</code> for no limit
     * @param parent the parent of the field editor's control
     */
    public JsonFieldEditor(String name, String labelText, int width,
            Composite parent) {
        this(name, labelText, width, 20, parent);
    }

    /**
     * Creates a JSON field editor of unlimited width.
     * Use the method <code>setTextLimit</code> to limit the text.
     *
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    public JsonFieldEditor(String name, String labelText, Composite parent) {
        this(name, labelText, UNLIMITED, parent);
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        GridData gd = (GridData) textField.getLayoutData();
        gd.horizontalSpan = numColumns - 1;
        // We only grab excess space if we have to
        // If another field editor has more columns then
        // we assume it is setting the width.
        gd.grabExcessHorizontalSpace = gd.horizontalSpan == 1;
    }

    /**
     * @param additionalValidation is used to set up a 
     * callback that will return an Optional String to point out errors in the JSON input.
     * <p>
     * Callback must return <code>Optional.empty()</code> if JSON is valid 
     * or return an Optional String error message if it is invalid.
     * </p>
     */
    public void setAdditionalJsonValidation(ICallback<Optional<String>, JsonValue> additionalValidation) {
        this.additionalValidation = additionalValidation;
    }

    /**
     * Fills this field editor's basic controls into the given parent.
     * <p>
     * The string field implementation of this <code>FieldEditor</code>
     * framework method contributes the text field. Subclasses may override
     * but must call <code>super.doFillIntoGrid</code>.
     * </p>
     */
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        getLabelControl(parent);

        textField = getTextControl(parent);
        GridData gd = new GridData();
        gd.horizontalSpan = numColumns - 1;
        if (widthInChars != UNLIMITED || heigthInChars > 1) {
            GC gc = new GC(textField);
            try {
                if (widthInChars != UNLIMITED) {
                    Point extent = gc.textExtent("X");//$NON-NLS-1$
                    gd.widthHint = widthInChars * extent.x;
                } else {
                    gd.horizontalAlignment = GridData.FILL;
                    gd.grabExcessHorizontalSpace = true;
                }
                if (heigthInChars > 1) {
                    gd.heightHint = heigthInChars * gc.getFontMetrics().getHeight();
                }
            } finally {
                gc.dispose();
            }
        } else {
            gd.horizontalAlignment = GridData.FILL;
            gd.grabExcessHorizontalSpace = true;
        }
        textField.setLayoutData(gd);
    }

    @Override
    protected void doLoad() {
        if (textField != null) {
            String value = getPreferenceStore().getString(getPreferenceName());
            textField.setText(value);
            oldValue = value;
        }
    }

    @Override
    protected void doLoadDefault() {
        if (textField != null) {
            String value = getPreferenceStore().getDefaultString(
                    getPreferenceName());
            textField.setText(value);
        }
        valueChanged();
    }

    @Override
    protected void doStore() {
        getPreferenceStore().setValue(getPreferenceName(), textField.getText());
    }

    /**
     * Returns the error message that will be displayed when and if
     * an error occurs.
     *
     * @return the error message, or <code>null</code> if none
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public int getNumberOfControls() {
        return 2;
    }

    /**
     * Returns the field editor's value.
     *
     * @return the current value
     */
    public String getStringValue() {
        if (textField != null) {
            return textField.getText();
        }

        return getPreferenceStore().getString(getPreferenceName());
    }

    /**
     * Returns this field editor's text control.
     *
     * @return the text control, or <code>null</code> if no
     * text field is created yet
     */
    protected StyledText getTextControl() {
        return textField;
    }

    /**
     * Returns this field editor's text control.
     * <p>
     * The control is created if it does not yet exist
     * </p>
     *
     * @param parent the parent
     * @return the text control
     */
    public StyledText getTextControl(Composite parent) {
        if (textField == null) {
            textField = createTextWidget(parent);
            textField.setFont(parent.getFont());
            textField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    valueChanged();
                }
            });
            textField.addFocusListener(new FocusAdapter() {
                // Ensure that the value is checked on focus loss in case we
                // missed a keyRelease or user hasn't released key.
                // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=214716
                @Override
                public void focusLost(FocusEvent e) {
                    valueChanged();
                }
            });
            textField.addDisposeListener(event -> textField = null);
            if (textLimit > 0) {//Only set limits above 0 - see SWT spec
                textField.setTextLimit(textLimit);
            }
        } else {
            checkParent(textField, parent);
        }
        return textField;
    }

    /**
     * Create the text widget.
     *
     * @param parent the parent composite
     * @return The widget
     * @since 3.17
     */
    protected StyledText createTextWidget(Composite parent) {
        if (heigthInChars > 1) {
            return new StyledText(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        }
        return new StyledText(parent, SWT.SINGLE | SWT.BORDER);
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    public boolean isValidToApply() {
        while (fieldValidation.getState() != Job.NONE) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.log(e);
                return false;
            }
        }
        return isValid;
    }

    /**
     * Sets the error message that will be displayed when and if
     * an error occurs.
     *
     * @param message the error message
     */
    public void setErrorMessage(String message) {
        errorMessage = message;
    }

    @Override
    public void setFocus() {
        if (textField != null) {
            textField.setFocus();
        }
    }

    /**
     * Sets this field editor's value.
     *
     * @param value the new value, or <code>null</code> meaning the empty string
     */
    public void setStringValue(String value) {
        if (textField != null) {
            if (value == null) {
                value = "";//$NON-NLS-1$
            }
            oldValue = textField.getText();
            if (!oldValue.equals(value)) {
                textField.setText(value);
                valueChanged();
            }
        }
    }

    /**
     * Sets this text field's text limit.
     *
     * @param limit the limit on the number of character in the text
     *  input field, or <code>UNLIMITED</code> for no limit
    
     */
    public void setTextLimit(int limit) {
        textLimit = limit;
        if (textField != null) {
            textField.setTextLimit(limit);
        }
    }

    /**
     * Shows the error message set via <code>setErrorMessage</code>.
     */
    public void showErrorMessage() {
        showErrorMessage(errorMessage);
    }

    /**
     * Informs this field editor's listener, if it has one, about a change
     * to the value (<code>VALUE</code> property) provided that the old and
     * new values are different.
     * <p>
     * This hook is <em>not</em> called when the text is initialized
     * (or reset to the default value) from the preference store.
     * </p>
     */
    protected void valueChanged() {
        setPresentsDefaultValue(false);
        isValid = false;
        fieldValidation.scheduleValidation(textField.getText());
    }

    /*
     * @see FieldEditor.setEnabled(boolean,Composite).
     */
    @Override
    public void setEnabled(boolean enabled, Composite parent) {
        super.setEnabled(enabled, parent);
        getTextControl(parent).setEnabled(enabled);
    }

    /**
     * Checks if the content and it's validity got updated and then updates the PreferencePage controls.
     * @param content
     */
    private void handleStateChange(String content) {
        if (isValid != oldState) {
            fireStateChanged(IS_VALID, oldState, isValid);
        }
        String newValue = content;
        if (!content.equals(oldValue)) {
            fireValueChanged(VALUE, oldValue, newValue);
            oldValue = newValue;
        }
    }

    /**
     * @param content
     * @param jsonError
     */
    private void handleError(String content, Tuple<Integer, String> jsonError) {
        textField.setStyleRange(null);
        if (jsonError == null) {
            isValid = true;
            clearErrorMessage();
        } else {
            int line = jsonError.o1;
            if (line != -1) {
                PySelection sel = new PySelection(new Document(content));
                int errorLine = line - 1;
                StyleRange styleRange = new StyleRange();
                styleRange.start = sel.getLineOffset(errorLine);
                styleRange.length = sel.getLine(errorLine).length();
                styleRange.fontStyle = SWT.BOLD;
                styleRange.foreground = this.textField.getDisplay().getSystemColor(SWT.COLOR_RED);
                textField.setStyleRange(styleRange);
            }
            setErrorMessage(jsonError.o2);
            showErrorMessage();
        }
        handleStateChange(content);
        oldState = isValid;
    }

}
