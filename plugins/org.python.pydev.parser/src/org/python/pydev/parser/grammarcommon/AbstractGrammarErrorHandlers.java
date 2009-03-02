package org.python.pydev.parser.grammarcommon;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.Token;

/**
 * This class contains the error-handling utilities.
 * 
 * @author Fabio
 */
public abstract class AbstractGrammarErrorHandlers extends AbstractGrammarWalkHelpers{

    public static boolean DEBUG = false;
    public final static boolean DEBUG_SHOW_PARSE_ERRORS = PyParser.DEBUG_SHOW_PARSE_ERRORS;
    public final static boolean DEBUG_SHOW_LOADED_TOKENS = false;


    /**
     * @return the actual jjtree used to build the nodes (tree)
     */
    protected abstract IJJTPythonGrammarState getJJTree();

    /**
     * List with the errors we handled during the parsing
     */
    private List<ParseException> parseErrors = new ArrayList<ParseException>();
    
    /**
     * @return a list with the parse errors. Note that the returned value is not a copy, but the actual
     * internal list used to store the errors.
     */
    public List<ParseException> getParseErrors(){
        return parseErrors;
    }
    

    /**
     * Adds some parse exception to the list of parse exceptions found.
     */
    protected void addParseError(ParseException e) {
        parseErrors.add(e);
    }

    /**
     * @return the 1st error that happened while parsing (or null if no error happened)
     */
    public Throwable getErrorOnParsing() {
        if(this.parseErrors != null && this.parseErrors.size() > 0){
            return this.parseErrors.get(0);
        }
        return null;
    }
    
    //---------------------------- Helpers to handle errors in the grammar.

    protected final void addAndReport(ParseException e, String msg){
        if(DEBUG_SHOW_PARSE_ERRORS){
            
            System.err.println("\n\n\n\n\n---------------------------------\n"+msg);
            e.printStackTrace();
        }
        addParseError(e);
    }
    
    /**
     * Called when there was an error trying to indent.
     */
    protected final void handleErrorInIndent(ParseException e) throws ParseException{
        addAndReport(e, "Handle indent");
    }
    
    /**
     * Called when there was an error trying to indent.
     */
    protected final void handleNoEof(ParseException e) throws ParseException{
        addAndReport(e, "Handle no EOF");
    }
    
    /**
     * Happens when we could find a parenthesis close (so, we don't create it), but it's
     * not the current, so, we have an error making the match.
     */
    protected final void handleRParensNearButNotCurrent(ParseException e) {
        addAndReport(e, "Handle parens near but not current");
        Token t = getCurrentToken();
        
        AbstractTokenManager tokenManager = getTokenManager();
        final int rparenId = tokenManager.getRparenId();
        while (t != null && t.kind != rparenId) {
            t = t.next;
        }
        if(t != null && t.kind == rparenId){
            //found it
            setCurrentToken(t);
        }
    }
    
    /**
     * Called when there was an error trying to dedent.
     */
    protected final void handleErrorInDedent(ParseException e) throws ParseException{
        addAndReport(e, "Handle dedent");
        final AbstractTokenManager tokenManager = this.getTokenManager();
        
        //go to the next dedent we were expecting...
        while(true){
            boolean foundNewLine = searchNewLine(tokenManager, false);
            if(foundNewLine){
                tokenManager.indenting(0);
                final Token nextToken = tokenManager.getNextToken();
                if(nextToken.kind == tokenManager.getDedentId()){
                    setCurrentToken(nextToken);
                    break;
                }
            }else{
                break;
            }
        }
    }
    
    protected final void handleErrorInStmt(ParseException e) throws ParseException{
        
    }
    

    /**
     * Called when there was an error while resolving a statement.
     */
    protected final void handleErrorInCompountStmt(ParseException e) throws ParseException{
        addAndReport(e, "Handle error in compount stmt");
        
//        AbstractTokenManager tokenManager = getTokenManager();
//        final int eofId = tokenManager.getEofId();
//        final int defId = tokenManager.getDefId();
//        
//        final Token firstToken = nextTokenConsideringNewLine(tokenManager);
//        Token nextToken = null;
//        while(true){
//            if(nextToken == null){
//                nextToken = firstToken;
//            }else{
//                nextToken = nextTokenConsideringNewLine(tokenManager);
//            }
//            if(nextToken.kind == tokenManager.getDedentId()){
//                if(DEBUG_SHOW_LOADED_TOKENS){
//                    System.out.println("<DEDENT>");
//                }
////                if(nextToken.next.kind == defId){
//                    setCurrentToken(nextToken);
//                    break;
////                }
//            }else{
//                if(DEBUG_SHOW_LOADED_TOKENS){
//                    System.out.println(nextToken);
//                }
//            }
//            
//            if(nextToken.kind == defId){
//                int lastIndentation = tokenManager.getLastIndentation();
//                int currentIndentation = tokenManager.getCurrentLineIndentation();
//                if(DEBUG_SHOW_LOADED_TOKENS){
//                    System.out.println("Current Indentation: "+currentIndentation+" - last: "+lastIndentation);
//                }
//            }
//            
//            
//            if(nextToken.kind == eofId){
//                setCurrentToken(firstToken);
//                break;
//            }
//        }
    }
    
    /**
     * Called when there was an error while resolving a statement.
     */
    protected final void handleNoNewline(ParseException e) throws ParseException{
        addAndReport(e, "Handle no newline");
    }
    
    /**
     * Called when there was an error because the value for a given key was not found.
     */
    protected final void handleNoValInDict(ParseException e) throws ParseException{
        addAndReport(e, "No value for dict key");
    }
    
    
    /**
     * This is called when recognized a suite without finding its indent.
     * 
     * @throws EmptySuiteException if it was called when an empty suite was actually matched (and thus, we should
     * go out of the suite context).
     */
    protected final void handleNoIndentInSuiteFound() throws EmptySuiteException{
        Token currentToken = getCurrentToken();
        addAndReport(new ParseException("No indent found.", currentToken), "Handle no indent in suite");
        
        JJTPythonGrammarState tree = (JJTPythonGrammarState) this.getJJTree();
        if(tree.lastIsNewScope()){
            //this is something like:
            //class A(ueo
            //def m1
            //where the def is out of the suite scope
            throw new EmptySuiteException();
        }
    }
    
    

    
    protected final void handleNoSuiteMatch(ParseException e){
        addAndReport(e, "Handle no suite match");
    }

    
    /**
     * Called when there was an error trying to indent.
     * 
     * Actually creates a name so that the parsing can continue.
     */
    protected final Token handleErrorInName(ParseException e) throws ParseException{
        addAndReport(e, "Handle name");
        Token currentToken = getCurrentToken();
        
        return this.getTokenManager().createFrom(currentToken, this.getTokenManager().getNameId(), "!<MissingName>!");
    }
    
}
