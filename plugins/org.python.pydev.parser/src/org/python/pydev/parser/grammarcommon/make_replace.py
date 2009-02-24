import os
parent_dir = os.path.split(os.path.split(__file__)[0])[0]


files = [
    os.path.join(parent_dir, 'grammar24', 'python.jjt_template'),
    os.path.join(parent_dir, 'grammar25', 'python.jjt_template'),
    os.path.join(parent_dir, 'grammar26', 'python.jjt_template'),
    os.path.join(parent_dir, 'grammar30', 'python.jjt_template'),
]

DEFINITIONS = dict(
    
NAME_DEFINITION=\
'''
Token Name() #Name:
{
    Token t;
}
{
    try{
        t = <NAME> 
    }catch(ParseException e){
        t = handleErrorInName(e);
    }

        { ((Name)jjtThis).id = t.image; return t; } {}

}
''',

RPAREN ='''{this.findTokenAndAdd(")");}<RPAREN>''',

COLON ='''{this.findTokenAndAdd(":");} <COLON>''',

YIELD = '''
//yield_expr: 'yield' [testlist]
void yield_expr(): {Object spStr;}
{ {spStr = createSpecialStr("yield","yield ", false);} <YIELD> [SmartTestList()] {this.addToPeek(spStr, false, Yield.class);}}
''',


SUITE = '''
//suite: simple_stmt | NEWLINE INDENT stmt+ DEDENT
void suite(): {}
{ 

try{
        simple_stmt() 
    |  
    
        try{<NEWLINE><INDENT>}catch(ParseException e){handleErrorInIndent(e);}
        
        (try{stmt()}catch(ParseException e){handleErrorInStmt(e);})+ 
        
        try{<DEDENT>}catch(ParseException e){handleErrorInDedent(e);} 
    
    |
    try{
        {handleNoIndentInSuiteFound();}
        
        (try{stmt()}catch(ParseException e){handleErrorInStmt(e);})+ 
        
        try{<DEDENT>}catch(ParseException e){handleErrorInDedent(e);} 
    }catch(EmptySuiteException emptySuiteE){} //just close it 'gracefully'
        

}catch(ParseException e){handleNoSuiteMatch(e);}

}
''',

STMT = '''
//stmt: simple_stmt | compound_stmt
void stmt() #void: {}
{ simple_stmt() | compound_stmt() }
''',


SIMPLE_STMT='''
//simple_stmt: small_stmt (';' small_stmt)* [';'] NEWLINE
void simple_stmt() #void: {}
{ 
    small_stmt() (LOOKAHEAD(2) <SEMICOLON> small_stmt())* 
    [<SEMICOLON>] 
    try{<NEWLINE>}catch(ParseException e){
        handleNoNewline(e);
    }
}
''',


IMPORTS='''
import java.util.List;
import java.util.ArrayList;
import org.python.pydev.parser.IGrammar;
import org.python.pydev.parser.grammarcommon.AbstractPythonGrammar;
import org.python.pydev.parser.grammarcommon.IJJTPythonGrammarState;
import org.python.pydev.parser.grammarcommon.AbstractTokenManager;
import org.python.pydev.parser.grammarcommon.JfpDef;
import org.python.pydev.parser.jython.CharStream;
import org.python.pydev.parser.jython.IParserHost;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.modType;
import org.python.pydev.parser.jython.TokenMgrError;
import org.python.pydev.parser.grammarcommon.EmptySuiteException;
import org.python.pydev.parser.grammarcommon.JJTPythonGrammarState;
'''
)

from string import Template

for file in files:
    
    s = Template(open(file, 'r').read())
    s = s.substitute(**DEFINITIONS)
    f = open(file[:-len('_template')], 'w')
    f.write(s)
    f.close()