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
//			writeHeading ( heads, links );
//		</script>
///////////////////////////////////////////////////////////////////////////////////////////////////
function writeHeading ( heads, links )
{
	document.write ( '<p class="Heading">' );
	for ( var i = 0; i < heads.length; i++ )
	{
		if ( links[i] != '' && i < heads.length - 1 )
			document.write ( '<a class="Heading" href="' + links[i] + '">' );
		document.write ( heads[i] );
		if ( links[i] != '' && i < heads.length - 1 )
			document.write ( '</a>' );
		if ( i < heads.length - 1 )
			document.write ( ' :: ' );
	}
	document.write ( '</p>' );
	document.write ( '<hr>' );
}
//////////////////////////////////////////////////////////////////////////////[ writeHeading ]/////


