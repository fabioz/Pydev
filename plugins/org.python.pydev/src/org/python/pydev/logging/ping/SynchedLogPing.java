/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.logging.ping;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Used for usage statistics.
 * 
 * This class is synchronized for concurrent access.
 */
public class SynchedLogPing implements ILogPing {

    private File location;
    private OutputStream bufferedOutputStream;
    private final List<String> keyValueContents = new ArrayList<String>();
    private ILogPingProvider provider;
    public ILogPingSender sender;
    private FileOutputStream stream;
    private static Object lock = new Object();

    public SynchedLogPing(String location) {
        this(location, new LogInfoProvider(), new LogPingSender());
    }

    public SynchedLogPing(String location, ILogPingProvider provider, ILogPingSender sender) {
        this.provider = provider;
        this.sender = sender;
        this.location = new File(location);
        start();
    }

    private void start() {
        synchronized (lock) {
            if (this.location.exists()) {
                //The file exists. Let's fill our list with its contents
                String fileContents;
                try {
                    fileContents = REF.getFileContents(this.location);
                } catch (Exception e) {
                    Log.log(e);
                    fileContents = "";
                }
                if (fileContents.length() > 1024 * 1024) {
                    //More than 1MB in the file. Let's throw the contents away (used for too long
                    //without being online or with active firewall).
                    try {
                        this.location.delete();
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
                List<String> fileLines = StringUtils.split(fileContents, '\n');
                keyValueContents.addAll(fileLines);
            }

            try {
                stream = new FileOutputStream(this.location, true);
                bufferedOutputStream = new BufferedOutputStream(stream);
            } catch (Exception e) {
                try {
                    stream.close();
                } catch (IOException e1) {
                    //ignore.
                }
                Log.log(e);

                bufferedOutputStream = new OutputStream() {

                    public void write(int b) throws IOException {
                        //do nothing (just a stub keeping the interface).
                    }
                };
            }
        }
    }

    /* (non-Javadoc)
     * @see org.python.pydev.logging.ILogPing#addPingOpenEditor()
     */
    public void addPingOpenEditor() {
        synchronized (lock) {
            String message = createPingOpenEditorEncodedMessage();
            addEncodedMessage(message);
        }
    }

    public void addPingStartPlugin() {
        synchronized (lock) {
            String message = createPingStartPluginEncodedMessage();
            addEncodedMessage(message);
        }
    };

    /*package*/String createPingOpenEditorEncodedMessage() {
        return createEncodedMessage("editor.opened", ("PydevEditor_" + PydevPlugin.version));
    }

    /*package*/String createPingStartPluginEncodedMessage() {
        return createEncodedMessage("plugin.started", ("Pydev_" + PydevPlugin.version));
    }

    /*package*/String createEncodedMessage(String key, String val) {
        return StringUtils.urlEncodeKeyValuePair(key + "[]", provider.getCurrentTime() + ":" + val);
    }

    /*package*/void addEncodedMessage(String urlEncodeKeyValuePair) {
        if (urlEncodeKeyValuePair != null) {
            keyValueContents.add(urlEncodeKeyValuePair);
            try {
                bufferedOutputStream.write(urlEncodeKeyValuePair.getBytes());
                bufferedOutputStream.write("\n".getBytes());
            } catch (IOException e) {
                Log.log(e);
            }
        }
    }

    /*package*/void clear() {
        synchronized (lock) {
            this.stop(); //clear in-memory
            try {
                this.location.delete(); //clear file
            } catch (Exception e) {
                Log.log(e);
            }
            this.start(); //recreate the initial structure
        }
    }

    /*package*/String getContentsToSend() {
        synchronized (lock) {
            if (keyValueContents.size() > 0) {
                return "id=" + provider.getApplicationId() + "&" + StringUtils.join("&", keyValueContents);
            }
        }
        return "";
    }

    /* (non-Javadoc)
     * @see org.python.pydev.logging.ILogPing#send()
     */
    public void send() {
        synchronized (lock) {
            String contentsToSend = getContentsToSend();
            //System.out.println("Sending: "+contentsToSend);
            if (this.sender.sendPing(contentsToSend)) {
                this.clear();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.python.pydev.logging.ILogPing#stop()
     */
    public void stop() {
        synchronized (lock) {
            this.keyValueContents.clear();
            try {
                this.bufferedOutputStream.flush();
            } catch (IOException e) {
                //ignore
            }
            try {
                this.bufferedOutputStream.close();
            } catch (IOException e) {
                //ignore
            }
            try {
                this.stream.close();
            } catch (IOException e) {
                //ignore
            }
            bufferedOutputStream = new OutputStream() {

                public void write(int b) throws IOException {
                    //do nothing (just a stub keeping the interface).
                }
            };
        }
    }

}
