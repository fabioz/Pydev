/*
 * Author: atotic
 * Created on Mar 23, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

/**
 * Interacts with jpydaemon.py.
 * 
 * Knows how to create and interpret jpydaemon commands.
 * 
 * jpydaemon commands:
 * 
 * STOP: exits the debugger
 * CMD <command>: takes a command and executes it 

success:
<JPY> <COMMAND cmd="CMD a=1" operation="a=1" result="OK" /></JPY>
<JPY> <STDOUT content="1" /></JPY>
<JPY> <STDOUT content="/EOL/" /></JPY>
<JPY> <COMMAND cmd="CMD print a" operation="print a" result="OK" /></JPY>
error:
<JPY> <COMMAND cmd="CMD er!" operation="er!" result="Error on CMD" /></JPY>
<JPY> <COMMANDDETAIL  content="Traceback (most recent call last):"  /></JPY>
<JPY> <COMMANDDETAIL  content="  File &quot;D:\pydevspace2\org.python.pydev.debug\pysrc\jpydaemon.py&quot;, line 231, in dealWithCmd    code = compile( arg ,&quot;&lt;string&gt;&quot; , cmdType)"  /></JPY>
<JPY> <COMMANDDETAIL  content="  File &quot;&lt;string&gt;&quot;, line 1"  /></JPY>
<JPY> <COMMANDDETAIL  content="    er!"  /></JPY>
<JPY> <COMMANDDETAIL  content="      ^"  /></JPY>
<JPY> <COMMANDDETAIL  content="SyntaxError: unexpected EOF while parsing"  /></JPY>

 * READSRC <filename>: reads in the whole file
 * SETARGS : sets arguments before we start execution
 <JPY> <COMMAND cmd="SETARGS -u -v" operation="-u -v" result="OK" /></JPY>

 * DBG:
 <JPY><COMMAND cmd="DBG test.py" /></JPY>
 <JPY> <LINE cmd="31" fn="&lt;string&gt;" lineno="1" name="?" line="" /></JPY>
 */
public class RemoteDebuggerCommand {
	public final static String VERSION = "JpyDbg 0.0.3" ; 	
	public final static int INACTIVE  = 0 ; 
	public final static int STARTING  = 1 ; 
	public final static int STARTED   = 2 ; 
    
	private final static String _ERROR_ = "ERROR:"  ;
	private final static String _END_OF_LINE_ = "\n" ; 
	private final static String _INACTIVE_TEXT_ = "inactive" ; 
	private final static String _READY_TEXT_ = "ready" ; 
	private final static String _STRING_ ="<string>"   ;
	private final static String _EOL_ = "/EOL/" ; 
	private final static String _OK_ = "OK" ; 
    
	private final static String _COMMAND_  = "CMD " ; 
	private final static String _BPSET_  = "BP+ " ; 
	private final static String _BPCLEAR_  = "BP- " ; 
	private final static String _DBG_      = "DBG " ; 
	private final static String _SETARGS_  = "SETARGS " ; 
	private final static String _READSRC_  = "READSRC " ; 
	private final static String _NEXT_     = "NEXT " ; 
	private final static String _SET_      = "set " ; 
	private final static String _STEP_     = "STEP " ; 
	private final static String _RUN_      = "RUN " ; 
	private final static String _STOP_     = "STOP " ; 
	private final static String _STACK_    = "STACK " ; 
	private final static String _GLOBALS_  = "GLOBALS " ; 
	private final static String _GLOBAL_   = "global" ; 
	private final static String _EQUAL_    = "=" ; 
	private final static String _SEMICOLON_= ";" ; 
	private final static String _SILENT_   = "silent" ; 
	private final static String _LOCALS_   = "LOCALS " ; 
	private final static String _SPACE_    = " " ; 

	String xmlMessage;
	
	public String getXMLMessage() {
		return xmlMessage;
	}
}
