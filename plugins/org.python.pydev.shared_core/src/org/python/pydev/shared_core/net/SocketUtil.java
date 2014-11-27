/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Mar 22, 2004
 */
package org.python.pydev.shared_core.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.python.pydev.shared_core.log.Log;

/**
 * Utility class to find a port to debug on.
 * 
 * Based on org.eclipse.jdt.launching.SocketUtil.
 */
public class SocketUtil {

    /**
     * Returns free ports on the local host.
     * 
     * @param ports: number of ports to return.
     */
    public static Integer[] findUnusedLocalPorts(final int ports) {

        Throwable firstFoundExc = null;
        final List<ServerSocket> socket = new ArrayList<ServerSocket>();
        final List<Integer> portsFound = new ArrayList<Integer>();
        try {
            try {
                for (int i = 0; i < ports; i++) {
                    ServerSocket s = new ServerSocket(0);
                    socket.add(s);
                    int localPort = s.getLocalPort();
                    checkValidPort(localPort);
                    portsFound.add(localPort);
                }

            } catch (Throwable e) {
                firstFoundExc = e;
                // Try a different approach...
                final Set<Integer> searched = new HashSet<Integer>();
                try {
                    for (int i = 0; i < ports && portsFound.size() < ports; i++) {
                        int localPort = findUnusedLocalPort(20000, 65535, searched);
                        checkValidPort(localPort);
                        portsFound.add(localPort);
                    }
                } catch (Exception e1) {
                    Log.log(e1); // log this one (but the outer one will be thrown).
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

            if (portsFound.size() != ports) {
                throw firstFoundExc;
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
        if (port == 0) {
            throw new IOException("Port not bound (found port 0). Is there an enabled firewall?");
        }
    }

    private static final Random fgRandom = new Random(System.currentTimeMillis());

    /**
     * Returns a free port number on the specified host within the given range,
     * or -1 if none found.
     */
    private static int findUnusedLocalPort(int searchFrom, int searchTo, Set<Integer> searched) {
        for (int i = 0; i < 15; i++) {
            int port = getRandomPort(searchFrom, searchTo);
            if (searched.contains(i)) {
                continue;
            }
            searched.add(i);
            ServerSocket s = null;
            try {
                s = new ServerSocket();
                SocketAddress sa = new InetSocketAddress(InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), port);
                s.bind(sa); // throws IOException (which can be ignored as this is in use...) 
                return s.getLocalPort();
            } catch (IOException e) {
            } finally {
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException ioe) {
                    }
                }
            }
        }
        return -1;
    }

    private static int getRandomPort(int low, int high) {
        return (int) (fgRandom.nextFloat() * (high - low)) + low;
    }

    public static ServerSocket createLocalServerSocket() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        int localPort = serverSocket.getLocalPort();
        try {
            SocketUtil.checkValidPort(localPort);
        } catch (Exception e) {
            // 0 did not give us a valid local port... close this one and try a different approach.
            try {
                serverSocket.close();
            } catch (Exception e1) {
            }

            serverSocket = new ServerSocket(SocketUtil.findUnusedLocalPorts(1)[0]);
            localPort = serverSocket.getLocalPort();
            try {
                SocketUtil.checkValidPort(localPort);
            } catch (IOException invalidPortException) {
                // Still invalid: close the socket and throw error!
                try {
                    serverSocket.close();
                } catch (Exception e1) {
                }
                throw invalidPortException;
            }
        }

        return serverSocket;
    }
}
