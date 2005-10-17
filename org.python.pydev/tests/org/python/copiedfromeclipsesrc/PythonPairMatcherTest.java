package org.python.copiedfromeclipsesrc;

import org.eclipse.jface.text.Document;

import junit.framework.TestCase;

public class PythonPairMatcherTest extends TestCase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(PythonPairMatcherTest.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testMatch() throws Exception {
		PythonPairMatcher matcher = new PythonPairMatcher(new char[]{'(', ')', '[', ']'});
		String s = "test (";
		assertEquals(5, matcher.searchForOpeningPeer(s.length(), '(', ')', new Document(s)));
		s = "test ";
		assertEquals(-1, matcher.searchForOpeningPeer(s.length(), '(', ')', new Document(s)));
		s = "test () ";
		assertEquals(-1, matcher.searchForOpeningPeer(s.length(), '(', ')', new Document(s)));
		
	}

}
