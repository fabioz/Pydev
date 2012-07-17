/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.logging.ping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;

/**
 * This is a log ping that makes the pinging asynchronous.
 */
public class AsyncLogPing implements ILogPing {

    public static final int SCHEDULE_TIME = 100;
    private final SynchedLogPing internal;
    private List<Object> operations = new ArrayList<Object>();
    private Object lockOperations = new Object();
    private Object lockExecuteCommands = new Object();
    private Job job;

    public AsyncLogPing(String location) {
        this(location, new LogInfoProvider(), new LogPingSender());
    }

    public AsyncLogPing(String location, ILogPingProvider provider, ILogPingSender sender) {
        this(new SynchedLogPing(location, provider, sender));
    }

    public AsyncLogPing(SynchedLogPing logPing) {
        internal = logPing;
        job = new Job("Consume commands") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                consumeAllCommands(true);
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.setPriority(Job.BUILD);
    }

    /**
     * Adds operation and schedules job to treat it later.
     */
    public void addPingOpenEditor() {
        String msg = internal.createPingOpenEditorEncodedMessage();
        synchronized (lockOperations) {
            operations.add(new Tuple<String, String>("addEncodedMessage", msg));
        }
        job.schedule(SCHEDULE_TIME);
    }

    public void addPingStartPlugin() {
        String msg = internal.createPingStartPluginEncodedMessage();
        synchronized (lockOperations) {
            operations.add(new Tuple<String, String>("addEncodedMessage", msg));
        }
        job.schedule(SCHEDULE_TIME);
    }

    /**
     * Adds operation and schedules job to treat it later.
     */
    public void send() {
        synchronized (lockOperations) {
            operations.add("send");
        }
        job.schedule(SCHEDULE_TIME);
    }

    /**
     * Yes, this is the exception that's not asynchronous! Note that sends won't execute at this time.
     * So, commands will only be added at this time without any actual send.
     */
    public void stop() {
        consumeAllCommands(false);
        internal.stop();
    }

    /**
     * Actually executes the commands.
     * 
     * If send is passed, send command are handled, otherwise they're ignored.
     */
    private void consumeAllCommands(final boolean send) {
        ArrayList<Object> local;
        synchronized (lockOperations) {
            local = new ArrayList<Object>(operations);
            operations.clear();
        }
        synchronized (lockExecuteCommands) {
            boolean containsSend = local.contains("send");
            if (containsSend) {
                for (Iterator<Object> it = local.iterator(); it.hasNext();) {
                    if ("send".equals(it.next())) {
                        it.remove(); //remove any send
                    }
                }
            }
            //make all pings before a send.
            for (Object cmd : local) {
                if (cmd instanceof Tuple) {
                    Tuple tuple = (Tuple) cmd;
                    if ("addEncodedMessage".equals(tuple.o1)) {
                        internal.addEncodedMessage((String) tuple.o2);
                        continue;
                    }
                }
                //if it got here, the command wasn't handled.
                Log.log("Invalid command: " + cmd);
            }

            if (send) {
                //make the send.
                if (containsSend) {
                    internal.send();
                }
            }
        }
    }

}
