package org.python.pydev.consoles;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.python.pydev.plugin.PydevPlugin;

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
            if (outputStream == null){
                MessageConsole console = getConsole(name, iconPath);
                
                HashMap<IOConsoleOutputStream, String> themeConsoleStreamToColor = new HashMap<IOConsoleOutputStream, String>();
                outputStream = console.newOutputStream();
                themeConsoleStreamToColor.put(outputStream, "console.output");
                console.setAttribute("themeConsoleStreamToColor", themeConsoleStreamToColor);
                consoles.put(name, console);
                consoleOutputs.put(name, outputStream);
            }
            return outputStream;
        }
    }

    public static MessageConsole getConsole(String name, String iconPath) {
        synchronized (lock) {
            MessageConsole console = consoles.get(name);
            if (console == null){
                console = new MessageConsole(name, PydevPlugin.getImageCache().getDescriptor(iconPath));
                ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{console});
                consoles.put(name, console);
            }
            return console;
        }
    }
}
