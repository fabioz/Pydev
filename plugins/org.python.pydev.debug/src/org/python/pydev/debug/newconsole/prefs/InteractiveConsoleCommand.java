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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.path_watch.IFilesystemChangesListener;
import org.python.pydev.shared_core.path_watch.PathWatch;
import org.python.pydev.shared_core.preferences.IScopedPreferences;
import org.python.pydev.shared_core.preferences.ScopedPreferences;

public class InteractiveConsoleCommand {

    public final String name;

    public String keybinding = "";

    public String commandText = "";

    private static final CallbackWithListeners<Object> interactiveConsoleCommandsChanged = new CallbackWithListeners<>();

    public InteractiveConsoleCommand(String name) {
        this.name = name;
    }

    public Map<String, Object> asMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("keybinding", keybinding);
        map.put("commandText", commandText);
        return map;
    }

    public static InteractiveConsoleCommand createFromMap(Map<String, Object> map) {
        String name = (String) map.get("name");
        if (name == null) {
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

    public static IScopedPreferences getScopedPreferences() {
        return ScopedPreferences.get("org.python.pydev.interactive_console.commands");
    }

    private static PathWatch watcher = null;
    private static final Object lock = new Object();

    public static void registerOnCommandsChangedCallback(ICallbackListener<Object> iCallbackListener) {
        interactiveConsoleCommandsChanged.registerListener(iCallbackListener);
        if (watcher == null) {
            synchronized (lock) {
                if (watcher == null) {
                    watcher = new PathWatch();
                    final File workspaceSettingsLocation = getScopedPreferences().getWorkspaceSettingsLocation();
                    if (!workspaceSettingsLocation.getParentFile().exists()) {
                        workspaceSettingsLocation.getParentFile().mkdirs();
                    }
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
                    IFilesystemChangesListener listener = new IFilesystemChangesListener() {

                        @Override
                        public void removed(File file) {
                            interactiveConsoleCommandsChanged.call(null);
                        }

                        @Override
                        public void added(File file) {
                            interactiveConsoleCommandsChanged.call(null);

                        }
                    };
                    watcher.track(workspaceSettingsLocation.getParentFile(), listener);
                }

            }
        }
    }

}
