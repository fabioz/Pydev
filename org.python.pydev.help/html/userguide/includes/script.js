/*=================================================================================================

	File:		script.js
	
	Author:		Parhaum Toofanian, Copyright 2004
	Contact:	ptoofani@andrew.cmu.edu
	Created:	2004-06-14
	Modified:	2004-06-14
	
	Script file for PyDev User Guide in Eclipse.

=================================================================================================*/


//////////////////////////////////////////////////////////////////////////////[ writeHeading ]/////
//	Name:	writeHeading
//
//	Desc:	Writes a standard, formatted header for each page body.
//
//	In:		heads			Array 		Stores multiple headlines 
//			links			Array		Stores links to headlines
//	Out:	-
//
//	Sample usage:
//
//		<script language="javascript">
//			var heads = new Array ( );
//			var links = new Array ( );
//
//			heads[0] = 'Foo';
//			links[0] = 'foo.html';
//
//			heads[1] = 'Bar';
//			links[1] = 'bar.html';
//
//			writeHeading ( heads, links );
//		</script>
//
//	Will produce:
//
//		<a href=foo.html>Foo</a> » Bar
///////////////////////////////////////////////////////////////////////////////////////////////////
function writeHeading ( heads, links )
{
	var doc = document;
	
	doc.write ( '<p class="Heading" name="top">' );
	for ( var i = 0; i < heads.length; i++ )
	{
		if ( links[i] != '' && i < heads.length - 1 )
			doc.write ( '<a class="Heading" href="' + links[i] + '">' );
		doc.write ( heads[i] );
		if ( links[i] != '' && i < heads.length - 1 )
			doc.write ( '</a>' );
		if ( i < heads.length - 1 )
			doc.write ( ' » ' );
	}
	doc.write ( '</p>' );
	doc.write ( '<hr>' );
}
//////////////////////////////////////////////////////////////////////////////[ writeHeading ]/////


///////////////////////////////////////////////////////////////////////////[ writeInvocation ]/////
//	Name:	writeInvocation
//
//	Desc:	Writes a table with an invocation description of a given feature, including an image
//			of the Source menu and hotkeys.
//
//	In:		rows			Array 		Stores which row(s) to point to on menu image
//			hotkeys			Array		Stores hotkey(s)
//			desc			String		Description of invocation, could be used with or as opposed
//										to the menu image and hotkeys.
//	Out:	-
//
//	Sample usage:
//
//		<script language="javascript">
//			var rows = new Array ( );
//			var hotkeys = new Array ( );
//
//			rows[0] 	= 2;
//			rows[1] 	= 5;
//
//			hotkeys[0] 	= 'Ctrl+4';
//			hotkeys[1] 	= 'Ctrl+5';
//
//			var desc	= '';
//
//			writeInvocation ( rows, hotkeys, desc );
//		</script>
//
//	Will produce:
//
//		(A table with pointers to the 2nd and 5th rows, as well as a list of the applicable
//		hotkeys.)
///////////////////////////////////////////////////////////////////////////////////////////////////
function writeInvocation ( rows, hotkeys, desc )
{
	// Check if there's anything at all to show
	if ( rows.length == 0 && hotkeys.length == 0 && desc == '' )
		return;
	
	var doc = document;
	
	doc.write ( '<b><u>Invocation</u></b>' );
	doc.write ( ' <a href="#top">[top]</a>' );
	doc.write ( '<ul>' );
	
	if ( rows.length > 0 )
	{
		doc.write ( '<li>Main Menu or Source Menu (right-click in editor):' );
		doc.write ( '<br><br>' );
		doc.write ( '<table>' );
		doc.write ( '	<tr>' );
		doc.write ( '		<td width = 15 align=left valign=top class=Pointer>' );
		doc.write ( '			<br>' );
		
		// Draw row pointers
		var j = 0;
		for ( var i = 0; i < rows.length; i++ )
		{
			for ( ; j < rows[i] - 1; j++ )
			{
				doc.write ( '<br>' );
			}
			doc.write ( '»' );
		}

		doc.write ( '		</td>' );
		doc.write ( '		<td>' );
		doc.write ( '			<img src="images/menu.jpg">' );
		doc.write ( '		</td>' );
		doc.write ( '	</tr>' );
		doc.write ( '</table>' );
	}
	if ( rows.length > 0 && hotkeys.length > 0 )
		doc.write ( '<br>' );
	if ( hotkeys.length > 0 )
	{
		doc.write ( '<li>Hotkeys: ' );
		
		for ( var i = 0; i < hotkeys.length; i++ )
		{
			doc.write ( hotkeys[i] );
			if ( i < hotkeys.length - 1 )
				doc.write ( ', ' );
		}
	}
	if ( desc != '' )
	{
		doc.write ( '<li>' + desc );
	}
	doc.write ( '</ul><br>' );
}
//////////////////////////////////////////////////////////////////////////[ writeInvocation ]//////


/////////////////////////////////////////////////////////////////////////////[ writeGeneric ]/////
//	Name:	writeGeneric
//
//	Desc:	Writes a general topic with headline and description.
//
//	In:		headline	String		Headline of topic
//			desc		String		Description
//	Out:	-
//
//	Sample usage:
//
//		<script language="javascript">
//			var headline	= 'Headliner';
//			var desc		= 'Do this and do that.';
//
//			writeProcess ( process );
//		</script>
//
//	Will produce:
//
//		<i>Headliner</i>
//
//			Do this and do that.
///////////////////////////////////////////////////////////////////////////////////////////////////
function writeGeneric ( headline, desc )
{
	// Check if anything to write
	if ( desc == '' )
		return;
	
	var doc = document;
	
	doc.write ( '<b><u>' + headline + '</u></b>' );
	doc.write ( ' <a href="#top">[top]</a>' );
	doc.write ( '<ul>' );
	doc.write ( desc );
	doc.write ( '</ul><br>' );
}
/////////////////////////////////////////////////////////////////////////////[ writeGeneric ]//////
