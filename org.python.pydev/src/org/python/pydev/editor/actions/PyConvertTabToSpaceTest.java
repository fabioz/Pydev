/*
 * Created on Jun 21, 2004
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
 * Tests the 'Convert Tab to Space' editor feature.  It performs 3 checks.  
 * 
 * The first fakes a selection of a couple lines in a fake document, and checks
 * to see that the proper tabs are converted to spaces.  
 * 
 * The second fakes a selection but stops in the middle of a line, to make sure that the 
 * whole line is considered.  
 * 
 * The third selects nothing, and makes sure that the whole document is affected by 
 * the conversion.
 */
public class PyConvertTabToSpaceTest extends TestCase
{
	/* The document that will fake an editor environment */
	IDocument document;
	/* Lines of 'code' in the fake document */
	String [] documentLines;
	/* For my own debugging edification, to output the name later */
	static final String TestFileName = "PyConvertTabToSpaceTest";

	/**
	 * Constructor for PyConvertTabToSpaceTest.
	 * @param arg0
	 */
	public PyConvertTabToSpaceTest(String arg0)
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
		// The X will be used to interchange tabs/spaces
		documentLines[++i] = "def bar ( self ):";
		documentLines[++i] = "X" + "print \"foo1\"X ";
		documentLines[++i] = "X" + "print \"bar1\"";
		documentLines[++i] = "X   ";
		documentLines[++i] = "def foo ( self ):X";
		documentLines[++i] = "X" + "print \"foo2\"X ";
		documentLines[++i] = "X" + "print \"bar2\"  ";

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
	 * Just for this and the vice versa case, to make things easier
	 * 
	 * @return String Space string
	 */
	public String from ( )
	{
		return "\t";
	}
	
	
	/*
	 * Just for this and the vice versa case, to make things easier
	 * 
	 * @return String Tab string
	 */
	public String to ( )
	{
		return PyConvertTabToSpace.getTabSpace ( );
	}
	
	
	/*
	 * Just to shorten the lines in the later tests, this calls the action's get tab width 
	 * function
	 * 
	 * @return int Tab width
	 */
	public int getTabWidth ( )
	{
		return PyConvertTabToSpace.getTabWidth ( );
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
		String [] strArr = document.get ( ).replaceAll ( "X", from ( ) ).split ( "\n" );
		int len = 0;
		len += strArr[++i].length ( ) + 1;		// "def bar ( self ):\n"
		len += strArr[++i].length ( ) + 1;		// "\tprint \"foo1\"\t \n"
		len += strArr[++i].length ( ) + 1;		// "\tprint \"bar1\"\n"
		len += strArr[++i].length ( ) + 1;		// "\n   \n"
			startLineIndex = i + 1;
			selBegin = len;
		len += strArr[++i].length ( ) + 1;		// "def foo ( self ):
		len += strArr[++i].length ( ) + 1;		// "\tprint \"foo2\"\t \n"
		len += strArr[++i].length ( );		// "\tprint \"bar2\"  \n"
			endLineIndex = i;
			selLength = len - selBegin;
		
		// Create result document
		i = -1;
		result.append ( documentLines[++i].replaceAll ( "X", from ( ) ) + "\n" );
		result.append ( documentLines[++i].replaceAll ( "X", from ( ) ) + "\n" );
		result.append ( documentLines[++i].replaceAll ( "X", from ( ) ) + "\n" );
		result.append ( documentLines[++i].replaceAll ( "X", from ( ) ) + "\n" );
		result.append ( documentLines[++i].replaceAll ( "X", to ( ) ) + "\n" );
		result.append ( documentLines[++i].replaceAll ( "X", to ( ) ) + "\n" );
		result.append ( documentLines[++i].replaceAll ( "X", to ( ) ) );
			
		// Our expected result
		IDocument resultDoc = new Document ( result.toString ( ) );
		
		// For timing data
		document.set ( document.get ( ).replaceAll ( "X", from ( ) ) );
		long begin = System.currentTimeMillis ( );
		PySelection ps = new PySelection ( document, startLineIndex, endLineIndex, selLength, true );
		PyConvertTabToSpace.perform ( ps );
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
		
		// 'Select' the entire last def
		String [] strArr = document.get ( ).replaceAll ( "X", from ( ) ).split ( "\n" );
		int len = 0;
		len += strArr[++i].length ( ) + 1;		// "def bar ( self ):\n"
		len += strArr[++i].length ( ) + 1;		// "\tprint \"foo1\"\t \n"
		len += strArr[++i].length ( ) + 1;		// "\tprint \"bar1\"\n"
		len += strArr[++i].length ( ) + 1;		// "\n   \n"
			startLineIndex = i + 1;
			selBegin = len;
		len += strArr[++i].length ( ) + 1;		// "def foo ( self ):
		len += strArr[++i].length ( ) + 1;		// "\tprint \"foo2\"\t \n"
		len += strArr[++i].length ( );		// "\tprint \"bar2\"  \n"
			endLineIndex = i;
			selLength = len - selBegin - 6;
		
		// Create result document
		i = -1;
		result.append ( documentLines[++i].replaceAll ( "X", from ( ) ) + "\n" );
		result.append ( documentLines[++i].replaceAll ( "X", from ( ) ) + "\n" );
		result.append ( documentLines[++i].replaceAll ( "X", from ( ) ) + "\n" );
		result.append ( documentLines[++i].replaceAll ( "X", from ( ) ) + "\n" );
		result.append ( documentLines[++i].replaceAll ( "X", to ( ) ) + "\n" );
		result.append ( documentLines[++i].replaceAll ( "X", to ( ) ) + "\n" );
		result.append ( documentLines[++i].replaceAll ( "X", to ( ) ) );
			
		// Our expected result
		IDocument resultDoc = new Document ( result.toString ( ) );
		
		// For timing data
		document.set ( document.get ( ).replaceAll ( "X", from ( ) ) );
		long begin = System.currentTimeMillis ( );
		PySelection ps = new PySelection ( document, startLineIndex, endLineIndex, selLength, true );
		PyConvertTabToSpace.perform ( ps );
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
		
		// 'Select' the entire last def
		startLineIndex = endLineIndex = 1;
		selBegin = 1;
		selLength = 0;
		
		// Our expected result
		IDocument resultDoc = new Document ( document.get ( ).toString ( ).replaceAll ( "X", to ( ) ) );
		
		// For timing data
		document.set ( document.get ( ).replaceAll ( "X", from ( ) ) );
		long begin = System.currentTimeMillis ( );
		PySelection ps = new PySelection ( document, startLineIndex, endLineIndex, selLength, true );
		PyConvertTabToSpace.perform ( ps );
		long end = System.currentTimeMillis ( );

		// Timing result
		System.err.print ( TestFileName + " :: " );
		System.err.println ( "testPerform3: " + ( end - begin ) + "ms" );

		// Document affected properly?
		assertEquals ( document.get ( ), resultDoc.get ( ) );
	}
}
