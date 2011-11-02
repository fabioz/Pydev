/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.fastparser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.DocIterator;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;

/**
 * This class is able to obtain the classes and function definitions as a tree structure (only filled with
 * classes and methods).
 *
 * @author Fabio
 */
public final class FastParser {

    private static final exprType[] EMTPY_EXPR_TYPE = new exprType[0];

    private static final decoratorsType[] EMTPY_DECORATORS_TYPE = new decoratorsType[0];

    private static final stmtType[] EMTPY_STMT_TYPE = new stmtType[0];

    //spaces* 'def' space+ identifier
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(\\s+|^)(def\\s+)(\\w*)");
    
    private static final Pattern FUNCTION_PATTERN_CYTHON = Pattern.compile("(\\s+|^)(cdef\\s+)(.*)");
    private static final Pattern FUNCTION_PATTERN_CYTHON2 = Pattern.compile("(\\s+|^)(ctypedef\\s+)(.*)");

    //spaces* 'class' space+ identifier
    private static final Pattern CLASS_PATTERN = Pattern.compile("(\\s+|^)(class\\s+)(\\w*)");
    

    //constant with the prefix position in the pattern (class or def)
    @SuppressWarnings("unused")
    private static final int PREFIX_GROUP = 2;
    
    //constant with the name position in the pattern
    private static final int NAME_GROUP = 3;
    

    /**
     * doc the document to be parsed
     */
    private IDocument doc;
    
    /**
     * currentLine the line where the parsing should begin (inclusive -- starts at 0)
     */
    private int currentLine; 
    
    /**
     * forward determines whether we should be iterating forward or backward
     */
    private boolean forward;
    
    /**
     * stopOnFirstMatch if true, will return right after getting the 1st match
     */
    private boolean stopOnFirstMatch;
    
    /**
     * If true, we'll stop when we're able to find the match to the globally accessible way for the current line.
     */
    private boolean findGloballyAccessiblePath;

    private int firstCharCol = -1;
    
    private boolean cythonParse = false;

    
    /**
     * Use the parse* methods to access what you need to create the parse.
     */
    private FastParser(IDocument doc, int currentLine, boolean forward, boolean stopOnFirstMatch){
        this.doc = doc;
        this.currentLine = currentLine;
        this.forward = forward;
        this.stopOnFirstMatch = stopOnFirstMatch;
    }
    
    /**
     * @param doc the document to be parsed
     * @return a list of statements with the classes and functions for this document
     */
    public static List<stmtType> parseClassesAndFunctions(IDocument doc) {
        return new FastParser(doc, 0, true, false).parse();
    }
    
    
    /**
     * @param doc the document to be parsed
     * @return a list of statements with the classes and functions for this document
     */
    public static List<stmtType> parseCython(IDocument doc) {
        FastParser fastParser = new FastParser(doc, 0, true, false);
        fastParser.cythonParse = true;
        return fastParser.parse();
    }
    

    /**
     * Note: Used from jython scripts.
     * 
     * @param doc the document to be parsed
     * @param currentLine the line where the parsing should begin (inclusive -- starts at 0)
     * @return the path to the current statement (where the current is the last element and the top-level is the 1st).
     * If it's empty that means that we're already in the top-level.
     */
    public static List<stmtType> parseToKnowGloballyAccessiblePath(IDocument doc, int currentLine) {
        FastParser parser = new FastParser(doc, currentLine, false, false);
        parser.findGloballyAccessiblePath = true;
        return parser.parse();
    }

    
    /**
     * @param doc the document to be parsed
     * @param currentLine the line where the parsing should begin (inclusive -- starts at 0)
     * @param forward determines whether we should be iterating forward or backward
     * @param stopOnFirstMatch if true, will return right after getting the 1st match
     * @return a list of statements with the classes and functions for this document
     */
    private static List<stmtType> parseClassesAndFunctions(IDocument doc, int currentLine, boolean forward, boolean stopOnFirstMatch) {
        return new FastParser(doc, currentLine, forward, stopOnFirstMatch).parse();
    }
    
    
    private List<stmtType> parse() {
        List<stmtType> body = new ArrayList<stmtType>();
        PySelection ps = new PySelection(doc);
        DocIterator it = new PySelection.DocIterator(forward, ps, currentLine, false);
        
        Matcher functionMatcher = FUNCTION_PATTERN.matcher("");
        List<Matcher> cythonMatchers = null;
        if(this.cythonParse){
            cythonMatchers = new ArrayList<Matcher>();
            cythonMatchers.add(FUNCTION_PATTERN_CYTHON.matcher(""));
            cythonMatchers.add(FUNCTION_PATTERN_CYTHON2.matcher(""));
        }
        
        
        Matcher classMatcher = CLASS_PATTERN.matcher("");
        
        
        while(it.hasNext()){
            Matcher functionFound = null;
            String line = it.next();
            
            //we don't care about empty lines
            if(line.trim().length() == 0){
                continue;
            }
            
            if(findGloballyAccessiblePath){
            	int currentFirstCharCol = PySelection.getFirstCharPosition(line);
	            if(firstCharCol == -1){
	                firstCharCol = currentFirstCharCol;
	            }else{
	            	//We must validate if this is a line we can accept based on the initial indentation
	            	//E.g.:
	            	//
	            	//def m1():
	            	//    def m2():
	            	//        pass
	            	//    pass <- If we're here, m2() should not be considered when getting the path
	            	//            to the global scope.
	            	if(firstCharCol <= currentFirstCharCol){
	            		continue; // don't check this line as it's not valid in the current context.
	            	}
	            }
            }
            
            
            functionMatcher.reset(line);
            if(functionMatcher.find()){
                functionFound = functionMatcher;
            }else if(cythonMatchers != null){
                for (Matcher matcher : cythonMatchers) {
                    matcher.reset(line);
                    if(matcher.find()){
                        functionFound = matcher;
                        break;
                    }
                }
            }
            
            if(functionFound != null){
                int lastReturnedLine = it.getLastReturnedLine();
                NameTok nameTok = createNameTok(functionFound, lastReturnedLine, NameTok.FunctionName, ps);
                
                if(nameTok != null){
                    FunctionDef functionDef = createFunctionDef(lastReturnedLine, nameTok, PySelection.getFirstCharPosition(line)); 
                    
                    if(!addStatement(body, functionDef)){
                        return body;
                    }
                    
                    if(stopOnFirstMatch){
                        return body;
                    }
                }
                continue;
            }
            
            classMatcher.reset(line);
            
            if(classMatcher.find()){
                int lastReturnedLine = it.getLastReturnedLine();
                
                NameTok nameTok = createNameTok(classMatcher, lastReturnedLine, NameTok.ClassName, ps);
                
                if(nameTok != null){
                    ClassDef classDef = createClassDef(lastReturnedLine, nameTok, PySelection.getFirstCharPosition(line)); 
                    
                    if(!addStatement(body, classDef)){
                        return body;
                    }
                    
                    if(stopOnFirstMatch){
                        return body;
                    }
                }
                continue;
            }
            
        }
        
        return body;
    }

