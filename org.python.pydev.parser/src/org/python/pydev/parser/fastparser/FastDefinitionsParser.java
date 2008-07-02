package org.python.pydev.parser.fastparser;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.stmtType;

/**
 * @note: Unfinished
 * 
 * This class should be able to gather the definitions found in a module in a very fast way.
 * 
 * The target is having a performance around 5x faster than doing a regular parse, focusing on getting
 * the name tokens for:
 * 
 * classes, functions, class attributes, instance attributes -- basically the tokens that provide a 
 * definition that can be 'globally' accessed.
 *
 * @author Fabio
 */
public class FastDefinitionsParser {
    
    /**
     * Set and kept in the constructor
     */
    
    /**
     * The chars we should iterate through.
     */
    final private char[] cs;
    
    /**
     * The length of the buffer we're iterating.
     */
    final private int length;
    
    /**
     * Last char we found
     */
    private char lastChar = '\n';
    
    /**
     * Current iteration index
     */
    private int currIndex = 0;
    
    /**
     * The current column
     */
    private int col;
    
    /**
     * The current row
     */
    private int row = 0;
    
    /**
     * The column where the 1st char was found
     */
    private int firstCharCol = 1;
    
    /**
     * Holds things added to the 'global' module
     */
    private final ArrayList<stmtType> body = new ArrayList<stmtType>();
    
    /**
     * Holds a stack of classes so that we create a new one in each new scope to be filled and when the scope is ended,
     * it should have its body filled with the stackBody contents related to each
     */
    private final FastStack<ClassDef> stack = new FastStack<ClassDef>();
    
    /**
     * For each item in the stack, there's a stackBody that has the contents to be added later to that class.
     */
    private final FastStack<List<stmtType>> stackBody = new FastStack<List<stmtType>>();

    /**
     * Buffer with the contents of a line.
     */
    private final FastStringBuffer lineBuffer = new FastStringBuffer();
    
    /**
     * Should we debug?
     */
    private final static boolean DEBUG = false;
    
    
    /**
     * Constructor
     * 
     * @param cs array of chars that should be filled.
     */
    private FastDefinitionsParser(char[] cs){
        this.cs = cs;
        this.length = cs.length;
    }
    
    
    /**
     * This is the method that actually extracts things from the passed buffer.
     */
    private void extractBody() {
        if(currIndex < length){
            handleNewLine();
        }
        
        for (;currIndex < length; currIndex++, col++) {
            char c = cs[currIndex];
            
            switch (c){
            
                case '\'':
                case '"': 
                    if(DEBUG){
                        System.out.println("literal");
                    }
                    //go to the end of the literal
                    currIndex = ParsingUtils.getLiteralEnd(cs, currIndex, c);
                    break;
                    
                    
                    
                case '#': 
                    if(DEBUG){
                        System.out.println("comment");
                    }
                    //go to the end of the comment
                    currIndex++;
                    OUT:
                    while(currIndex < length){
                        c = cs[currIndex];
                        currIndex++;
                        switch(c){
                            case '\r': 
                                if(currIndex < length-1 && cs[currIndex+1] == '\n'){
                                    currIndex++;
                                }
                                /*FALLTHROUGH**/
                            case '\n': 
                                break OUT;
                        }
                    }
                    
                    //after a comment, we'll always be in a new line
                    currIndex++;
                    handleNewLine();
                    
                    break;
                    
                    
                    
                case '\r': 
                    if(currIndex < length-1 && cs[currIndex+1] == '\n'){
                        currIndex++;
                    }
                    /*FALLTHROUGH**/
                case '\n': 
                    currIndex++;
                    handleNewLine();
                    
                    break;
                    
                //No default
                    
            }
            lineBuffer.append(c);
            lastChar = c;
        }
        
        while(stack.size() > 0){
            endScope();
        }
    }
    
    
    /**
     * Called when a new line is found. Tries to make the match of function and class definitions.
     */
    private void handleNewLine() {
        if(currIndex >= length-1){
            return;
        }
        
        col = 1;
        row ++;
        if(DEBUG){
            System.out.println("Handling new line:"+row);
        }
        
        lineBuffer.clear();
        char c = cs[currIndex];
        
        while(currIndex < length-1 && Character.isWhitespace(c) && c != '\r' && c != '\n'){
            currIndex ++;
            col++;
            c = cs[currIndex];
        }
        
        
        if (c == 'c' && matchClass()){
            int startClassCol = col;
            currIndex += 6;
            col += 6;
            
            startClass(getNextIdentifier(c), row, startClassCol);
            
        }else if (c == 'd' && matchFunction()){
            int startMethodCol = col;
            currIndex += 4;
            col += 4;
            
            startMethod(getNextIdentifier(c), row, startMethodCol);
        }
        currIndex --;
    }


