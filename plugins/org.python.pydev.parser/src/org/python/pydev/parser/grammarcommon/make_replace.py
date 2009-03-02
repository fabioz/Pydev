from string import Template
import os
import sys
grammar_common_dir = os.path.split(__file__)[0]
parent_dir = os.path.split(grammar_common_dir)[0]




#=======================================================================================================================
# RunCog
#=======================================================================================================================
def RunCog():
    #Add cog to the pythonpath
    cog_dir = parent_dir[:parent_dir.index('plugins')]
    cog_src_dir = os.path.join(cog_dir, 'builders', 'org.python.pydev.build', 'cog_src')
    assert os.path.exists(cog_src_dir), '%s does not exist' % (cog_src_dir,)
    sys.path.append(cog_src_dir)
    
    import cog
    cog.RunCogInFiles([os.path.join(grammar_common_dir, 'AbstractTokenManagerWithConstants.java')])


#=======================================================================================================================
# CreateFileInput
#=======================================================================================================================
def CreateFileInput(NEWLINE):
    return '''
//file_input: (NEWLINE | stmt)* ENDMARKER
modType file_input(): {}
{
    ($NEWLINE | stmt())* try{<EOF>}catch(ParseException e){handleNoEof(e);}
    { return (modType) jjtree.popNode(); }
}
'''.replace('$NEWLINE', NEWLINE)


#=======================================================================================================================
# CreateNameDefinition
#=======================================================================================================================
def CreateNameDefinition():
    return '''
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
'''


#=======================================================================================================================
# CreateYield
#=======================================================================================================================
def CreateYield():
    return '''
//yield_expr: 'yield' [testlist]
void yield_expr(): {Object spStr;}
{ {spStr = createSpecialStr("yield","yield ", false);} <YIELD> [SmartTestList()] {this.addToPeek(spStr, false, Yield.class);}}
'''



#=======================================================================================================================
# CreateSuite
#=======================================================================================================================
def CreateSuite(NEWLINE):
    return '''
//suite: simple_stmt | NEWLINE INDENT stmt+ DEDENT
void suite(): {}
{ 

try{
        simple_stmt() 
    |  
    
        try{$NEWLINE<INDENT>}catch(ParseException e){handleErrorInIndent(e);}
        
        (try{stmt()}catch(ParseException e){handleErrorInStmt(e);})+ 
        
        try{<DEDENT>}catch(ParseException e){handleErrorInDedent(e);} 
    
    |
        try{
            <INDENT>
            {handleNoIndentInSuiteFound();}
            
            (try{stmt()}catch(ParseException e){handleErrorInStmt(e);})+ 
            
            try{<DEDENT>}catch(ParseException e){handleErrorInDedent(e);} 
        }catch(EmptySuiteException emptySuiteE){} //just close it 'gracefully'
    |
        try{
            {handleNoIndentInSuiteFound();}
            
            (try{stmt()}catch(ParseException e){handleErrorInStmt(e);})+ 
            
            try{<DEDENT>}catch(ParseException e){handleErrorInDedent(e);} 
        }catch(EmptySuiteException emptySuiteE){} //just close it 'gracefully'
    
        

}catch(ParseException e){handleNoSuiteMatch(e);}


}
'''.replace('$NEWLINE', NEWLINE)


#=======================================================================================================================
# CreateStmt
#=======================================================================================================================
def CreateStmt():
    return '''
//stmt: simple_stmt | compound_stmt
void stmt() #void: {}
{ simple_stmt() | try{compound_stmt()}catch(ParseException e){handleErrorInCompountStmt(e);} }
'''

#=======================================================================================================================
# CreateCommomMethods
#=======================================================================================================================
def CreateCommomMethods():
    return '''
    /**
     * @return the current token found.
     */
    protected final Token getCurrentToken() {
        return this.token;
    }
    
    /**
     * Sets the current token.
     */
    protected final void setCurrentToken(Token t) {
        this.token = t;
    }
    
    
    /**
     * @return the jjtree from this grammar
     */
    protected final IJJTPythonGrammarState getJJTree(){
        return jjtree;
    }


    /**
     * @return the special tokens in the token source
     */
    @SuppressWarnings("unchecked")
    protected final List<Object> getTokenSourceSpecialTokensList(){
        return token_source.specialTokens;
    }


    /**
     * @return the jj_lastpos
     */
    protected final Token getJJLastPos(){
        return jj_lastpos;
    }
'''





#=======================================================================================================================
# CreateCommomMethodsForTokenManager
#=======================================================================================================================
def CreateCommomMethodsForTokenManager():
    return '''
    
    
    /**
     * @return The current level of the indentation in the current line.
     */
    public int getCurrentLineIndentation(){
        return indent;
    }
    
    /**
     * @return The current level of the indentation.
     */
    public int getLastIndentation(){
        return indentation[level];
    }

    
    public final void indenting(int ind) {
        indent = ind;
        if (indent == indentation[level])
            SwitchTo(INDENTATION_UNCHANGED);
        else
            SwitchTo(INDENTING);
    }
'''
    



