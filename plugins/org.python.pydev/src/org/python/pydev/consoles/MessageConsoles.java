/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.consoles;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.ConsoleColorCache;

/**
 * Helper for classes that want to create a message console for writing to it in a stream later on.
 */
public class MessageConsoles {

    private static Map<String, MessageConsole> consoles = new HashMap<String, MessageConsole>();
    private static Map<String, IOConsoleOutputStream> consoleOutputs = new HashMap<String, IOConsoleOutputStream>();
    private static Object lock = new Object();

    public static IOConsoleOutputStream getConsoleOutputStream(String name, String iconPath) {
        synchronized (lock) {
            IOConsoleOutputStream outputStream = consoleOutputs.get(name);
            if (outputStream == null) {
                MessageConsole console = getConsole(name, iconPath);

                HashMap<IOConsoleOutputStream, String> themeConsoleStreamToColor = new HashMap<IOConsoleOutputStream, String>();
                outputStream = console.newOutputStream();
                themeConsoleStreamToColor.put(outputStream, "console.output");
                console.setAttribute("themeConsoleStreamToColor", themeConsoleStreamToColor);
                ConsoleColorCache.getDefault().keepConsoleColorsSynched(console);
                consoles.put(name, console);
                consoleOutputs.put(name, outputStream);
            }
            return outputStream;
        }
    }

    public static MessageConsole getConsole(String name, String iconPath) {
        synchronized (lock) {
            MessageConsole console = consoles.get(name);
            if (console == null) {
                console = new MessageConsole(name, PydevPlugin.getImageCache().getDescriptor(iconPath));
                ConsoleColorCache.getDefault().keepConsoleColorsSynched(console);
                ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { console });
                consoles.put(name, console);
            }
            return console;
        }
    }
}
