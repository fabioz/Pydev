/*
 * Created on Jun 16, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import junit.framework.TestCase;

/**
 * @author Dreamer
 *
 * Tests the 'Strip Trailing Whitespace' editor feature.  It performs 4 checks.  
 * 
 * The first checks to see if the simple String-input 'trim' feature works properly.  
 * 
 * The other 3 are code-based checks.  
 * 
 * The first fakes a selection of a couple lines in a fake document, and checks
 * to see that the proper whitespace is removed.  
 * 
 * The second fakes a selection but stops in the
 * middle of a line, to make sure that the Whitespace handler doesn't just strip whitespace from
 * the partial selection unless that line has whitespace to be stripped.  
 * 
 * The third selects
 * nothing, and makes sure that the whole document is affected by whitespace stripping.
 */
public class PyStripTrailingWhitespaceTest extends TestCase
{
	/* The document that will fake an editor environment */
	IDocument document;
	/* Lines of 'code' in the fake document */
	String [] documentLines;
	/* For my own debugging edification, to output the name later */
	static final String TestFileName = "PyStripTrailingWhitespaceTest";

	/**
	 * Constructor for PyStripTrailingWhitespaceTest.
	 * @param arg0
	 */
	public PyStripTrailingWhitespaceTest(String arg0)
	{
		super(arg0);
	}


	/*
	 * Sets up the document 'code' and adds it to the document.
	 * 
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
		
		int i 				= -1;
		int length 			= 7;	
		documentLines 		= new String[length];
		documentLines[++i] = "def bar ( self ):";
		documentLines[++i] = "\tprint \"foo1\"\t ";
		documentLines[++i] = "\tprint \"bar1\"";
		documentLines[++i] = "\t   ";
		documentLines[++i] = "def foo ( self ):    ";
		documentLines[++i] = "\tprint \"foo2\"\t ";
		documentLines[++i] = "\tprint \"bar2\"  ";

		StringBuffer doc	= new StringBuffer ( );

		for ( i = 0; i < documentLines.length; i++ )
		{
			doc.append ( documentLines[i] + ( i < documentLines.length - 1 ? "\n" : "" ) );
		}
		
		document = new Document ( doc.toString ( ) );
	}


	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}


	/*
	 * Just to shorten the lines in the later tests, this calls the action's trim function
	 * 
	 * @param in Line of 'code' to be stripped
	 * @return String Stripped 'code' line
	 */
	public String callTrim ( String in )
	{
		return PyStripTrailingWhitespace.trimTrailingWhitespace ( in );
	}


	/*
	 * Checks single-line trimming.
	 */
	public void testTrimTrailingWhitespace ( )
	{
		// Preserves everything but last whitespace
		assertEquals ( "\tprint \"foo\"", callTrim ( "\tprint \"foo\"\t" ) );
		// Ditto, without tab in front
		assertEquals ( "print \"foo\"", callTrim ( "print \"foo\"    \t" ) );
		// Full line blank
		assertEquals ( "", callTrim ( "\t   \t" ) );
		// Null input
		assertEquals ( "", callTrim ( null ) );
	}


	/*
	 * Checks multiple-line selection.
	 */
	public void testPerform1 ( )
	{
		StringBuffer result = new StringBuffer ( );
		int i = -1;
		
		int startLineIndex = 0;
		int endLineIndex = 0;
		int selBegin = 0;
		int selLength = 0;
		
		// 'Select' the entire last def
		result.append ( documentLines[++i] + "\n" );				// "def bar ( self ):\n"
		result.append ( documentLines[++i] + "\n" );				// "\tprint \"foo1\"\t \n"
		result.append ( documentLines[++i] + "\n" );				// "\tprint \"bar1\"\n"
		result.append ( documentLines[++i] + "\n" );				// "\n   \n"
			startLineIndex = i + 1;
			selBegin = result.toString ( ).length ( );
		result.append ( callTrim ( documentLines[++i] ) + "\n" );	// "def foo ( self ):      \n"
		result.append ( callTrim ( documentLines[++i] ) + "\n" );	// "\tprint \"foo2\"\t \n"
		result.append ( callTrim ( documentLines[++i] ) );			// "\tprint \"bar2\"  \n"
			endLineIndex = i;
			selLength = document.get ( ).length ( ) - selBegin;
			
		// Our expected result
		IDocument resultDoc = new Document ( result.toString ( ) );
		
		// For timing data
		long begin = System.currentTimeMillis ( );
		PySelection ps = new PySelection ( document, startLineIndex, endLineIndex, selLength, true );
		PyStripTrailingWhitespace.perform ( ps );
		long end = System.currentTimeMillis ( );

		// Timing result
		System.err.print ( TestFileName + " :: " );
		System.err.println ( "testPerform1: " + ( end - begin ) + "ms" );

		// Document affected properly?
		assertEquals ( document.get ( ), resultDoc.get ( ) );
	}