    /**
     * Get the next identifier available.
     * @param c the current char
     * @return the identifier found
     */
    private String getNextIdentifier(char c) {
        c = this.cs[currIndex];
        
        while(currIndex < length && Character.isWhitespace(c)){
            currIndex ++;
            c = this.cs[currIndex];
        }
        
        int currClassNameCol = currIndex;
        while(Character.isJavaIdentifierPart(c)){
            currIndex++;
            if(currIndex >= length){
                break;
            }
            c = this.cs[currIndex];
        }
        return new String(this.cs, currClassNameCol, currIndex-currClassNameCol);
    }

    

    
    
    /**
     * Start a new method scope with the given row and column.
     * @param startMethodRow the row where the scope should start
     * @param startMethodCol the column where the scope should start
     */
    private void startMethod(String name, int startMethodRow, int startMethodCol) {
        NameTok nameTok = new NameTok(name, NameTok.ClassName);
        FunctionDef functionDef = new FunctionDef(nameTok, null, null, null);
        functionDef.beginLine = startMethodRow;
        functionDef.beginColumn = startMethodCol;

        addToPertinentScope(functionDef);
    }

    
    
    /**
     * Start a new class scope with the given row and column.
     * @param startClassRow the row where the scope should start
     * @param startClassCol the column where the scope should start
     */
    private void startClass(String name, int startClassRow, int startClassCol) {
        NameTok nameTok = new NameTok(name, NameTok.ClassName);
        ClassDef classDef = new ClassDef(nameTok, null, null);
        
        classDef.beginLine = startClassRow;
        classDef.beginColumn = startClassCol;
        
        stack.push(classDef);
        stackBody.push(new ArrayList<stmtType>());
    }
    
    
    /**
     * Finish the current scope in the stack.
     * 
     * May close many scopes in a single call depending on where the class should be added to.
     */
    private void endScope(){
        ClassDef def = stack.pop();
        List<stmtType> body = stackBody.pop();
        def.body = body.toArray(new stmtType[body.size()]);
        addToPertinentScope(def);
    }


    /**
     * This is the definition to be added to a given scope.
     * 
     * It'll find a correct scope based on the column it has to be added to.
     * 
     * @param def the definition to be added
     */
    private void addToPertinentScope(stmtType def) {
        //see where it should be added (global or class scope)
        while(stack.size() > 0){
            ClassDef parent = stack.peek();
            if(parent.beginColumn < def.beginColumn){
                List<stmtType> peek = stackBody.peek();
                
                if(def instanceof FunctionDef){
                    int size = peek.size();
                    if(size > 0){
                        stmtType existing = peek.get(size-1);
                        if(existing.beginColumn < def.beginColumn){
                            //we don't want to add a method inside a method at this point.
                            //all the items added should have the same column.
                            return;
                        }
                    }
                }
                peek.add(def);
                return;
            }else{
                endScope();
            }
        }
        //if it still hasn't returned, add it to the global
        this.body.add(def);
    }
    
    
    
    /**
     * @return true if we have a match for 'class' in the current index (the 'c' must be already matched at this point)
     */
    private boolean matchClass(){
        if(currIndex + 5 > this.length){
            return false;
        }
        return (this.cs[currIndex+1] == 'l' && this.cs[currIndex+2] == 'a' && 
                this.cs[currIndex+3] == 's' && this.cs[currIndex+4] == 's' && Character.isWhitespace(this.cs[currIndex+5]));
    }
    
    
    /**
     * @return true if we have a match for 'def' in the current index (the 'd' must be already matched at this point)
     */
    private boolean matchFunction(){
        if(currIndex + 3 > this.length){
            return false;
        }
        return (this.cs[currIndex+1] == 'e' && this.cs[currIndex+2] == 'f' && Character.isWhitespace(this.cs[currIndex+3]));
    }

    
    /**
     * Convenience method for parse(s.toCharArray())
     * @param s the string to be parsed
     * @return a Module node with the structure found
     */
    public static SimpleNode parse(String s) {
        return parse(s.toCharArray());
    }
    
    
    /**
     * This method will parse the char array passed and will build a structure with the contents of the file.
     * @param cs the char array to be parsed
     * @return a Module node with the structure found
     */
    public static SimpleNode parse(char[] cs) {
        FastDefinitionsParser parser = new FastDefinitionsParser(cs);
        parser.extractBody();
        List<stmtType> body = parser.body;
        return new Module(body.toArray(new stmtType[body.size()]));
    }

}
