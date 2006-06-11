/*
 * Created on 13/07/2005
 */
package org.python.pydev.core.docutils;

import java.util.Iterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IPythonPartitions;



public class ParsingUtils implements IPythonPartitions{
    
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
    public static int getLiteralEnd(Object cs, int i, char curr) {
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

    public static void removeCommentsWhitespacesAndLiterals(StringBuffer buf) {
        removeCommentsWhitespacesAndLiterals(buf, true);
    }
    /**
     * Removes all the comments, whitespaces and literals from a stringbuffer (might be useful when
     * just finding matches for something).
     * 
     * NOTE: the literals and the comments are changed for spaces (if we don't remove them too)
     * 
     * @param buf the buffer from where things should be removed.
     * @param whitespacesToo: are you sure about the whitespaces?
     */
    public static void removeCommentsWhitespacesAndLiterals(StringBuffer buf, boolean whitespacesToo) {
        for (int i = 0; i < buf.length(); i++) {
            char ch = buf.charAt(i);
            if(ch == '#'){
                
                int j = i;
                while(j < buf.length() && ch != '\n' && ch != '\r'){
                    ch = buf.charAt(j);
                    j++;
                }
                buf.delete(i, j);
            }
            
            if(ch == '\'' || ch == '"'){
                int j = getLiteralEnd(buf, i, ch);
                if(whitespacesToo){
	              	buf.delete(i, j+1);
                }else{
	                for (int k = 0; i+k < j+1; k++) {
						buf.replace(i+k, i+k+1, " ");
					}
                }
            }
        }
        
        if(whitespacesToo){
            int length = buf.length();
            for (int i = length -1; i >= 0; i--) {
                char ch = buf.charAt(i);
                if(Character.isWhitespace(ch)){
                    buf.deleteCharAt(i);
                }
            }
        }
    }
	public static void removeLiterals(StringBuffer buf) {
        for (int i = 0; i < buf.length(); i++) {
            char ch = buf.charAt(i);
            if(ch == '#'){
                //just past through comments
                while(i < buf.length() && ch != '\n' && ch != '\r'){
                    ch = buf.charAt(i);
                    i++;
                }
            }
            
            if(ch == '\'' || ch == '"'){
                int j = getLiteralEnd(buf, i, ch);
                for (int k = 0; i+k < j+1; k++) {
					buf.replace(i+k, i+k+1, " ");
				}
            }
        }
	}
    
	public static Iterator getNoLiteralsOrCommentsIterator(IDocument doc) {
		return new PyDocIterator(doc);
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


    /**
     * @param initial the document
     * @param currPos the offset we're interested in
     * @return the content type of the current position
     * 
     * The version with the IDocument as a parameter should be preffered, as
     * this one can be much slower (still, it is an alternative in tests or
     * other places that do not have document access), but keep in mind
     * that it may be slow.
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
            	curr = PY_SINGLELINE_STRING1;
            	if(ch == '"'){
            		curr = PY_SINGLELINE_STRING2;
            	}
                i = getLiteralEnd(buf, i, ch);
            }
        }
        return curr;
    }

    /**
     * @param document the document we want to get info on
     * @param i the document offset we're interested in
     * @return the content type at that position (according to IPythonPartitions)
     * 
     * Uses the default if the partitioner is not set in the document (for testing purposes)
     */
    public static String getContentType(IDocument document, int i) {
        IDocumentExtension3 docExtension= (IDocumentExtension3) document;
        IDocumentPartitioner partitioner = docExtension.getDocumentPartitioner(IPythonPartitions.PYTHON_PARTITION_TYPE);
        if(partitioner != null){
            return partitioner.getContentType(i);
        }
        return getContentType(document.get(), i);
    }

    public static String makePythonParseable(String code, String delimiter) {
        return makePythonParseable(code, delimiter, new StringBuffer());
    }
    
    /**
     * Ok, this method will get some code and make it suitable for putting at a shell
     * @param code the initial code we'll make parseable
     * @param delimiter the delimiter we should use
     * @return a String that can be passed to the shell
     */
    public static String makePythonParseable(String code, String delimiter, StringBuffer lastLine) {
        StringBuffer buffer = new StringBuffer();
        StringBuffer currLine = new StringBuffer();
        
        //we may have line breaks with \r\n, or only \n or \r
        boolean foundNewLine = false;
        boolean foundNewLineAtChar;
        boolean lastWasNewLine = false;
        
        if(lastLine.length() > 0){
            lastWasNewLine = true;
        }
        
        for (int i = 0; i < code.length(); i++) {
            foundNewLineAtChar = false;
            char c = code.charAt(i);
            if(c == '\r'){
                if(i +1 < code.length() && code.charAt(i+1) == '\n'){
                    i++; //skip the \n
                }
                foundNewLineAtChar = true;
            }else if(c == '\n'){
                foundNewLineAtChar = true;
            }
            
            if(!foundNewLineAtChar){
                if(lastWasNewLine && !Character.isWhitespace(c)){
                    if(lastLine.length() > 0 && Character.isWhitespace(lastLine.charAt(0))){
                        buffer.append(delimiter);
                    }
                }
                currLine.append(c);
                lastWasNewLine = false;
            }else{
                lastWasNewLine = true;
            }
            if(foundNewLineAtChar || i == code.length()-1){
                if(!PySelection.containsOnlyWhitespaces(currLine.toString())){
                    buffer.append(currLine);
                    lastLine = currLine;
                    currLine = new StringBuffer();
                    buffer.append(delimiter);
                    foundNewLine = true;
                    
                }else{ //found a line only with whitespaces
                    currLine = new StringBuffer();
                }
            }
        }
        if(!foundNewLine){
            buffer.append(delimiter);
        }else{
            if(!WordUtils.endsWith(buffer, '\r') && !WordUtils.endsWith(buffer, '\n')){
                buffer.append(delimiter);
            }
            if(lastLine.length() > 0 && Character.isWhitespace(lastLine.charAt(0)) && 
                    (code.indexOf('\r') != -1 || code.indexOf('\n') != -1)){
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }

    public static String getLastLine(String code) {
        int i = code.lastIndexOf('\r');
        int j = code.lastIndexOf('\n');
        if(i == -1 && j == -1){
            return code;
        }
        
        char toSplit = '\n';
        if(i > j){
            toSplit = '\r';
        }
        
        String[] strings = FullRepIterable.split(code, toSplit);
        return strings[strings.length-1];
    }



}
