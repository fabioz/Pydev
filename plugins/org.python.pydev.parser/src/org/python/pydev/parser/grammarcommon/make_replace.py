from string import Template
import os
import sys
grammar_common_dir = os.path.split(__file__)[0]
parent_dir = os.path.split(grammar_common_dir)[0]




#=======================================================================================================================
# RunCog
#=======================================================================================================================
def RunCog():
    # Add cog to the pythonpath
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
def CreateNameDefinition(accept_as, accept_with, force_print_as_name):
    if accept_as:
        accept_as = '|(t=<AS>)'
    else:
        accept_as = ''

    if accept_with:
        accept_with = '|(t=<WITH>)'
        accept_with_part_2 = '''
        {
            if(acceptWithStmt && "with".equals(t.image)){
                throw withNameInvalidException;
            }
        }
        '''
    else:
        accept_with = ''
        accept_with_part_2 = ''

    ret = '''
Token Name() #Name:
{
    Token t;
}
{
    try{
        (t = <NAME>)%s%s
    }catch(ParseException e){
        t = handleErrorInName(e);
    }
    %s

        { ((Name)jjtThis).id = t.image; return t; } {}

}
'''
    if force_print_as_name:
        ret += '''


Token Name2() #Name:
{
    Token t;
}
{
    try{
        (t = <NAME>)|(t=<AS>)|(t=<PRINT>)
    }catch(ParseException e){
        t = handleErrorInName(e);
    }


        { ((Name)jjtThis).id = t.image; return t; } {}

}
'''

    return ret % (accept_as, accept_with, accept_with_part_2)


#=======================================================================================================================
# CreateYield
#=======================================================================================================================
def CreateYield():
    return '''
//yield_expr: 'yield' [testlist]
void yield_expr(): {Token spStr;}
{ spStr=<YIELD> [SmartTestList()] {this.grammarActions.addToPeek(spStr, false, Yield.class);}}
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
        <INDENT>
        {handleNoNewlineInSuiteFound();} //this only happens when we already had some error!

        (try{stmt()}catch(ParseException e){handleErrorInStmt(e);})+

        try{<DEDENT>}catch(ParseException e){handleErrorInDedent(e);}



}catch(ParseException e){
    handleNoSuiteMatch(e);

}catch(EmptySuiteException e){
    /*Just ignore: This was thrown in the handleErrorInIndent*/
}


}
'''.replace('$NEWLINE', NEWLINE)


#=======================================================================================================================
# CreateStmt
#=======================================================================================================================
def CreateStmt():
    return '''
//stmt: simple_stmt | compound_stmt
void stmt() #void: {}
{
        simple_stmt()
    |
        try{
            compound_stmt()
        }catch(ParseException e){
            handleErrorInCompountStmt(e);
        }
}
'''
#=======================================================================================================================
# CreateStmt25
#=======================================================================================================================
def CreateStmt25():
    return '''
//stmt: simple_stmt | compound_stmt
void stmt() #void: {}
{
        {Token curr = this.jj_lastpos;}
        try{
            simple_stmt()
        }catch(WithNameInvalidException e){
            while(!"with".equals(curr.next.image)){
                curr = curr.next;
                if(curr == null){
                    throw new RuntimeException("Unexpected: with not found when it should be already available");
                }
            }
            setCurrentToken(curr);
            with_stmt();
        }
    |
        try{
            compound_stmt()
        }catch(ParseException e){
            handleErrorInCompountStmt(e);
        }
}
'''

#=======================================================================================================================
# CreateCommomMethods
#=======================================================================================================================
def CreateCommomMethods():
    return '''

    private final FastStringBuffer dottedNameStringBuffer = new FastStringBuffer();

    private final ITreeBuilder builder;

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
    protected final AbstractJJTPythonGrammarState getJJTree(){
        return jjtree;
    }


    /**
     * @return the special tokens in the token source
     */
    public final List<Object> getTokenSourceSpecialTokensList(){
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
        return indentation.atLevel();
    }


    public final void indenting(int ind) {
        indent = ind;
        if (indent == indentation.atLevel())
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
    small_stmt() (LOOKAHEAD(2) temporaryToken=<SEMICOLON>{grammarActions.addSpecialToken(temporaryToken);} small_stmt())*
    [temporaryToken=<SEMICOLON>{grammarActions.addSpecialToken(temporaryToken);}]
    $NEWLINE
}
'''.replace('$NEWLINE', NEWLINE)


