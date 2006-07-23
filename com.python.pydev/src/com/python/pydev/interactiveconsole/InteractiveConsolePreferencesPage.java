/*
 * Created on Mar 18, 2006
 */
package com.python.pydev.interactiveconsole;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.utils.LabelFieldEditor;
import org.python.pydev.utils.MultiStringFieldEditor;

import com.python.pydev.PydevPlugin;

public class InteractiveConsolePreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    public static final String SHOW_CONSOLE_INPUT = "SHOW_CONSOLE_INPUT";
    public static final boolean DEFAULT_SHOW_CONSOLE_INPUT = true;

    public static final String EVAL_ON_NEW_LINE = "EVAL_ON_NEW_LINE";
    public static final boolean DEFAULT_EVAL_ON_NEW_LINE = false;
    
    public static final String INITIAL_INTERPRETER_CMDS = "INITIAL_INTERPRETER_CMDS";
    public static final String DEFAULT_INITIAL_INTERPRETER_CMDS = ""+
    "import sys; sys.ps1=''; sys.ps2=''\r\n"+
    "print >> sys.stderr, 'PYTHONPATH:'\r\n"+
    "for p in sys.path:\r\n"+
    "    print >> sys.stderr,  p\r\n" +
    "\r\n" +                                                //to finish the for scope
    "print >> sys.stderr, 'Ok, all set up... Enjoy'\r\n"+
    "";
    
    public InteractiveConsolePreferencesPage() {
        super(GRID);
        //Set the preference store for the preference page.
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());      
        
    }

    @Override
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new LabelFieldEditor("Interactive_console_note","The console is automatically activated with Ctrl+Alt+Enter in any Pydev Editor.\n\n", p){

            private Label label;

            /**
             * Returns this field editor's label component.
             * <p>
             * The label is created if it does not already exist
             * </p>
             *
             * @param parent the parent
             * @return the label control
             */
            public Label getLabelControl2(Composite parent) {
                if (label == null) {
                    label = new Label(parent, SWT.LEFT);
                    label.setFont(parent.getFont());
                    String text = getLabelText();
                    if (text != null)
                        label.setText("Note:\n\n");
                    label.addDisposeListener(new DisposeListener() {
                        public void widgetDisposed(DisposeEvent event) {
                            label = null;
                        }
                    });
                } else {
                    checkParent(label, parent);
                }
                return label;
            }
            public int getNumberOfControls() {
                return 2;
            }
            protected void doFillIntoGrid(Composite parent, int numColumns) {
                getLabelControl2(parent);
                getLabelControl(parent);
            }

        });
        addField(new BooleanFieldEditor(SHOW_CONSOLE_INPUT, "Show the input given to the console?", p));
        addField(new BooleanFieldEditor(EVAL_ON_NEW_LINE, "Evaluate on console on each new line (or only on request)?", p));
        addField(new MultiStringFieldEditor(INITIAL_INTERPRETER_CMDS, "Initial\ninterpreter\ncommands:\n", p));
    }

    public void init(IWorkbench workbench) {
    }

    /**
     * should we show the inputs that are given to the console?
     */
    public static boolean showConsoleInput() {
        return PydevPlugin.getDefault().getPreferenceStore().getBoolean(SHOW_CONSOLE_INPUT);
    }
    
    /**
     * should we evaluate on each new line or only on request?
     */
    public static boolean evalOnNewLine() {
        return PydevPlugin.getDefault().getPreferenceStore().getBoolean(EVAL_ON_NEW_LINE);
    }
    
    /**
     * The initial commonds to pass to the interpreter
     */
    public static String getInitialInterpreterCmds() {
        return PydevPlugin.getDefault().getPreferenceStore().getString(INITIAL_INTERPRETER_CMDS);
    }

}
