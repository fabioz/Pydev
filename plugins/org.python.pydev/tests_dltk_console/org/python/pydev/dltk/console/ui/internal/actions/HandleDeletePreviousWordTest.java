/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.dltk.console.ui.internal.actions;

import org.eclipse.jface.text.Document;
import org.python.pydev.shared_interactive_console.console.ui.internal.actions.HandleDeletePreviousWord;

import junit.framework.TestCase;

public class HandleDeletePreviousWordTest extends TestCase {

    public void testDeletePreviousWord() throws Exception {
        HandleDeletePreviousWord previousWord = new HandleDeletePreviousWord();
        Document doc = new Document(">>> abc def");

        previousWord.execute(doc, doc.getLength(), 4);
        assertEquals(">>> abc ", doc.get());

        previousWord.execute(doc, doc.getLength(), 4);
        assertEquals(">>> ", doc.get());

        previousWord.execute(doc, doc.getLength(), 4);
        assertEquals(">>> ", doc.get());

        previousWord.execute(doc, 2, 4);
        assertEquals(">>> ", doc.get());

        doc = new Document(">>> class A:");
        previousWord.execute(doc, doc.getLength(), 4);
        assertEquals(">>> class A", doc.get());

    }
}