#=======================================================================================================================
# CreateImports
#=======================================================================================================================
def CreateImports():
    return '''
import java.util.ArrayList;
import java.util.List;
import org.python.pydev.parser.IGrammar;
import org.python.pydev.parser.grammarcommon.AbstractJJTPythonGrammarState;
import org.python.pydev.parser.grammarcommon.AbstractPythonGrammar;
import org.python.pydev.parser.grammarcommon.AbstractTokenManager;
import org.python.pydev.parser.grammarcommon.EmptySuiteException;
import org.python.pydev.parser.grammarcommon.IJJTPythonGrammarState;
import org.python.pydev.parser.grammarcommon.ITreeBuilder;
import org.python.pydev.parser.grammarcommon.JJTPythonGrammarState;
import org.python.pydev.parser.grammarcommon.JfpDef;
import org.python.pydev.parser.grammarcommon.WithNameInvalidException;
import org.python.pydev.parser.jython.CharStream;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.TokenMgrError;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.modType;
import org.python.pydev.shared_core.string.FastStringBuffer;
'''


#=======================================================================================================================
# CreateDictMakerWithDeps
#=======================================================================================================================
def CreateDictMakerWithDeps(definitions):
    # Done later because it depends on others.
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
# CreateDictOrSetMakerWithDeps
#=======================================================================================================================
def CreateDictOrSetMakerWithDeps(definitions):
    # Done later because it depends on others.
    DICTMAKER = '''
//dictorsetmaker: (
//                   (test ':' test (comp_for | (',' test ':' test)* [',']))
//                  |(test (comp_for | (',' test)* [',']))
//                )
void dictorsetmaker() #void: {}
{
    test()

    (
        (
            $COLON
            try{
                test()
            }catch(ParseException e){
                handleNoValInDict(e);
            }
            (
                comp_for()
                |
                (LOOKAHEAD(2) $COMMA test()$COLON test())*[$COMMA]
            )
        )
        |
        (
          (LOOKAHEAD(2) comp_for() | ($COMMA [test()])* #set)
        )
    )
}

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
void if_stmt(): {Object[] elseToks;}
{
    temporaryToken=<IF> {this.markLastAsSuiteStart();} {grammarActions.addSpecialTokenToLastOpened(temporaryToken);} test() $COLON suite()
         (begin_elif_stmt() test() $COLON suite())*
             [ elseToks=begin_else_stmt() suite() {grammarActions.addToPeek(elseToks[0], false, Suite.class);grammarActions.addToPeek(elseToks[1], false, Suite.class);}]
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
# CreateExec
#=======================================================================================================================
def CreateExec(definitions):
    EXEC = '''
//exec_stmt: 'exec' expr ['in' test [',' test]]
void exec_stmt(): {}
{ temporaryToken=<EXEC>{grammarActions.addSpecialTokenToLastOpened(temporaryToken);} expr() [temporaryToken=<IN>{grammarActions.addSpecialToken(temporaryToken);} test() [$COMMA test()]] }
'''

    EXEC = Template(EXEC)
    substituted = str(EXEC.substitute(**definitions))
    return substituted


#=======================================================================================================================
# CreateImportStmt
#=======================================================================================================================
def CreateImportStmt():
    return '''
//import_stmt: 'import' dotted_name (',' dotted_name)* | 'from' dotted_name 'import' ('*' | NAME (',' NAME)*)
void import_stmt() #void: {Import imp; Object spStr;}
{
    try{
        spStr=<IMPORT> imp = Import() {if(imp!=null){imp.addSpecial(spStr,false);}}
        |
        {temporaryToken=grammarActions.createSpecialStr("from");}<FROM> {grammarActions.addSpecialToken(temporaryToken,STRATEGY_BEFORE_NEXT);} ImportFrom()
    }catch(ParseException e){handleErrorInImport(e);}
}
'''



#=======================================================================================================================
# CreateCallAssert
#=======================================================================================================================
def CreateCallAssert():
    return '''temporaryToken=<ASSERT> assert_stmt() {grammarActions.addToPeek(temporaryToken, false); }
'''


#=======================================================================================================================
# CreatePy3KWithStmt
#=======================================================================================================================
def CreatePy3KWithStmt(definitions):
    PY3K_WITH_STMT = '''
//with_stmt: 'with' with_item (',' with_item)*  ':' suite
void with_stmt(): {}
{ <WITH>
    {grammarActions.addSpecialToken("with ", STRATEGY_BEFORE_NEXT); }

    with_item()
    ($COMMA with_item())*

    $COLON suite()
}

//with_item: test ['as' expr]
void with_item():{}
{ test() [$AS2 expr()]}

'''
    PY3K_WITH_STMT = Template(PY3K_WITH_STMT)
    substituted = str(PY3K_WITH_STMT.substitute(**definitions))
    return substituted



#=======================================================================================================================
# CreateIndenting
#=======================================================================================================================
def CreateIndenting():
    return '''
