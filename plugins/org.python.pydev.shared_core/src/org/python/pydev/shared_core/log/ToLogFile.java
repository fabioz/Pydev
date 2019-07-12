package org.python.pydev.shared_core.log;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FullRepIterable;

public class ToLogFile {

    public final static StringBuffer logIndent = new StringBuffer();
    public final static Object lock = new Object();
    public static boolean firstCall = true;

    public static String getLogOutputFile() {
        try {
            SharedCorePlugin default1 = SharedCorePlugin.getDefault();
            if (default1 != null) {
                IPath stateLocation = default1.getStateLocation().append("PyDevLog.log");
                return stateLocation.toOSString();
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
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

    public static ICallback<Object, String> afterOnToLogFile;

    public static void toLogFile(String buffer) {
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

        if (afterOnToLogFile != null) {
            afterOnToLogFile.call(buffer);
        }
    }

    public static void toLogFile(Exception e) {
        String msg = Log.getExceptionStr(e);
        toLogFile(msg);
    }

    public static void toLogFile(String string, Class<? extends Object> class1) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(logIndent);
        buffer.append(FullRepIterable.getLastPart(class1.getName()));
        buffer.append(": ");
        buffer.append(string);

        toLogFile(buffer.toString());
    }

    public static void toLogFile(Object obj, String string) {
        synchronized (lock) {
            if (obj == null) {
                obj = new Object();
            }
            Class<? extends Object> class1 = obj.getClass();
            toLogFile(string, class1);
        }
    }

}
