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
public class FastParser {

    private static final exprType[] EMTPY_EXPR_TYPE = new exprType[0];

    private static final decoratorsType[] EMTPY_DECORATORS_TYPE = new decoratorsType[0];

    private static final stmtType[] EMTPY_STMT_TYPE = new stmtType[0];

    //spaces* 'def' space+ identifier
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(\\s+|^)(def\\s+)(\\w*)");

    //spaces* 'class' space+ identifier
    private static final Pattern CLASS_PATTERN = Pattern.compile("(\\s+|^)(class\\s+)(\\w*)");
    

    //constant with the prefix position in the pattern (class or def)
    private static final int PREFIX_GROUP = 2;
    
    //constant with the name position in the pattern
    private static final int NAME_GROUP = 3;
    
    
    /**
     * @param doc the document to be parsed
     * @return a list of statements with the classes and functions for this document
     */
    public static List<stmtType> parseClassesAndFunctions(IDocument doc) {
        return parseClassesAndFunctions(doc, 0, true, false);
    }
    
    /**
     * @param doc the document to be parsed
     * @param currentLine the line where the parsing should begin (inclusive)
     * @param forward determines whether we should be iterating forward or backward
     * @param stopOnFirstMatch if true, will return right after getting the 1st match
     * @return a list of statements with the classes and functions for this document
     */
    private static List<stmtType> parseClassesAndFunctions(IDocument doc, int currentLine, boolean forward, boolean stopOnFirstMatch) {
        List<stmtType> body = new ArrayList<stmtType>();
        PySelection ps = new PySelection(doc);
        DocIterator it = new PySelection.DocIterator(forward, ps, currentLine, false);
        
        Matcher functionMatcher = FUNCTION_PATTERN.matcher("");
        Matcher classMatcher = CLASS_PATTERN.matcher("");
        
        while(it.hasNext()){
            String line = it.next();
            
            functionMatcher.reset(line);
            
            if(functionMatcher.find()){
                int lastReturnedLine = it.getLastReturnedLine();
                NameTok nameTok = createNameTok(functionMatcher, lastReturnedLine, NameTok.FunctionName, ps);
                
                if(nameTok != null){
                    argumentsType args = new argumentsType(EMTPY_EXPR_TYPE, null, null, EMTPY_EXPR_TYPE,
                            null, null, null, null, null, null);
                    FunctionDef functionDef = new FunctionDef(nameTok, args, EMTPY_STMT_TYPE, EMTPY_DECORATORS_TYPE, null);
                    functionDef.beginLine = lastReturnedLine+1;
                    functionDef.beginColumn = functionMatcher.start(PREFIX_GROUP)+1;
                    
                    body.add(functionDef);
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
                    ClassDef classDef = new ClassDef(nameTok, EMTPY_EXPR_TYPE, EMTPY_STMT_TYPE, null, null, null, null);
                    classDef.beginLine = lastReturnedLine+1;
                    classDef.beginColumn = classMatcher.start(PREFIX_GROUP)+1;
                    
                    body.add(classDef);
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
     * @param doc the document where the search should take place
     * @param currentLine the line where the parsing should begin (inclusive)
     * @param forward determines if the search should be forward or backward in the document considering the
     * current position.
     * @return the first class or function definition found on the given document
     */
    public static stmtType firstClassOrFunction(IDocument doc, int currentLine, boolean forward) {
        List<stmtType> found = parseClassesAndFunctions(doc, currentLine, forward, true);
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
    private static NameTok createNameTok(Matcher matcher, int lastReturnedLine, int type, PySelection ps) {
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
