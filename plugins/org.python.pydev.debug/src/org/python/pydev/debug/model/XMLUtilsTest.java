/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;

/**
 * @author Fabio
 *
 */
public class XMLUtilsTest extends TestCase {

    public void testXmlUtils() throws Exception {
        String payload = "" + "<xml><comp p0=\"pow\" p1=\"%25\" p2=\"(x, y)\" p3=\"2\"/>" + "</xml>\n" + "\n" + "";
        List<Object[]> xmlToCompletions = XMLUtils.convertXMLcompletionsFromConsole(payload);
        for (Object[] objects : xmlToCompletions) {
            assertEquals("%", objects[1]);
        }
    }

    public void testXmlUtils2() throws Exception {
        String payload = "<xml><thread id=\"pid25170_seq1\" stop_reason=\"111\">\n"
                + "<frame id=\"28191216\" name=\"<module>\" file=\"helloWorld.py\" line=\"6\"></frame><frame id=\"27818048\" name=\"run\" file=\"pydevd.py\" line=\"1355\">\"</frame>\n"
                + "<frame id=\"25798272\" name=\"<module>\" file=\"pydevd.py\" line=\"1738\"></frame></thread></xml>";
        AbstractDebugTarget target = new AbstractDebugTarget() {

            @Override
            public void launchRemoved(ILaunch launch) {
                throw new RuntimeException("not implemented");
            }

            @Override
            public IProcess getProcess() {
                throw new RuntimeException("not implemented");
            }

            @Override
            public boolean isTerminated() {
                throw new RuntimeException("not implemented");
            }

            @Override
            public boolean canTerminate() {
                throw new RuntimeException("not implemented");
            }

            @Override
            protected PyThread findThreadByID(String thread_id) {
                return new PyThread(this, "bar", "10");
            }
        };
        XMLUtils.XMLToStack(target, payload);
    }
}
