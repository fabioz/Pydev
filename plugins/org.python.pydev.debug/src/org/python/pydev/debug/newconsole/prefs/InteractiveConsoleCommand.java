/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under1 the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.prefs;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.IExecuteLineAction;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.path_watch.IFilesystemChangesListener;
import org.python.pydev.shared_core.path_watch.IPathWatch;
import org.python.pydev.shared_core.path_watch.PathWatch;
import org.python.pydev.shared_core.preferences.IScopedPreferences;
import org.python.pydev.shared_core.preferences.ScopedPreferences;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_ui.bindings.BindKeysHelper;
import org.python.pydev.shared_ui.bindings.BindKeysHelper.IFilter;
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;
import org.python.pydev.shared_ui.utils.RunInUiThread;

/**
 * Used to deal with the interactive console commands defined by the user.
 *
 * These commands can be bound to a key which when triggered will send some
 * content to the console based on the current selection and defined text.
 */
public class InteractiveConsoleCommand {

    public static final class InteractiveCommandCustomHandler extends AbstractHandler {
        private final InteractiveConsoleCommand interactiveConsoleCommand;

        public InteractiveCommandCustomHandler(InteractiveConsoleCommand interactiveConsoleCommand) {
            this.interactiveConsoleCommand = interactiveConsoleCommand;
        }

        @Override
        public Object execute(ExecutionEvent event) throws ExecutionException {
            Object applicationContext = event.getApplicationContext();
            if (applicationContext instanceof org.eclipse.core.expressions.IEvaluationContext) {
                org.eclipse.core.expressions.IEvaluationContext iEvaluationContext = (org.eclipse.core.expressions.IEvaluationContext) applicationContext;
                Object activeEditor = iEvaluationContext.getVariable("activeEditor");
                if (activeEditor instanceof PyEdit) {
                    PyEdit pyEdit = (PyEdit) activeEditor;
                    execute(pyEdit);
                } else {
                    Log.log("Expected PyEdit. Found: " + activeEditor);
                }
            }
            return null;
        }

        public void execute(PyEdit pyEdit) {
            IAction action = pyEdit.getAction("org.python.pydev.editor.actions.execLineInConsole");
            if (action instanceof IExecuteLineAction) {
                IExecuteLineAction executeLineAction = (IExecuteLineAction) action;
                String commandText = this.interactiveConsoleCommand.commandText;
                TextSelectionUtils ts = pyEdit.createTextSelectionUtils();
                String selectedText = ts.getSelectedText();
                if (selectedText.length() == 0) {
                    selectedText = ts.getCursorLineContents();
                }
                executeLineAction.executeText(new FastStringBuffer(commandText, selectedText.length() * 2).replaceAll(
                        "${text}",
                        selectedText).toString());
            } else {
                Log.log("Expected: " + action + " to implement IExecuteLineAction.");
            }
        }
    }

    // The name is always the USER_COMMAND_PREFIX + int (saying which command is bound).
    public static final String USER_COMMAND_PREFIX = "org.python.pydev.custom.interactive_console.user_command.InteractiveConsoleUserCommand";

    /**
     * The name for the command (caption for the user/keybindings).
     */
    public final String name;

    public String keybinding = "";

    public String commandText = "";

    private static final CallbackWithListeners<Object> interactiveConsoleCommandsChanged = new CallbackWithListeners<>();
    private static final IFilesystemChangesListener pathChangesListener = new IFilesystemChangesListener() {

        @Override
        public void removed(File file) {
            interactiveConsoleCommandsChanged.call(null);
        }

        @Override
        public void added(File file) {
            interactiveConsoleCommandsChanged.call(null);

        }
    };

    /**
     * @param name the name of the command.
     * Note that other fields must be properly filled later on.
     */
    public InteractiveConsoleCommand(String name) {
        Assert.isNotNull(name);
        Assert.isTrue(name.length() > 0);
        this.name = name;
    }

