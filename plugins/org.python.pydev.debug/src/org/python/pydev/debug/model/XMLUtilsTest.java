/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import java.util.List;

import junit.framework.TestCase;

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
}
