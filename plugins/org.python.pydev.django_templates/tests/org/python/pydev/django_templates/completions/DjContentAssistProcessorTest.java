/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.completions;

import org.eclipse.jface.text.Document;
import org.python.pydev.django_templates.IDjConstants;

import junit.framework.TestCase;

public class DjContentAssistProcessorTest extends TestCase {

    public void testExtractPrefix() {
        DjContentAssistProcessor djContentAssistProcessor = new DjContentAssistProcessor(
                IDjConstants.CONTENT_TYPE_DJANGO_HTML, null);
        Document doc = new Document();
        assertEquals(djContentAssistProcessor.extractPrefix(doc, 1), "");

        doc = new Document("" + "test" + "");
        assertEquals(djContentAssistProcessor.extractPrefix(doc, 1), "t");
        assertEquals(djContentAssistProcessor.extractPrefix(doc, 2), "te");
        assertEquals(djContentAssistProcessor.extractPrefix(doc, 5), "");

    }

}