<INDENTING> TOKEN :
{
    <DEDENT: "">
        {
            if (indent > indentation.atLevel()) {
                indentation.pushLevel(indent);
                matchedToken.kind=INDENT;
                matchedToken.image = "<INDENT>";
            }
            else if (indentation.level > 0) {
                Token t = matchedToken;
                indentation.level -= 1;
                while (indentation.level > 0 && indent < indentation.atLevel()) {
                    indentation.level--;
                    t = addDedent(t);
                }
                if (indent != indentation.atLevel()) {
                    throw new TokenMgrError("inconsistent dedent",
                                            t.endLine, t.endColumn);
                }
                t.next = null;
            }
        } : DEFAULT
}
'''


#===============================================================================
# CreateWhileWithDeps
#===============================================================================
def CreateWhileWithDeps(definitions):
    WHILE = '''
//while_stmt: 'while' test ':' suite ['else' ':' suite]
void while_stmt(): {Object[] elseToks;}
{ begin_while_stmt() test() $COLON suite()
  [ elseToks=begin_else_stmt()  suite() {grammarActions.addToPeek(elseToks[0], false, Suite.class);grammarActions.addToPeek(elseToks[1], false, Suite.class);}] }

void begin_while_stmt(): {}
{ temporaryToken=<WHILE>{grammarActions.addSpecialToken(temporaryToken,STRATEGY_BEFORE_NEXT);} {this.markLastAsSuiteStart();}
}
'''
    WHILE = Template(WHILE)
    substituted = str(WHILE.substitute(**definitions))
    return substituted


#===============================================================================
# CreateBeginElseWithDeps
#===============================================================================
def CreateBeginElseWithDeps(definitions):
    BEGIN_ELSE = '''
Object[] begin_else_stmt(): {Object o1, o2;}
{ o1=<ELSE> o2=<COLON>{return new Object[]{o1, o2};}
}
'''
    BEGIN_ELSE = Template(BEGIN_ELSE)
    substituted = str(BEGIN_ELSE.substitute(**definitions))
    return substituted



#=======================================================================================================================
# CreateGrammarFiles
#=======================================================================================================================
def CreateGrammarFiles():

    NEWLINE = '''try{<NEWLINE>}catch(ParseException e){handleNoNewline(e);}'''

    definitions = dict(
        FILE_INPUT=CreateFileInput(NEWLINE),

        NEWLINE=NEWLINE,

        RPAREN='''try{{grammarActions.findTokenAndAdd(")");}<RPAREN> }catch(ParseException e){handleRParensNearButNotCurrent(e);}''',

        COLON='''{grammarActions.findTokenAndAdd(":");}<COLON>''',

        AT='''temporaryToken=<AT>  {grammarActions.addSpecialToken(temporaryToken, STRATEGY_BEFORE_NEXT);}''',

        COMMA='''{grammarActions.findTokenAndAdd(",");}<COMMA>''',

        YIELD=CreateYield(),

        SUITE=CreateSuite(NEWLINE),

        SIMPLE_STMT=CreateSimpleStmt(NEWLINE),

        IMPORTS=CreateImports(),

        COMMOM_METHODS=CreateCommomMethods(),

        CALL_ASSERT=CreateCallAssert(),

        TOKEN_MGR_COMMOM_METHODS=CreateCommomMethodsForTokenManager(),

        IMPORT_STMT=CreateImportStmt(),

        INDENTING=CreateIndenting(),

        RAISE='''{temporaryToken=grammarActions.createSpecialStr("raise");}<RAISE> {grammarActions.addSpecialToken(temporaryToken, STRATEGY_BEFORE_NEXT);}''',

        DEF_START='''<DEF> {this.markLastAsSuiteStart();} Name()''',

        LPAREN1='''{temporaryToken=grammarActions.createSpecialStr("(");}<LPAREN>  {grammarActions.addSpecialToken(temporaryToken, STRATEGY_BEFORE_NEXT);}''',

        LPAREN2='''{grammarActions.findTokenAndAdd("(");}<LPAREN>''',

        PASS_STMT='''//pass_stmt: 'pass'
Token pass_stmt(): {Token spStr;}
{ spStr=<PASS> {return spStr;}}''',

        LPAREN3='''{temporaryToken=grammarActions.createSpecialStr("(");}<LPAREN>  {grammarActions.addSpecialToken(temporaryToken, STRATEGY_ADD_AFTER_PREV);}''',

        DELL_STMT='''//del_stmt: 'del' exprlist