    /**
     * @return whether we should continue iterating.
     */
    private boolean addStatement(List<stmtType> body, stmtType stmt) {
        if(!findGloballyAccessiblePath){
            body.add(stmt);
            return true;
        }else{
            if(body.size() > 0){
                if(stmt.beginColumn == body.get(0).beginColumn){
                  //don't add one that's in the same column of the last found (we need only the path to the parent, not siblings)
                    return true; 
                }
            }
            body.add(0, stmt);
            if(stmt.beginColumn == 1){
                //gotten to root
                return false;
            }
            return true;
        }
    }

    private FunctionDef createFunctionDef(int lastReturnedLine, NameTok nameTok, int matchedCol) {
        argumentsType args = new argumentsType(EMTPY_EXPR_TYPE, null, null, EMTPY_EXPR_TYPE,
                null, null, null, null, null, null);
        FunctionDef functionDef = new FunctionDef(nameTok, args, EMTPY_STMT_TYPE, EMTPY_DECORATORS_TYPE, null);
        functionDef.beginLine = lastReturnedLine+1;
        functionDef.beginColumn = matchedCol+1;
        return functionDef;
    }

    private ClassDef createClassDef(int lastReturnedLine, NameTok nameTok, int matchedCol) {
        ClassDef classDef = new ClassDef(nameTok, EMTPY_EXPR_TYPE, EMTPY_STMT_TYPE, null, null, null, null);
        classDef.beginLine = lastReturnedLine+1;
        classDef.beginColumn = matchedCol+1;
        return classDef;
    }

    
    /**
     * @param doc the document where the search should take place
     * @param currentLine the line where the parsing should begin (inclusive)
     * @param forward determines if the search should be forward or backward in the document considering the
     * current position.
     * @return the first class or function definition found on the given document
     */
    public static stmtType firstClassOrFunction(IDocument doc, int currentLine, boolean forward, boolean isCython) {
        boolean stopOnFirstMatch = true;
        FastParser fastParser = new FastParser(doc, currentLine, forward, stopOnFirstMatch);
        fastParser.cythonParse = isCython;
        List<stmtType> found = fastParser.parse(); 
        if(found.size() > 0){
            return found.get(0);
        }
        return null;
    }

    
    /**
     * @param matcher this is the class that just matched the class or function
     * @param lastReturnedLine the line it has done the match
     * @param type the type of the name token (@see NameTok constants)
     * @param ps the pyselection that has the document
     * @return null if the location is not a valid location for a function or class or a NameTok to
     * be used with the ClassDef / FunctionDef
     */
    private NameTok createNameTok(Matcher matcher, int lastReturnedLine, int type, PySelection ps) {
        int col = matcher.start(NAME_GROUP);
        
        int absoluteCursorOffset = ps.getAbsoluteCursorOffset(lastReturnedLine, col);
        if(ParsingUtils.getContentType(ps.getDoc(), absoluteCursorOffset) != IPythonPartitions.PY_DEFAULT){
            return null;
        }

        NameTok nameTok = new NameTok(matcher.group(NAME_GROUP), type);
        nameTok.beginLine = lastReturnedLine+1;
        nameTok.beginColumn = col+1;
        return nameTok;
    }


}
