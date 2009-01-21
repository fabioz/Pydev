/*
 * Author: atotic
 * Created on Mar 22, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.plugin;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Utility class to find a port to debug on.
 * 
 * Straight copy of package org.eclipse.jdt.launching.SocketUtil.
 * I just could not figure out how to import that one. 
 * No dependencies kept it on the classpath reliably
 */
public class SocketUtil {


    /**
     * Returns a free port number on the specified host within the given range,
     * or throws an exception.
     * 
     * @param host name or IP addres of host on which to find a free port
     * @param searchFrom the port number from which to start searching 
     * @param searchTo the port number at which to stop searching
     * @return a free port in the specified range, or an exception if it cannot be found
     */
    public static int findUnusedLocalPort() {
        Exception exception = null;
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } catch (IOException e) {
            exception = e;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        String message = "Unable to find an unused local port (is your firewall enabled?)";
        if (exception != null) {
            throw new RuntimeException(message, exception);
        } else {
            throw new RuntimeException(message);
        }
    }

}
