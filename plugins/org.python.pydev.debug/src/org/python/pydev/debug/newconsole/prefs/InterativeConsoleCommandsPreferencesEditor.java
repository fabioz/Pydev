/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.prefs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.preferences.IScopedPreferences;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;
import org.python.pydev.shared_ui.dialogs.DialogHelpers;
import org.yaml.snakeyaml.Yaml;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class InterativeConsoleCommandsPreferencesEditor {

    private IScopedPreferences scopedPreferences;
    private Combo combo;
    private final Map<String, InteractiveConsoleCommand> nameToCommand = new HashMap<String, InteractiveConsoleCommand>();
    private Text textCommand;
    private Text textKeybinding;
    private Label errorLabel;
    private Color red;
    private ModifyListener textKeybindingListener;
    private ModifyListener textCommandListener;

    public InterativeConsoleCommandsPreferencesEditor() {
        this.scopedPreferences = InteractiveConsoleCommand.getScopedPreferences();
    }

    public Combo getCombo() {
        return combo;
    }

    public Control createContents(Composite parent) {
        parent = new Composite(parent, SWT.FLAT);
        parent.setLayout(new GridLayout(4, false));

        Label label = new Label(parent, SWT.NONE);
        label.setLayoutData(createGridData());
        label.setText("Command");

        combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setLayoutData(createComboGridData());

        final Button button = new Button(parent, SWT.PUSH);
        button.setLayoutData(createGridData());
        button.setText("Add");

        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String[] items = combo.getItems();
                final Set<String> set = new HashSet<>(Arrays.asList(items));
                IInputValidator validator = new IInputValidator() {

                    @Override
                    public String isValid(String newText) {
                        if (newText.length() == 0) {
                            return "At least 1 char must be provided.";
                        }
                        if (set.contains(newText)) {
                            return "A command named: " + newText + " already exists.";
                        }
                        return null;
                    }
                };
                String name = DialogHelpers.openInputRequest("Command name", "Please enter the name of the command",
                        button.getShell(), validator);
                if (name != null && name.length() > 0) {
                    InteractiveConsoleCommand cmd = new InteractiveConsoleCommand(name);
                    addCommand(cmd);
                    comboSelectionChanged();
                }
            }
        });

        Button buttonRemove = new Button(parent, SWT.PUSH);
        buttonRemove.setLayoutData(createGridData());
        buttonRemove.setText("Remove");
        buttonRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                removeSelectedCommand();
                comboSelectionChanged();
            }
        });

        label = new Label(parent, SWT.NONE);
        label.setLayoutData(GridDataFactory.fillDefaults().span(1, 1).create());
        label.setText("Keybinding");

        textKeybinding = new Text(parent, SWT.SINGLE | SWT.BORDER);
        textKeybinding.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());
        textKeybindingListener = new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                String comboText = combo.getText();
                InteractiveConsoleCommand interactiveConsoleCommand = nameToCommand.get(comboText);
                if (interactiveConsoleCommand == null) {
                    Log.log("Expected a command to be bound to: " + comboText);
                    return;
                }
                validateAndSetKeybinding(comboText, interactiveConsoleCommand);
            }
        };

        label = new Label(parent, SWT.NONE);
        label.setLayoutData(GridDataFactory.fillDefaults().span(4, 1).create());
        label.setText("Command text.\n\n${text} is replaced by the currently selected text\nor the full line if no text is selected.");

        textCommand = new Text(parent, SWT.MULTI | SWT.BORDER);
        textCommand.setLayoutData(createTextGridData());
        textCommandListener = new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {

                String comboText = combo.getText();
                InteractiveConsoleCommand interactiveConsoleCommand = nameToCommand.get(comboText);
                if (interactiveConsoleCommand == null) {
                    Log.log("Expected a command to be bound to: " + comboText);
                    return;
                }
                interactiveConsoleCommand.commandText = textCommand.getText();
            }
        };

        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                comboSelectionChanged();
            }
        });

        errorLabel = new Label(parent, SWT.NONE);
        errorLabel.setLayoutData(GridDataFactory.fillDefaults().span(4, 1).create());
        errorLabel
                .setText("Command text.\n\n${text} is replaced by the currently selected text\nor the full line if no text is selected.");
        errorLabel.setVisible(false);

        red = new Color(Display.getCurrent(), 255, 0, 0);
        errorLabel.setForeground(red);

        this.loadCommands();

        addTextListeners();
        return parent;
    }

    private boolean registered = false;

    private void addTextListeners() {
        if (!registered) {
            textKeybinding.addModifyListener(textKeybindingListener);
            textCommand.addModifyListener(textCommandListener);
            registered = true;
        }
    }

    private void removeTextListeners() {
        if (registered) {
            textKeybinding.removeModifyListener(textKeybindingListener);
            textCommand.removeModifyListener(textCommandListener);
            registered = false;
        }
    }

    protected void showKeybindingError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.getParent().layout(true);
    }

    protected void showKeybindingError(Exception e1) {
        showKeybindingError("" + e1.getMessage());
    }

    protected void hideKeybindingError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.getParent().layout(true);
    }

    protected void comboSelectionChanged() {
        removeTextListeners();
        try {
            String text = combo.getText();
            InteractiveConsoleCommand interactiveConsoleCommand = this.nameToCommand.get(text);
            if (interactiveConsoleCommand == null) {
                textKeybinding.setText("");
                textKeybinding.setEnabled(false);

                textCommand.setText("");
                textCommand.setEnabled(false);

            } else {
                textKeybinding.setText(interactiveConsoleCommand.keybinding);
                textKeybinding.setEnabled(true);

                textCommand.setText(interactiveConsoleCommand.commandText);
                textCommand.setEnabled(true);
            }

            if (interactiveConsoleCommand != null) {
                validateAndSetKeybinding(text, interactiveConsoleCommand);
            } else {
                hideKeybindingError();
            }

        } finally {
            addTextListeners();
        }

    }

    public void clearCommands() {
        this.combo.setItems(new String[0]);
        this.nameToCommand.clear();
        comboSelectionChanged();
    }

    public void loadCommands() {
        clearCommands();
        List<InteractiveConsoleCommand> loadExistingCommands = InteractiveConsoleCommand.loadExistingCommands();
        for (InteractiveConsoleCommand command : loadExistingCommands) {
            this.addCommand(command);
        }
        comboSelectionChanged();
    }

    private Object createTextGridData() {
        GridData data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        data.horizontalSpan = 4;
        return data;
    }

    private GridData createGridData() {
        GridData data = new GridData();
        return data;
    }

    private GridData createComboGridData() {
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        return data;
    }

    public void addCommand(InteractiveConsoleCommand command) {
        this.combo.add(command.name);
        this.nameToCommand.put(command.name, command);
        this.combo.select(this.combo.getItemCount() - 1);
    }

    public void performSave() {
        Yaml yaml = new Yaml();

        Map map = new HashMap<>();
        ArrayList<Object> commands = new ArrayList<>();

        String[] items = this.combo.getItems();
        for (String string : items) {
            InteractiveConsoleCommand command = this.nameToCommand.get(string);
            if (command.isValid()) {
                commands.add(command.asMap());
            }
        }

        map.put("commands", commands);

        File yamlFile = this.scopedPreferences.getWorkspaceSettingsLocation();
        if (!yamlFile.getParentFile().exists()) {
            yamlFile.getParentFile().mkdirs();
        }
        String dumpAsMap = yaml.dumpAsMap(map);
        FileUtils.writeStrToFile(dumpAsMap, yamlFile);
        InteractiveConsoleCommand.keepBindingsUpdated();
    }

    public void removeSelectedCommand() {
        String selectedName = this.combo.getText();
        InteractiveConsoleCommand command = this.nameToCommand.get(selectedName);
        if (command != null) {
            int selectionIndex = combo.getSelectionIndex();
            this.nameToCommand.remove(selectedName);
            this.combo.remove(selectedName);
            if (selectionIndex >= this.combo.getItemCount()) {
                selectionIndex--;
            }
            if (selectionIndex >= 0 && selectionIndex < this.combo.getItemCount()) {
                this.combo.select(selectionIndex);
            }
        }

    }

    public void performDefaults() {
        if (DialogHelpers.openQuestion("Confirm", "Clear all the commands created?")) {
            this.clearCommands();
            this.performSave();
        }
    }

    public void dispose() {
        red.dispose();
    }

    public void setCommandText(String text) {
        this.textCommand.setText(text);

    }

    public void setKeybindingText(String text) {
        this.textKeybinding.setText(text);
    }

    public void selectComboText(String string) {
        String[] items = this.combo.getItems();
        for (int i = 0; i < items.length; i++) {
            if (string.equals(items[i])) {
                this.combo.select(i);
                comboSelectionChanged();
            }
        }
    }

    public String getCommandText() {
        return this.textCommand.getText();
    }

    public String getCommandKeybinding() {
        return this.textKeybinding.getText().trim();
    }

    private Set<String> getCurrentBindings(String comboTextToIgnore) {
        Set<String> currentBindings = new HashSet<>();
        String[] items = combo.getItems();
        for (String commandName : items) {
            if (!commandName.equals(comboTextToIgnore)) {
                //Check all but the current one
                InteractiveConsoleCommand command = nameToCommand.get(commandName);
                if (command != null) {
                    currentBindings.add(command.keybinding);
                }
            }
        }
        return currentBindings;
    }

    /**
     * Returns whether the keybinding is valid.
     * @return
     */
    private boolean validateAndSetKeybinding(String comboText, InteractiveConsoleCommand interactiveConsoleCommand) {
        try {
            String text = textKeybinding.getText().trim();
            if (text.length() == 0) {
                showKeybindingError("The keybinding must be specified.");
                return false;
            }

            Set<String> currentBindings = getCurrentBindings(comboText);
            if (currentBindings.contains(text)) {
                showKeybindingError("The keybinding: " + text + " is already being used.");
                return false;
            }

            KeySequence keySequence = KeyBindingHelper.getKeySequence(text); // Just check if it's valid

            IBindingService bindingService = null;
            try {
                bindingService = (IBindingService) PlatformUI.getWorkbench().getService(
                        IBindingService.class);
            } catch (Throwable e) {
            }
            FastStringBuffer bufConflicts = new FastStringBuffer();
            int numberOfConflicts = 0;
            if (bindingService != null) {
                //We can only do this check if the workbench is running...
                Binding[] bindings = bindingService.getBindings();
                for (Binding binding : bindings) {
                    if (binding.getContextId().equals(PyEdit.PYDEV_EDITOR_KEYBINDINGS_CONTEXT_ID)) {
                        if (binding.getTriggerSequence().equals(keySequence)) {
                            ParameterizedCommand parameterizedCommand = binding.getParameterizedCommand();
                            if (parameterizedCommand == null) {
                                bufConflicts.append(binding.toString()).append('\n');
                            } else {
                                if (!parameterizedCommand.getCommand().getId()
                                        .startsWith(InteractiveConsoleCommand.USER_COMMAND_PREFIX)) {
                                    bufConflicts.append(" - ").append(parameterizedCommand.getName()).append('\n');
                                    numberOfConflicts += 1;
                                }
                            }
                        }
                    }
                }
            }

            interactiveConsoleCommand.keybinding = text;
            if (bufConflicts.length() > 0) {
                showKeybindingError("The current keybinding (" + text + ") conflicts with:\n" + bufConflicts.toString()
                        + "(" + (numberOfConflicts == 1 ? "they" : "it")
                        + "'ll be removed if the changes are applied and\n"
                        + "can only be restored in the 'Keys' preferences page).");
                // Although we show an error, it's valid
                return true;
            } else {
                hideKeybindingError();
                return true;
            }
        } catch (ParseException | IllegalArgumentException e1) {
            showKeybindingError(e1);
            return false;
        } catch (Exception e1) {
            showKeybindingError(e1);
            Log.log(e1);
            return false;
        }
    }

}
