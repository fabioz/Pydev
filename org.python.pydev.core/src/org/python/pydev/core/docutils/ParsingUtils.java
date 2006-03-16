/*
 * Created on 13/07/2005
 */
package org.python.pydev.core.docutils;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;



public class ParsingUtils {
    
    /**
     * @param cs the char array we are parsing
     * @param buf used to add the comments contents (out)
     * @param i the position
     * @return the : position
     */
    public static int eatToColon(char[] cs, StringBuffer buf, int i) {
        while(i < cs.length && cs[i] != ':'){
            buf.append(cs[i]);
            i++;
        }
        if(i < cs.length)
            buf.append(cs[i]);
        
        return i;
    }

    /**
     * @param cs the char array we are parsing
     * @param buf used to add the comments contents (out)
     * @param i the # position
     * @return the end of the comments position (end of document or new line char)
     */
    public static int eatComments(Object cs, StringBuffer buf, int i) {
        while(i < len(cs) && charAt(cs,i) != '\n' && charAt(cs,i) != '\r'){
            buf.append(charAt(cs,i));
            i++;
        }
        if(i < len(cs))
            buf.append(charAt(cs,i));
    
        return i;
    }
    
    /**
     * @param cs the char array we are parsing
     * @param buf used to add the comments contents (out)
     * @param i the # position
     * @return the end of the comments position (end of document or new line char)
     */
    public static int eatComments(char[] cs, int i) {
        while(i < cs.length && cs[i] != '\n' && cs[i] != '\r'){
            i++;
        }
        
        return i;
    }
    
    /**
     * @param cs the char array we are parsing
     * @param buf used to add the token contents (out)
     * @param i the start of the token
     * @return the end of the token position (end of document or new line char or whitespace)
     */
    public static int eatToken(char[] cs, StringBuffer buf, int i) {
        while(i < cs.length && !Character.isWhitespace(cs[i])){
            buf.append(cs[i]);
            i++;
        }
        if(i < cs.length)
            buf.append(cs[i]);
        
        return i;
    }

    /**
     * @param cs the char array we are parsing
     * @param buf used to add the literal contents (out)
     * @param i the ' or " position
     * @return the end of the literal position (or end of document)
     */
    public static int eatLiterals(Object cs, StringBuffer buf, int i) {
        //ok, current pos is ' or "
        //check if we're starting a single or multiline comment...
        char curr = charAt(cs, i);
        
        if(curr != '"' && curr != '\''){
            throw new RuntimeException("Wrong location to eat literals. Expecting ' or \" ");
        }
        
        int j = getLiteralEnd(cs, i, curr);
        
        if(buf != null){
            for (int k = i; k < len(cs) && k <= j; k++) {
                buf.append(charAt(cs, k));
            }
        }
        return j;
        
    }

    /**
     * @param cs object whith len and charAt
     * @param i index we are analyzing it
     * @param curr current char
     * @return the end of the multiline literal
     */
    private static int getLiteralEnd(Object cs, int i, char curr) {
        boolean multi = isMultiLiteral(cs, i, curr);
        
        int j;
        if(multi){
            j = findNextMulti(cs, i+3, curr);
        }else{
            j = findNextSingle(cs, i+1, curr);
        }
        return j;
    }

    /**
     * @param cs the char array we are parsing
     * @param buf used to add the comments contents (out)
     * @param i the ' or " position
     * @return the end of the literal position (or end of document)
     */
    public static int eatPar(Object cs, int i, StringBuffer buf) {
        char c = ' ';
        StringBuffer locBuf = new StringBuffer();
        
        int j = i+1;
        while(j < len(cs) && (c = charAt(cs,j)) != ')'){
            
            j++;
            
            if(c == '\'' || c == '"'){ //ignore comments or multiline comments...
                j = ParsingUtils.eatLiterals( cs, locBuf, j-1)+1;
                
            }else if(c == '#'){
                j = ParsingUtils.eatComments(cs, locBuf, j-1)+1;
                
            }else if( c == '('){ //open another par.
                j = eatPar(cs, j-1, locBuf)+1;
            
            }else{

                locBuf.append(c);
            }
        }
        return j;
    }

    
    /**
     * discover the position of the closing quote
     */
    public static int findNextSingle(Object cs, int i, char curr) {
    	boolean ignoreNext = false;
        while(i < len(cs)){
        	char c = charAt(cs,i);
        	
        	
			if(!ignoreNext && c == curr){
        		break;
        	}

			ignoreNext = false;
			if(c == '\\'){ //escaped quote, ignore the next char even if it is a ' or "
				ignoreNext = true;
			}
            
			i++;
        }
        return i;
    }

    /**
     * check the end of the multiline quote
     */
    public static int findNextMulti(Object cs, int i, char curr) {
        while(i+2 < len(cs)){
            char c = charAt(cs,i);
			if (c == curr && charAt(cs,i+1) == curr && charAt(cs,i+2) == curr){
                break;
            }
			i++;
			if(c == '\\'){ //this is for escaped quotes
				i++;
			}
        }
        if(len(cs) < i+2){
            return len(cs);
        }
        return i+2;
    }
    
