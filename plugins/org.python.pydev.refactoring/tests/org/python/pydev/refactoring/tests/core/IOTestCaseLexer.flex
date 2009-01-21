package org.python.pydev.refactoring.tests.core;

import java.io.IOException;

%%
%class IOTestCaseLexer
%standalone
%line

%init{
	sourceLines = new StringBuffer();
	resultLines = new StringBuffer();
	configLines = new StringBuffer();
%init}

%{
	StringBuffer sourceLines;
	StringBuffer resultLines;
	StringBuffer configLines;
	
	public String getSource()
	{
		return sourceLines.toString().trim();
	}
	
	public String getResult()
	{
		return resultLines.toString().trim();
	}
	
	public String getConfig()
	{
		return configLines.toString().trim();
	}
	
	public void scan() throws IOException
	{
		while ( !this.zzAtEOF ) 
			this.yylex();
	}

%}

Source = "##s" {InputCharacter}* {newline}
Config = "##c" {InputCharacter}* {newline}
Result = "##r" {InputCharacter}* {newline}
TripleQuote = '''

InputCharacter = [^\r\n]
newline = \r\n|\n|\r

line = {InputCharacter}*

%xstate RESULT
%xstate CONFIG

%%
<YYINITIAL> {

{Source} { }

{Result} {
	yybegin(RESULT);
}

{Config} {
	yybegin(CONFIG);
}

{line} { sourceLines.append(yytext()); }

{newline} { sourceLines.append("\n"); }

}

<CONFIG> {

{TripleQuote} {} 

{Source} {
	yybegin(YYINITIAL);
}

{Result} {
	yybegin(RESULT);
}

{line} { configLines.append(yytext()); }

{newline} { configLines.append("\n"); }
 
}

<RESULT> {

{Source} {
	yybegin(YYINITIAL);
}

{Config} {
	yybegin(CONFIG);
}

{line} { resultLines.append(yytext()); }

{newline} { resultLines.append("\n"); }
 
}