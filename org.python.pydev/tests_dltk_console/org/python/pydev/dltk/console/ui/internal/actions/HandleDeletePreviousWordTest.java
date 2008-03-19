package org.python.pydev.dltk.console.ui.internal.actions;

import org.eclipse.jface.text.Document;

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
