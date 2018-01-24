package org.python.pydev.shared_ui.log;

import java.io.File;
import java.util.HashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_ui.ConsoleColorCache;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.utils.RunInUiThread;

public class ToLogFile {

    public final static Object lock = new Object();
    public final static StringBuffer logIndent = new StringBuffer();

    /**
     * Console used to log contents
     */
    public static MessageConsole fConsole;
    public static IOConsoleOutputStream fOutputStream;

    public static boolean firstCall = true;

    public static void toLogFile(Object obj, String string) {
        synchronized (lock) {
            if (obj == null) {
                obj = new Object();
            }
            Class<? extends Object> class1 = obj.getClass();
            toLogFile(string, class1);
        }
    }

    public static void toLogFile(String string, Class<? extends Object> class1) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(logIndent);
        buffer.append(FullRepIterable.getLastPart(class1.getName()));
        buffer.append(": ");
        buffer.append(string);

        toLogFile(buffer.toString());
    }

    public static void toLogFile(final String buffer) {
        final Runnable r = new Runnable() {

            @Override
            public void run() {
                synchronized (lock) {
                    try {
                        //Print to console view (must be in UI thread).
                        IOConsoleOutputStream c = getConsoleOutputStream();
                        c.write(buffer.toString());
                        c.write(System.lineSeparator());
                    } catch (Throwable e) {
                        Log.log(e);
                    }
                }

            }
        };

        String file = getLogOutputFile();
        synchronized (lock) {
            //Always print to stdout
            System.out.println(buffer);
            if (file == null) {
                return;
            }

            if (firstCall) {
                //On the first call, remove the file if it's already big (just so that we don't grow it indefinitely).
                try {
                    File f = new File(file);
                    if (f.length() > 1024 * 1024) { //1MB file: delete
                        f.delete();
                    }
                } catch (Exception e) {
                    Log.log(e);
                } finally {
                    firstCall = false;
                }
            }

            //Print to file we can see later on even if not on the UI thread.
            FileUtils.appendStrToFile(buffer + System.lineSeparator(), file);
        }

        RunInUiThread.async(r, true);
    }

    public static void toLogFile(Exception e) {
        String msg = Log.getExceptionStr(e);
        toLogFile(msg);
    }

    public static void addLogLevel() {
        synchronized (lock) {
            logIndent.append("    ");
        }
    }

    public static void remLogLevel() {
        synchronized (lock) {
            if (logIndent.length() > 3) {
                logIndent.delete(0, 4);
            }
        }
    }

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

    public static String getLogOutputFile() {
        try {
            SharedUiPlugin default1 = SharedUiPlugin.getDefault();
            if (default1 != null) {
                IPath stateLocation = default1.getStateLocation().append("PyDevLog.log");
                return stateLocation.toOSString();
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

}
