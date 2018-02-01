package org.python.pydev.shared_ui.log;

import java.util.HashMap;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.python.pydev.shared_ui.ConsoleColorCache;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.SharedUiPlugin;

public class ToLogFile {

    /**
     * Console used to log contents
     */
    public static MessageConsole fConsole;

    public static IOConsoleOutputStream fOutputStream;

    public static IOConsoleOutputStream getConsoleOutputStream() {
        if (fConsole == null) {
            fConsole = new MessageConsole("PyDev Logging",
                    ImageCache.asImageDescriptor(SharedUiPlugin.getImageCache().getDescriptor(
                            "icons/python_logging.png")));

            fOutputStream = fConsole.newOutputStream();

            HashMap<IOConsoleOutputStream, String> themeConsoleStreamToColor = new HashMap<IOConsoleOutputStream, String>();
            themeConsoleStreamToColor.put(fOutputStream, "console.output");
            fConsole.setAttribute("themeConsoleStreamToColor", themeConsoleStreamToColor);
            ConsoleColorCache.getDefault().keepConsoleColorsSynched(fConsole);

            ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { fConsole });
        }
        return fOutputStream;
    }

}