    public static char charAt(Object o, int i){
        if (o instanceof char[]) {
            return ((char[]) o)[i];
        }
        if (o instanceof StringBuffer) {
            return ((StringBuffer) o).charAt(i);
        }
        if (o instanceof String) {
            return ((String) o).charAt(i);
        }
        if (o instanceof IDocument) {
            try {
                return ((IDocument) o).getChar(i);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("unable to get char at of "+o.getClass());
    }
    
    public static int len(Object o){
        if (o instanceof char[]) {
            return ((char[]) o).length;
        }
        if (o instanceof StringBuffer) {
            return ((StringBuffer) o).length();
        }
        if (o instanceof String) {
            return ((String) o).length();
        }
        if (o instanceof IDocument) {
            return ((IDocument) o).getLength();
        }
        throw new RuntimeException("unable to get len of "+o.getClass());
    }
    
    /**
     * 
     * @param cs may be a string, a string buffer or a char array
     * @param i current position (should have a ' or ")
     * @param curr the current char (' or ")
     * @return whether we are at the start of a multi line literal or not.
     */
    public static boolean isMultiLiteral(Object cs, int i, char curr){
        if(len(cs) <= i + 2){
            return false;
        }
        if(charAt(cs, i+1) == curr && charAt(cs,i+2) == curr){
            return true;
        }
        return false;
    }

    public static int eatWhitespaces(char cs[], int i) {
        while(i < cs.length && Character.isWhitespace(cs[i])){
            i++;
        }
        return i;
    }

    public static int eatWhitespaces(StringBuffer buf, int i) {
        while(i < buf.length() && Character.isWhitespace(buf.charAt(i))){
            i++;
        }
        return i;
    }

    /**
     * Removes all the comments, whitespaces and literals from a stringbuffer (might be useful when
     * just finding matches for something).
     * 
     * @param buf the buffer from where things should be removed.
     */
    public static void removeCommentsWhitespacesAndLiterals(StringBuffer buf) {
        for (int i = 0; i < buf.length(); i++) {
            char ch = buf.charAt(i);
            if(ch == '#'){
                
                int j = i;
                while(j < buf.length() && ch != '\n' && ch != '\r'){
                    j++;
                    ch = buf.charAt(j);
                }
                buf.delete(i, j);
            }
            
            if(ch == '\'' || ch == '"'){
                int j = getLiteralEnd(buf, i, ch);
                buf.delete(i, j+1);
            }
        }
        
        int length = buf.length();
        for (int i = length -1; i >= 0; i--) {
            char ch = buf.charAt(i);
            if(Character.isWhitespace(ch)){
                buf.deleteCharAt(i);
            }
        }
    }
    
    public static void removeCommentsAndWhitespaces(StringBuffer buf) {
        
        for (int i = 0; i < buf.length(); i++) {
            char ch = buf.charAt(i);
            if(ch == '#'){
            
                int j = i;
                while(j < buf.length() -1 && ch != '\n' && ch != '\r'){
                    j++;
                    ch = buf.charAt(j);
                }
                buf.delete(i, j);
            }
        }
        
        int length = buf.length();
        for (int i = length -1; i >= 0; i--) {
            char ch = buf.charAt(i);
            if(Character.isWhitespace(ch)){
                buf.deleteCharAt(i);
            }
        }
    }

    public static void removeToClosingPar(StringBuffer buf) {
        int length = buf.length();
        for (int i = length -1; i >= 0; i--) {
            char ch = buf.charAt(i);
            if(ch != ')'){
                buf.deleteCharAt(i);
            }else{
                buf.deleteCharAt(i);
                return;
                
            }
        }
    }

    public final static String PY_COMMENT           = "__python_comment";
    public final static String PY_SINGLELINE_STRING = "__python_singleline_string";
    public final static String PY_MULTILINE_STRING  = "__python_multiline_string";
    public final static String PY_BACKQUOTES        = "__python_backquotes";
    public final static String PY_DEFAULT           = "__dftl_partition_content_type";

    /**
     * @param initial
     * @param currPos
     * @return the content type of the 
     */
    public static String getContentType(String initial, int currPos) {
        StringBuffer buf = new StringBuffer(initial);
        String curr = PY_DEFAULT;
        
        for (int i = 0; i < buf.length() && i < currPos; i++) {
            char ch = buf.charAt(i);
            curr = PY_DEFAULT;
            
            if(ch == '#'){
                curr = PY_COMMENT;
                
                int j = i;
                while(j < buf.length()-1 && ch != '\n' && ch != '\r'){
                    j++;
                    ch = buf.charAt(j);
                }
                i = j;
            }
            if(i >= currPos){
                return curr;
            }
            
            if(ch == '\'' || ch == '"'){
                curr = PY_SINGLELINE_STRING;
                i = getLiteralEnd(buf, i, ch);
            }
        }
        return curr;
    }

}