	/*
	 * Checks multiple-line selection with the last line partially selected.
	 */
	public void testPerform2 ( )
	{
		StringBuffer result = new StringBuffer ( );
		int i = -1;
		
		int startLineIndex = 0;
		int endLineIndex = 0;
		int selBegin = 0;
		int selLength = 0;
		
		// 'Select' part of the last def
		result.append ( documentLines[++i] + "\n" );				// "def bar ( self ):\n"
		result.append ( documentLines[++i] + "\n" );				// "\tprint \"foo1\"\t \n"
		result.append ( documentLines[++i] + "\n" );				// "\tprint \"bar1\"\n"
		result.append ( documentLines[++i] + "\n" );				// "\n   \n"
			startLineIndex = i + 1;
			selBegin = result.toString ( ).length ( );
		result.append ( callTrim ( documentLines[++i] ) + "\n" );	// "def foo ( self ):      \n"
		result.append ( callTrim ( documentLines[++i] ) + "\n" );	// "\tprint \"foo2\"\t \n"
		result.append ( documentLines[++i] );						// "\tprint \"bar2\"  \n"
			endLineIndex = i;
			selLength = document.get ( ).length ( ) - selBegin - 6;
			
		// Our expected result
		IDocument resultDoc = new Document ( result.toString ( ) );
		
		// For timing data
		long begin = System.currentTimeMillis ( );
		PySelection ps = new PySelection ( document, startLineIndex, endLineIndex, selLength, true );
		PyStripTrailingWhitespace.perform ( ps );
		long end = System.currentTimeMillis ( );
		
		// Timing result
		System.err.print ( TestFileName + " :: " );
		System.err.println ( "testPerform2: " + ( end - begin ) + "ms" );

		// Document affected properly?
		assertEquals ( document.get ( ), resultDoc.get ( ) );
	}


	/*
	 * Checks empty selection to affect cursor line.
	 */
	public void testPerform3 ( )
	{
		StringBuffer result = new StringBuffer ( );
		int i = -1;
		
		int startLineIndex = 0;
		int endLineIndex = 0;
		int selBegin = 0;
		int selLength = 0;
		
		// 'Select' nothing, which should call action on the whole thing
		result.append ( callTrim ( documentLines[++i] ) + "\n" );	// "def bar ( self ):\n"
		result.append ( callTrim ( documentLines[++i] ) + "\n" );	// "\tprint \"foo1\"\t \n"
		result.append ( callTrim ( documentLines[++i] ) + "\n" );	// "\tprint \"bar1\"\n"
		result.append ( callTrim ( documentLines[++i] ) + "\n" );	// "\n   \n"
		result.append ( callTrim ( documentLines[++i] ) + "\n" );	// "def foo ( self ):      \n"
		result.append ( callTrim ( documentLines[++i] ) + "\n" );	// "\tprint \"foo2\"\t \n"
		result.append ( callTrim ( documentLines[++i] ) );			// "\tprint \"bar2\"  \n"
		endLineIndex = i;
			
		// Our expected result
		IDocument resultDoc = new Document ( result.toString ( ) );
		
		// For timing data
		long begin = System.currentTimeMillis ( );
		PySelection ps = new PySelection ( document, startLineIndex, endLineIndex, selLength, true );
		PyStripTrailingWhitespace.perform ( ps );
		long end = System.currentTimeMillis ( );
		
		// Timing result
		System.err.print ( TestFileName + " :: " );
		System.err.println ( "testPerform3: " + ( end - begin ) + "ms" );

		// Document affected properly?
		assertEquals ( document.get ( ), resultDoc.get ( ) );
	}
}