void del_stmt(): {}
{ begin_del_stmt() exprlist() }

void begin_del_stmt(): {}
{ temporaryToken=<DEL> {this.grammarActions.addToPeek(temporaryToken,false);}
}
''',

        LAMBDA_COLON='''{temporaryToken=grammarActions.createSpecialStr(":");}<COLON> {
if(hasArgs)
    grammarActions.addSpecialToken(temporaryToken);
else
    grammarActions.addSpecialToken(temporaryToken,STRATEGY_BEFORE_NEXT);}
''',

        START_CLASS='''<CLASS> {this.markLastAsSuiteStart();} Name()''',

        EQUAL='''{grammarActions.findTokenAndAdd("=");}<EQUAL>''',

        EQUAL2='''{temporaryToken=grammarActions.createSpecialStr("=");}<EQUAL> {grammarActions.addSpecialToken(temporaryToken, STRATEGY_BEFORE_NEXT);}''',

        IN='''{grammarActions.findTokenAndAdd("in");}<IN> ''',

        IF_COMP='''{grammarActions.findTokenAndAdd("if");}<IF>''',

        FOR_COMP='''{grammarActions.findTokenAndAdd("for");}<FOR>''',

        IMPORT='''{grammarActions.findTokenAndAdd("import");}<IMPORT>''',

        AS='''{grammarActions.findTokenAndAdd("as");}<AS>''',

        DOTTED_NAME='''
//dotted_name: NAME ('.' NAME)*
String dotted_name(): { Token t; FastStringBuffer sb = dottedNameStringBuffer.clear(); }
{ t=Name() { sb.append(t.image); }
    (<DOT> t=Name() { sb.append(".").append(t.image); } )*
        { return sb.toString(); }
}
''',

        DOTTED_NAME_ACCEPTING_PRINT='''
//dotted_name: NAME ('.' NAME)*
String dotted_name(): { Token t; FastStringBuffer sb = dottedNameStringBuffer.clear(); }
{ t=Name() { sb.append(t.image); }
    (<DOT> t=Name2() { sb.append(".").append(t.image); } )*
        { return sb.toString(); }
}
''',

        AS2='''{temporaryToken=grammarActions.createSpecialStr("as");}<AS> {grammarActions.addSpecialToken(temporaryToken, STRATEGY_BEFORE_NEXT);}''',

        IF_EXP='''void if_exp():{}
{{temporaryToken=grammarActions.createSpecialStr("if");}<IF> {grammarActions.addSpecialToken(temporaryToken,STRATEGY_ADD_AFTER_PREV);} or_test() {grammarActions.findTokenAndAdd("else");}<ELSE> test()}''',

        GLOBAL='''temporaryToken=<GLOBAL> {grammarActions.addSpecialToken(temporaryToken, STRATEGY_BEFORE_NEXT);}''',

        BACKQUOTE='''"`" SmartTestList() "`" #str_1op(1)''',

        SLICE='''//sliceop: ':' [test]
void slice() #void: {}
{ Colon() [test()] (Colon() [test()])? }
''',
    )

    definitions['EXEC'] = CreateExec(definitions)
    definitions['DICTMAKER'] = CreateDictMakerWithDeps(definitions)
    definitions['DICTORSETMAKER'] = CreateDictOrSetMakerWithDeps(definitions)
    definitions['IF'] = CreateIfWithDeps(definitions)
    definitions['ASSERT'] = CreateAssertWithDeps(definitions)
    definitions['WHILE'] = CreateWhileWithDeps(definitions)
    definitions['BEGIN_ELSE'] = CreateBeginElseWithDeps(definitions)
    definitions['PY3K_WITH_STMT'] = CreatePy3KWithStmt(definitions)


    files = [
        (os.path.join(parent_dir, 'grammar24', 'python.jjt_template'), 24),
        (os.path.join(parent_dir, 'grammar25', 'python.jjt_template'), 25),
        (os.path.join(parent_dir, 'grammar26', 'python.jjt_template'), 26),
        (os.path.join(parent_dir, 'grammar27', 'python.jjt_template'), 27),
        (os.path.join(parent_dir, 'grammar30', 'python.jjt_template'), 30),
    ]

    for file, version in files:
        if version == 24:
            definitions['NAME_DEFINITION'] = CreateNameDefinition(True, False, True)
        elif version == 25:
            definitions['NAME_DEFINITION'] = CreateNameDefinition(True, True, True)
        else:
            definitions['NAME_DEFINITION'] = CreateNameDefinition(False, False, False)

        if version == 25:
            definitions['STMT'] = CreateStmt25()

        else:
            definitions['STMT'] = CreateStmt()

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

