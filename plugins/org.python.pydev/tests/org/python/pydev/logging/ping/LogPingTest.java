/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.logging.ping;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.python.pydev.core.TestDependent;
import org.python.pydev.logging.ping.AsyncLogPing;
import org.python.pydev.logging.ping.ILogPingProvider;
import org.python.pydev.logging.ping.ILogPingSender;
import org.python.pydev.logging.ping.SynchedLogPing;

public class LogPingTest extends TestCase {

    private SynchedLogPing logPing;
    private String location;
    private long nextTime;
    private boolean sentOk;
    private String sent;

    protected void setUp() throws Exception {
        location = TestDependent.TEST_PYDEV_BASE_LOC + "/testing_ping.txt";
        new File(location).delete(); //delete if it exists
        logPing = createLogPing();
    }

    private SynchedLogPing createLogPing() {
        return new SynchedLogPing(location, new ILogPingProvider() {

            public long getCurrentTime() {
                nextTime += 1;
                return nextTime;
            }

            public String getApplicationId() {
                return "00000000-0000-0000-0000-000000000000";
            }
        },

        new ILogPingSender() {

            public boolean sendPing(String pingString) {
                sent = pingString;
                return sentOk;
            }
        });
    }

    protected void tearDown() throws Exception {
        new File(location).delete();
    }

    /**
        String sent by Aptana Studio:
         
    	String queryString = "" +
    			"id=00000000-0000-0000-0000-000000000000&" +
    			"version=3.0.0.qualifier&" +
    			"product=org.eclipse.sdk.ide&" +
    			"eclipse_version=3.6.0.v20100301&" +
    			"os_architecture=x86&" +
    			"os_name=Windows+XP&" +
    			"os_version=5.1&" +
    			"STUDIO_KEY=00%3A15%3AC5%3AC3%3A42%3ABA&" +
    			"editor.opened%5B%5D=1272400425203%3Aorg.python.pydev.editor.TestingSend&" +
    			"editor.closed%5B%5D=1272400529875%3Aorg.python.pydev.editor.TestingSend&" +
    			"DATE_TIME%5B%5D=1272400557500%3A";
    			
    	logPing.sendUpdate(queryString);
     */
    public void testLogPing() throws Exception {
        logPing.addPingOpenEditor();
        logPing.addPingOpenEditor();

        //Note, already utf-8 encoded.
        assertEquals("id=00000000-0000-0000-0000-000000000000&"
                + "editor.opened%5B%5D=1%3APydevEditor_REPLACE_VERSION&"
                + "editor.opened%5B%5D=2%3APydevEditor_REPLACE_VERSION", logPing.getContentsToSend());
        logPing.clear();
        assertEquals("", logPing.getContentsToSend());

        logPing.addPingOpenEditor();
        logPing.addPingOpenEditor();

        sentOk = false;
        logPing.send();
        assertEquals("id=00000000-0000-0000-0000-000000000000&"
                + "editor.opened%5B%5D=3%3APydevEditor_REPLACE_VERSION&"
                + "editor.opened%5B%5D=4%3APydevEditor_REPLACE_VERSION", logPing.getContentsToSend());
        sentOk = true;
        logPing.send();
        assertEquals("", logPing.getContentsToSend());
        assertEquals("id=00000000-0000-0000-0000-000000000000&"
                + "editor.opened%5B%5D=3%3APydevEditor_REPLACE_VERSION&"
                + "editor.opened%5B%5D=4%3APydevEditor_REPLACE_VERSION", sent);

        logPing.addPingOpenEditor();
        logPing.addPingStartPlugin();
        logPing.stop();
        logPing = createLogPing();
        assertEquals("id=00000000-0000-0000-0000-000000000000&"
                + "editor.opened%5B%5D=5%3APydevEditor_REPLACE_VERSION&"
                + "plugin.started%5B%5D=6%3APydev_REPLACE_VERSION", logPing.getContentsToSend());

        logPing.stop();
        logPing = createLogPing();
        assertEquals("id=00000000-0000-0000-0000-000000000000&"
                + "editor.opened%5B%5D=5%3APydevEditor_REPLACE_VERSION&"
                + "plugin.started%5B%5D=6%3APydev_REPLACE_VERSION", logPing.getContentsToSend());

        logPing.clear();
        logPing.addPingOpenEditor();
        logPing.send();
        assertEquals("", logPing.getContentsToSend());
        assertEquals(
                "id=00000000-0000-0000-0000-000000000000&" + "editor.opened%5B%5D=7%3APydevEditor_REPLACE_VERSION",
                sent);
        logPing.stop();

    }

    public void testAsync() throws Exception {
        sentOk = true;
        AsyncLogPing asyncLogPing = new AsyncLogPing(logPing);
        asyncLogPing.addPingOpenEditor();
        asyncLogPing.addPingOpenEditor();
        asyncLogPing.addPingOpenEditor();
        asyncLogPing.send();
        asyncLogPing.addPingOpenEditor();
        asyncLogPing.addPingStartPlugin();
        asyncLogPing.send();

        assertEquals(null, sent);
        synchronized (this) {
            wait(AsyncLogPing.SCHEDULE_TIME * 5);
        }
        assertEquals("id=00000000-0000-0000-0000-000000000000&"
                + "editor.opened%5B%5D=1%3APydevEditor_REPLACE_VERSION&"
                + "editor.opened%5B%5D=2%3APydevEditor_REPLACE_VERSION&"
                + "editor.opened%5B%5D=3%3APydevEditor_REPLACE_VERSION&"
                + "editor.opened%5B%5D=4%3APydevEditor_REPLACE_VERSION&"
                + "plugin.started%5B%5D=5%3APydev_REPLACE_VERSION", sent);
        asyncLogPing.stop();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("LogPingTest");
        //$JUnit-BEGIN$
        suite.addTestSuite(LogPingTest.class);
        //$JUnit-END$
        return suite;
    }

}