#=======================================================================================================================
# CreateSimpleStmt
#=======================================================================================================================
def CreateSimpleStmt(NEWLINE):
    return '''
//simple_stmt: small_stmt (';' small_stmt)* [';'] NEWLINE
void simple_stmt() #void: {}
{ 
    small_stmt() (LOOKAHEAD(2) <SEMICOLON> small_stmt())* 
    [<SEMICOLON>] 
    $NEWLINE
}
'''.replace('$NEWLINE', NEWLINE)


#=======================================================================================================================
# CreateImports
#=======================================================================================================================
def CreateImports():
    return '''
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


#=======================================================================================================================
# CreateDictMakerWithDeps
#=======================================================================================================================
def CreateDictMakerWithDeps(definitions):
    #Done later because it depends on others.
    DICTMAKER = '''
//dictmaker: test ':' test (',' test ':' test)* [',']
void dictmaker() #void: {}
{
    test() $COLON 
    
    try{
        test()
    }catch(ParseException e){
        handleNoValInDict(e);
    } 
    
    (LOOKAHEAD(2) $COMMA test() $COLON test())* 
    
    [$COMMA]}
'''


    DICTMAKER = Template(DICTMAKER)
    substituted = str(DICTMAKER.substitute(**definitions))
    return substituted
    
    
    

#=======================================================================================================================
# CreateIfWithDeps
#=======================================================================================================================
def CreateIfWithDeps(definitions):
    IF = '''
//if_stmt: 'if' test ':' suite ('elif' test ':' suite)* ['else' ':' suite]
void if_stmt(): {Object spStr;}
{
    {spStr = createSpecialStr("if","if ", false);}
    <IF> {this.addSpecialTokenToLastOpened(spStr);} test() $COLON suite()
         (begin_elif_stmt() test() $COLON suite())* 
             [ <ELSE> {this.findTokenAndAdd("else","else:",false);} <COLON> suite()]
}
'''

    IF = Template(IF)
    substituted = str(IF.substitute(**definitions))
    return substituted
    
    
#=======================================================================================================================
# CreateAssertWithDeps
#=======================================================================================================================
def CreateAssertWithDeps(definitions):
    ASSERT = '''
//assert_stmt: 'assert' test [',' test]
void assert_stmt(): {}
{ test() [$COMMA test()] }
'''

    ASSERT = Template(ASSERT)
    substituted = str(ASSERT.substitute(**definitions))
    return substituted

    
#=======================================================================================================================
# CreateAssert
#=======================================================================================================================
def CreateCallAssert():
    return '''<ASSERT>{spStr  = createSpecialStr("assert", "assert ", false);} assert_stmt() {addToPeek(spStr, false); }
'''

    
    
#=======================================================================================================================
# CreateGrammarFiles
#=======================================================================================================================
def CreateGrammarFiles():
    files = [
        os.path.join(parent_dir, 'grammar24', 'python.jjt_template'),
        os.path.join(parent_dir, 'grammar25', 'python.jjt_template'),
        os.path.join(parent_dir, 'grammar26', 'python.jjt_template'),
        os.path.join(parent_dir, 'grammar30', 'python.jjt_template'),
    ]
    
    NEWLINE = '''try{<NEWLINE>}catch(ParseException e){handleNoNewline(e);}'''
    
    definitions = dict(
        FILE_INPUT = CreateFileInput(NEWLINE),
        
        NEWLINE = NEWLINE,
        
        NAME_DEFINITION=CreateNameDefinition(),
        
        RPAREN ='''try{{this.findTokenAndAdd(")");}<RPAREN>}catch(ParseException e){handleRParensNearButNotCurrent(e);}''',
        
        COLON ='''{this.findTokenAndAdd(":");} <COLON>''',
        
        COMMA='''{this.findTokenAndAdd(",");} <COMMA>''',
        
        YIELD = CreateYield(),
        
        SUITE = CreateSuite(NEWLINE),
        
        STMT = CreateStmt(),
        
        SIMPLE_STMT=CreateSimpleStmt(NEWLINE),
        
        IMPORTS=CreateImports(),
        
        COMMOM_METHODS = CreateCommomMethods(),
        
        CALL_ASSERT = CreateCallAssert(),
        
        TOKEN_MGR_COMMOM_METHODS = CreateCommomMethodsForTokenManager(),
    )
    
    definitions['DICTMAKER'] = CreateDictMakerWithDeps(definitions)
    definitions['IF'] = CreateIfWithDeps(definitions)
    definitions['ASSERT'] = CreateAssertWithDeps(definitions)
    
    for file in files:
        s = Template(open(file, 'r').read())
        s = s.substitute(**definitions)
        f = open(file[:-len('_template')], 'w')
        f.write(s)
        f.close()
        
        
#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    RunCog()
    CreateGrammarFiles()
    
    