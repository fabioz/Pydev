/*
 * Created on Jun 17, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package test;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Dreamer
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class AllTests
{
	public static String TestFileName = "Editor.Actions";
	
	public static Test suite()
	{
		TestSuite suite =
			new TestSuite("Test for org.python.pydev.editor.actions");
			
		//$JUnit-BEGIN$
		suite.addTest(new TestSuite(PyCommentTest.class));
		suite.addTest(new TestSuite(PyUncommentTest.class));
		suite.addTest(new TestSuite(PyAddBlockCommentTest.class));
		suite.addTest(new TestSuite(PyRemoveBlockCommentTest.class));
		suite.addTest(new TestSuite(PyStripTrailingWhitespaceTest.class));
		suite.addTest(new TestSuite(PyConvertSpaceToTabTest.class));
		suite.addTest(new TestSuite(PyConvertTabToSpaceTest.class));
		//$JUnit-END$
		
		System.out.println ( "Running Test Suite '" + TestFileName + "'..." );
		return suite;
	}
}
