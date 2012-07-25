/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.logging.ping;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.zip.GZIPOutputStream;

import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;

public class LogPingSender implements ILogPingSender {

    private static final String UPDATE_URL = "https://ping.aptana.com/ping.php"; //$NON-NLS-1$

    public boolean sendPing(String pingString) {
        URL url = null;
        boolean result = false;

        try {
            // gzip POST string
            // System.out.println("Sending: " + queryString);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(baos);
            try {
                gos.write(pingString.getBytes());
                gos.flush();
                gos.finish();
            } finally {
                gos.close();
            }
            baos.close();
            byte[] gzippedData = baos.toByteArray();

            // byte[] gzippedData = queryString.getBytes();

            // create URL
            url = new URL(UPDATE_URL);

            // open connection and configure it
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Encoding", "gzip"); //$NON-NLS-1$ //$NON-NLS-2$
            connection.setRequestProperty("Content-Length", String.valueOf(gzippedData.length)); //$NON-NLS-1$
            connection.setRequestProperty("User-Agent", "Pydev/" + PydevPlugin.version); //$NON-NLS-1$
            try {
                connection.setReadTimeout(1000 * 60); // 1 minute read timeout
            } catch (Throwable e) {
                //ignore (not available for java 1.4)
            }

            // write POST
            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            try {
                output.write(gzippedData);
                // Get the response
                // NOTE: we really only need to read one line, but we read all of them since this lets
                // us examine error messages and helps with debugging
                BufferedReader input;
                StringBuffer sb;
                // output.writeBytes(queryString);
                output.flush();
                input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                try {
                    sb = new StringBuffer();
                    String line;
                    while ((line = input.readLine()) != null) {
                        sb.append(line);
                    }
                    // Debug result! (+ means recorded and - means not recorded)
                    // String resultText = sb.toString();
                    // System.out.println(resultText);
                } finally {
                    input.close();
                }
            } finally {
                output.close();
            }

            result = true;
        } catch (UnknownHostException e) {
            // happens when user is offline or can't resolve aptana.com
        } catch (Exception e) {
            // No need to log (i.e.: Server returned HTTP response code: 500).
        }

        return result;
    }

}