    /**
     * @return a map (which we can persist and use later to create a new command).
     */
    public Map<String, Object> asMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("keybinding", keybinding);
        map.put("commandText", commandText);
        return map;
    }

    /**
     * Creates the command from a map. May return null if the contents are not valid.
     */
    public static InteractiveConsoleCommand createFromMap(Map<String, Object> map) {
        String name = (String) map.get("name");
        if (name == null || name.length() == 0) {
            return null;
        }
        String keybinding = (String) map.get("keybinding");
        if (keybinding == null) {
            return null;
        }
        String commandText = (String) map.get("commandText");
        if (commandText == null) {
            return null;
        }
        InteractiveConsoleCommand ret = new InteractiveConsoleCommand(name);
        ret.keybinding = keybinding;
        ret.commandText = commandText;
        return ret;
    }

    /**
     * Loads the commands that the user created previously in the preferences.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static List<InteractiveConsoleCommand> loadExistingCommands() {
        List<InteractiveConsoleCommand> ret = new ArrayList<InteractiveConsoleCommand>();
        try {
            IScopedPreferences scopedPreferences = getScopedPreferences();
            File workspaceSettingsLocation = scopedPreferences.getWorkspaceSettingsLocation();
            Map<String, Object> contentsAsMap = scopedPreferences.getYamlFileContents(workspaceSettingsLocation);
            if (contentsAsMap != null) {
                Object commands = contentsAsMap.get("commands");
                if (commands instanceof Collection) {
                    Collection collection = (Collection) commands;
                    for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
                        Object object = iterator.next();
                        if (object instanceof Map) {
                            Map<String, Object> map = (Map<String, Object>) object;
                            InteractiveConsoleCommand created = InteractiveConsoleCommand.createFromMap(map);
                            if (created != null) {
                                ret.add(created);
                            }

                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return ret;
    }

    /**
     * @return the preferences we'll use to save the commands.
     */
    public static IScopedPreferences getScopedPreferences() {
        return ScopedPreferences.get("org.python.pydev.interactive_console.commands");
    }

    /**
     * Helper to track changes to the preferences.
     */
    private static IPathWatch watcher = null;

    /**
     * Helper for locking.
     */
    private static final Object lock = new Object();

    /**
     * Whenever the preferences related to the command change the passed callback will be called.
     */
    public static void registerOnCommandsChangedCallback(ICallbackListener<Object> iCallbackListener) {
        final File workspaceSettingsLocation = getScopedPreferences().getWorkspaceSettingsLocation();
        File folderToTrack = workspaceSettingsLocation.getParentFile();
        if (folderToTrack.exists()) {
            if (!folderToTrack.isDirectory()) {
                folderToTrack.delete();
                folderToTrack.mkdirs();
            }
        } else {
            // It doesn't exist: create it.
            folderToTrack.mkdirs();
        }

        interactiveConsoleCommandsChanged.registerListener(iCallbackListener);

        // Make sure that we're really tracking the needed folder
        synchronized (lock) {
            if (watcher != null) {
                if (!watcher.hasTracker(folderToTrack, pathChangesListener)) {
                    watcher.dispose();
                    watcher = null;
                }
            }
            if (watcher == null) {
                watcher = new PathWatch();
                watcher.setDirectoryFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File pathname) {
                        return pathname.equals(workspaceSettingsLocation);
                    }
                }, new FileFilter() {

                    @Override
                    public boolean accept(File pathname) {
                        return false;
                    }
                });
                watcher.track(folderToTrack, pathChangesListener);
            }
        }
    }

    public static void unregisterOnCommandsChangedCallback(ICallbackListener<Object> iCallbackListener) {
        interactiveConsoleCommandsChanged.unregisterListener(iCallbackListener);
    }

    /**
     * Helper which when called will synchronize the interactive console commands to the
     * actual commands/bindings in the eclipse preferences.
     *
     * Note that it may be called even if nothing changed...
     */
    private static ICallbackListener<Object> syncCommands = new ICallbackListener<Object>() {

        @Override
        public Object call(Object obj) {
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    syncCommands();
                }
            };
            RunInUiThread.async(r, false);
            return null;
        }
    };

    private static boolean alreadyListening = false;

    /**
     * Makes sure that whenever the user commands are changed the keybindings/commands are kept up to date.
     */
    public static void keepBindingsUpdated() {
        if (alreadyListening) {
            return;
        }
        alreadyListening = true;
        try {
            registerOnCommandsChangedCallback(syncCommands);
            //On the first call make sure that we have the initial state synchronized.
            syncCommands.call(null);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * Creates a handler for the given command.
     */
    protected static InteractiveCommandCustomHandler createHandler(
            final InteractiveConsoleCommand interactiveConsoleCommand) {
        return new InteractiveCommandCustomHandler(interactiveConsoleCommand);
    }

    /**
     * Makes sure that the commands are always updated (to be called in the UI-thread). Not thread safe.
     */
    private static void syncCommands() {
        IWorkbench workbench;
        try {
            workbench = PlatformUI.getWorkbench();
        } catch (Throwable e) {
            // It's already disposed or not even created -- and the class may be unavailable on unit-tests.
            // Log.log(e); -- don't even log (if we're in a state we can't use it, there's no point in doing anything).
            return;
        }
        ICommandService commandService = (ICommandService) workbench.getService(ICommandService.class);

        //Note: hardcoding that we want to deal with the PyDev editor category.
        Category pydevCommandsCategory = commandService.getCategory("org.python.pydev.ui.category.source");
        if (!pydevCommandsCategory.isDefined()) {
            Log.log("Expecting org.python.pydev.ui.category.source to be a a defined commands category.");
            return;
        }

        //First we have to remove bindings which would conflict.
        final Set<KeySequence> removeKeySequences = new HashSet<>();
        List<InteractiveConsoleCommand> existingCommands = InteractiveConsoleCommand.loadExistingCommands();
        for (InteractiveConsoleCommand interactiveConsoleCommand : existingCommands) {
            try {
                removeKeySequences.add(KeyBindingHelper.getKeySequence(interactiveConsoleCommand.keybinding));
            } catch (Exception e) {
                Log.log("Error resolving: " + interactiveConsoleCommand.keybinding, e);
            }
        }

        BindKeysHelper bindKeysHelper = new BindKeysHelper(PyEdit.PYDEV_EDITOR_KEYBINDINGS_CONTEXT_ID);
        bindKeysHelper.removeUserBindingsWithFilter(new IFilter() {

            @Override
            public boolean removeBinding(Binding binding) {
                TriggerSequence triggerSequence = binding.getTriggerSequence();

                if (removeKeySequences.contains(triggerSequence)) {
                    return true;
                }
                ParameterizedCommand command = binding.getParameterizedCommand();
                if (command == null) {
                    return false;
                }
                String id = command.getId();
                if (id.startsWith(USER_COMMAND_PREFIX)) {
                    return true;
                }
                return false;
            }
        });

        Map<String, InteractiveCommandCustomHandler> commandIdToHandler = new HashMap<>();

        // Now, define the commands and the bindings for the user-commands.
        int i = 0;
        for (InteractiveConsoleCommand interactiveConsoleCommand : existingCommands) {
            try {
                String commandId = USER_COMMAND_PREFIX + i;
                Command cmd = commandService.getCommand(commandId);
                if (!cmd.isDefined()) {
                    cmd.define(interactiveConsoleCommand.name, interactiveConsoleCommand.name,
                            pydevCommandsCategory);
                }
                InteractiveCommandCustomHandler handler = createHandler(interactiveConsoleCommand);
                commandIdToHandler.put(commandId, handler);
                cmd.setHandler(handler);
                KeySequence keySequence;
                try {
                    if (interactiveConsoleCommand.keybinding == null
                            || interactiveConsoleCommand.keybinding.length() == 0) {
                        continue;
                    }
                    keySequence = KeyBindingHelper.getKeySequence(interactiveConsoleCommand.keybinding);
                } catch (IllegalArgumentException | ParseException e) {
                    Log.log("Error resolving: " + interactiveConsoleCommand.keybinding, e);
                    continue;
                }
                bindKeysHelper.addUserBindings(keySequence, new ParameterizedCommand(cmd, null));
            } catch (Exception e) {
                Log.log(e);
            }
            i++;
        }

        // Unbind any command we may have previously created.
        for (; i < 100; i++) {
            Command cmd = commandService
                    .getCommand(USER_COMMAND_PREFIX + i);
            if (cmd.isDefined()) {
                cmd.undefine();
            }
        }
        bindKeysHelper.saveIfChanged();
        setCommandIdToHandler(commandIdToHandler);
    }

    /**
     * API to know that the list of commands pointing from command id to the handler changed.
     */
    public static final CallbackWithListeners<Object> onCommandIdToHandlerChanged = new CallbackWithListeners<>();
    private static Map<String, InteractiveCommandCustomHandler> commandIdToHandler = new HashMap<>();

    private static void setCommandIdToHandler(Map<String, InteractiveCommandCustomHandler> commandIdToHandler0) {
        commandIdToHandler = commandIdToHandler0;
        onCommandIdToHandlerChanged.call(commandIdToHandler0);
    }

    public static Map<String, InteractiveCommandCustomHandler> getCommandIdToHandler() {
        return commandIdToHandler;
    }

    public boolean isValid() {
        if (this.name != null && this.name.trim().length() > 0 && this.keybinding != null
                && this.keybinding.trim().length() > 0 && this.commandText != null) {
            //it may be valid, let's check if the keybinding actually resolves.
            try {
                KeyBindingHelper.getKeySequence(keybinding);
            } catch (IllegalArgumentException | ParseException e) {
                return false;
            }
            return true;
        }
        return false;
    }
}
