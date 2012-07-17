/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Mar 22, 2004
 */
package org.python.pydev.plugin;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

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
    public static Integer[] findUnusedLocalPorts(int ports) {
        List<ServerSocket> socket = new ArrayList<ServerSocket>();
        List<Integer> portsFound = new ArrayList<Integer>();
        try {
            try {
                for (int i = 0; i < ports; i++) {
                    ServerSocket s = new ServerSocket(0);
                    socket.add(s);
                    int localPort = s.getLocalPort();
                    checkValidPort(localPort);
                    portsFound.add(localPort);
                }
            } finally {
                for (ServerSocket s : socket) {
                    if (s != null) {
                        try {
                            s.close();
                        } catch (Exception e) {
                            //Just ignore errors closing sockets
                        }
                    }
                }
            }
        } catch (Throwable e) {
            String message = "Unable to find an unused local port (is there an enabled firewall?)";
            throw new RuntimeException(message, e);
        }

        return portsFound.toArray(new Integer[portsFound.size()]);
    }

    public static void checkValidPort(int port) throws IOException {
        if (port == -1) {
            throw new IOException("Port not bound (found port -1). Is there an enabled firewall?");
        }
    }

}
