package com.python.pydev.analysis.external;

import java.io.IOException;

import org.python.pydev.core.log.Log;

public class WriteToStreamHelper {

    private static Object lock = new Object();

    public static void write(String msg, IExternalCodeAnalysisStream out, Object... args) {
        try {
            if (out != null) {
                synchronized (lock) {
                    if (args != null) {
                        for (Object arg : args) {
                            if (arg instanceof String) {
                                msg += " " + arg;
                            } else if (arg instanceof String[]) {
                                String[] strings = (String[]) arg;
                                for (String string : strings) {
                                    msg += " " + string;
                                }
                            }
                        }
                    }
                    out.write(msg + "\n");
                }
            }
        } catch (IOException e) {
            Log.log(e);
        }
    }

}
