/*
 * Created on 13/07/2005
 */
package org.python.pydev.core.docutils;

import java.util.Iterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitionerExtension2;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.structure.FastStringBuffer;


/**
 * Helper class for parsing python code.
 *
 * @author Fabio
 */
public abstract class ParsingUtils implements IPythonPartitions{
    

    /**
     * Class that handles char[]
     *
     * @author Fabio
     */
    private static class CharArrayParsingUtils extends ParsingUtils{
        private char[] cs;
        public CharArrayParsingUtils(char[] cs) {
            this.cs = cs;
        }
        public int len() {
            return cs.length;
        }
        public char charAt(int i) {
            return cs[i];
        }
    }
    
    
    /**
     * Class that handles FastStringBuffer
     *
     * @author Fabio
     */
    private static class FastStringBufferParsingUtils extends ParsingUtils{
        private FastStringBuffer cs;
        public FastStringBufferParsingUtils(FastStringBuffer cs) {
            this.cs = cs;
        }
        public int len() {
            return cs.length();
        }
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }
    
    /**
     * Class that handles StringBuffer
     *
     * @author Fabio
     */
    private static class StringBufferParsingUtils extends ParsingUtils{
        private StringBuffer cs;
        public StringBufferParsingUtils(StringBuffer cs) {
            this.cs = cs;
        }
        public int len() {
            return cs.length();
        }
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }
    
    /**
     * Class that handles String
     *
     * @author Fabio
     */
    private static class StringParsingUtils extends ParsingUtils{
        private String cs;
        public StringParsingUtils(String cs) {
            this.cs = cs;
        }
        public int len() {
            return cs.length();
        }
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }
    
    /**
     * Class that handles String
     *
     * @author Fabio
     */
    private static class IDocumentParsingUtils extends ParsingUtils{
        private IDocument cs;
        public IDocumentParsingUtils(IDocument cs) {
            this.cs = cs;
        }
        public int len() {
            return cs.getLength();
        }
        public char charAt(int i) {
            try {
                return cs.getChar(i);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * Factory method to create it.
     */
    public static ParsingUtils create(Object cs) {
        if(cs instanceof char[]){
            return new CharArrayParsingUtils((char[])cs);
        }
        if(cs instanceof FastStringBuffer){
            return new FastStringBufferParsingUtils((FastStringBuffer)cs);
        }
        if(cs instanceof StringBuffer){
            return new StringBufferParsingUtils((StringBuffer)cs);
        }
        if(cs instanceof String){
            return new StringParsingUtils((String)cs);
        }
        if(cs instanceof IDocument){
            return new IDocumentParsingUtils((IDocument)cs);
        }
        throw new RuntimeException("Don't know how to create instance for: "+cs.getClass());
    }
    
    
    //Abstract interfaces -------------------------------------------------------------

    
    /**
     * @return the char at a given position of the object
     */
    public abstract char charAt(int i);
    
    
    /**
     * @return the length of the contained object
     */
    public abstract int len();
    
    
    //API methods --------------------------------------------------------------------

    
    /**
     * @param cs the char array we are parsing
     * @param buf used to add the comments contents (out) -- if it's null, it'll simply advance to the position and 
     * return it.
     * @param i the # position
     * @return the end of the comments position (end of document or new line char) 
     * @note the new line char (\r or \n) will be added as a part of the comment.
     */
    public int eatComments(FastStringBuffer buf, int i) {
        int len = len();
        char c;
        
        while(i < len && (c = charAt(i)) != '\n' && c != '\r'){
            if(buf != null){
                buf.append(c);
            }
            i++;
        }
        
        if(i < len){
            if(buf != null){
                buf.append(charAt(i));
            }
        }
    
        return i;
    }
    


    
    /**
     * @param cs the char array we are parsing
     * @param buf used to add the spaces (out) -- if it's null, it'll simply advance to the position and 
     * return it.
     * @param i the first ' ' position
     * @return the position of the last space found
     */
    public int eatWhitespaces(FastStringBuffer buf, int i) {
        int len = len();
        char c;
        
        while(i < len && (c = charAt(i)) == ' '){
            if(buf != null){
                buf.append(c);
            }
            i++;
        }
        
        //go back to the last space found
        i--;
        
        return i;
    }
    
    
    

    
    
    /**
     * @param cs the char array we are parsing
     * @param buf used to add the literal contents (out)
     * @param i the ' or " position
     * @return the end of the literal position (or end of document) -- so, the final char is the ' or " position
     */
    public int eatLiterals(FastStringBuffer buf, int i) {
        //ok, current pos is ' or "
        //check if we're starting a single or multiline comment...
        char curr = charAt(i);
        
        if(curr != '"' && curr != '\''){
            throw new RuntimeException("Wrong location to eat literals. Expecting ' or \" ");
        }
        
        int j = getLiteralEnd(i, curr);
        
        if(buf != null){
            int len = len();
            for (int k = i; k < len && k <= j; k++) {
                buf.append(charAt(k));
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
    public int getLiteralEnd(int i, char curr) {
        boolean multi = isMultiLiteral(i, curr);
        
        int j;
        if(multi){
            j = findNextMulti(i+3, curr);
        }else{
            j = findNextSingle(i+1, curr);
        }
        return j;
    }

    /**
     * @param cs the char array we are parsing
     * @param buf used to add the comments contents (out)
     * @param i the ' or " position
     * @return the end of the literal position (or end of document)
     */
    public int eatPar(int i, FastStringBuffer buf) {
        return eatPar(i, buf, '(');
    }
    
    
    /**
     * @param i the index where we should start getting chars
     * @param buf the buffer that should be filled with the contents gotten (if null, they're ignored)
     * @return the index where the parsing stopped
     */
    public int getFullFlattenedLine(int i, FastStringBuffer buf) {
        char c = this.charAt(i);
        int len = len();
        boolean ignoreNextNewLine = false;
        while(i < len){
            c = charAt(i);
            
            i++;
            
            if(c == '\'' || c == '"'){ //ignore comments or multiline comments...
                i = eatLiterals(null, i-1)+1;
                
            }else if(c == '#'){
                i = eatComments(null, i-1)+1;
                break;
                
            }else if( c == '(' || c == '[' || c == '{'){ //open par.
                i = eatPar(i-1, null, c)+1;
            
            }else if( c == '\r' || c == '\n' ){
                if(!ignoreNextNewLine){
                    break;
                }
                
            }else if( c == '\\' || c == '\\' ){
                ignoreNextNewLine = true;
                continue;
                
            }else{
                if(buf != null){
                    buf.append(c);
                }
            }
            
            ignoreNextNewLine = false;
        }
        return i;
    }


    /**
     * @param buf if null, it'll simply advance without adding anything to the buffer.
     */
    public int eatPar(int i, FastStringBuffer buf, char par) {
        char c = ' ';
        
        char closingPar = DocUtils.getPeer(par);
        
        int j = i+1;
        int len = len();
        while(j < len && (c = charAt(j)) != closingPar){
            
            j++;
            
            if(c == '\'' || c == '"'){ //ignore comments or multiline comments...
                j = eatLiterals(null, j-1)+1;
                
            }else if(c == '#'){
                j = eatComments(null, j-1)+1;
                
            }else if( c == par){ //open another par.
                j = eatPar(j-1, null, par)+1;
            
            }else{
                if(buf != null){
                    buf.append(c);
                }
            }
        }
        return j;
    }

    
    /**
     * discover the position of the closing quote
     */
    public int findNextSingle(int i, char curr) {
    	boolean ignoreNext = false;
        int len = len();
        while(i < len){
        	char c = charAt(i);
        	
        	
			if(!ignoreNext && c == curr){
        		break;
        	}

            if(!ignoreNext){
    			if(c == '\\'){ //escaped quote, ignore the next char even if it is a ' or "
    				ignoreNext = true;
    			}
            }else{
                ignoreNext = false;
            }
            
			i++;
        }
        return i;
    }

    /**
     * check the end of the multiline quote
     */
    public int findNextMulti(int i, char curr) {
        int len = len();
        while(i+2 < len){
            char c = charAt(i);
			if (c == curr && charAt(i+1) == curr && charAt(i+2) == curr){
                break;
            }
			i++;
			if(c == '\\'){ //this is for escaped quotes
				i++;
			}
        }
        if(len < i+2){
            return len;
        }
        return i+2;
    }
    

    
    
    
    
    
    
    
    
    
    
    
    //STATIC INTERFACES FROM NOW ON ----------------------------------------------------------------
    //STATIC INTERFACES FROM NOW ON ----------------------------------------------------------------
    //STATIC INTERFACES FROM NOW ON ----------------------------------------------------------------
    //STATIC INTERFACES FROM NOW ON ----------------------------------------------------------------
    //STATIC INTERFACES FROM NOW ON ----------------------------------------------------------------

    
    
    
    
    
    /**
     * 
     * @param cs may be a string, a string buffer or a char array
     * @param i current position (should have a ' or ")
     * @param curr the current char (' or ")
     * @return whether we are at the start of a multi line literal or not.
     */
    public boolean isMultiLiteral(int i, char curr){
        int len = len();
        if(len <= i + 2){
            return false;
        }
        if(charAt(i+1) == curr && charAt(i+2) == curr){
            return true;
        }
        return false;
    }


    public static void removeCommentsWhitespacesAndLiterals(FastStringBuffer buf) {
        removeCommentsWhitespacesAndLiterals(buf, true);
    }
    
    /**
     * Removes all the comments, whitespaces and literals from a FastStringBuffer (might be useful when
     * just finding matches for something).
     * 
     * NOTE: the literals and the comments are changed for spaces (if we don't remove them too)
     * 
     * @param buf the buffer from where things should be removed.
     * @param whitespacesToo: are you sure about the whitespaces?
     */
    public static void removeCommentsWhitespacesAndLiterals(FastStringBuffer buf, boolean whitespacesToo) {
        ParsingUtils parsingUtils = create(buf);
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
                int j = parsingUtils.getLiteralEnd(i, ch);
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
	public static void removeLiterals(FastStringBuffer buf) {
	    ParsingUtils parsingUtils = create(buf);
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
                int j = parsingUtils.getLiteralEnd(i, ch);
                for (int k = 0; i+k < j+1; k++) {
					buf.replace(i+k, i+k+1, " ");
				}
            }
        }
	}
    
	public static Iterator<String> getNoLiteralsOrCommentsIterator(IDocument doc) {
		return new PyDocIterator(doc);
	}

    
    public static void removeCommentsAndWhitespaces(FastStringBuffer buf) {
        
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
        FastStringBuffer buf = new FastStringBuffer(initial, 0);
        ParsingUtils parsingUtils = create(initial);
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
                i = parsingUtils.getLiteralEnd(i, ch);
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
        IDocumentPartitionerExtension2 partitioner = (IDocumentPartitionerExtension2)
        	docExtension.getDocumentPartitioner(IPythonPartitions.PYTHON_PARTITION_TYPE);
        
        if(partitioner != null){
            return partitioner.getContentType(i, true);
        }
        return getContentType(document.get(), i);
    }

    public static String makePythonParseable(String code, String delimiter) {
        return makePythonParseable(code, delimiter, new FastStringBuffer());
    }
    
    /**
     * Ok, this method will get some code and make it suitable for putting at a shell
     * @param code the initial code we'll make parseable
     * @param delimiter the delimiter we should use
     * @return a String that can be passed to the shell
     */
    public static String makePythonParseable(String code, String delimiter, FastStringBuffer lastLine) {
        FastStringBuffer buffer = new FastStringBuffer();
        FastStringBuffer currLine = new FastStringBuffer();
        
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
                    currLine = new FastStringBuffer();
                    buffer.append(delimiter);
                    foundNewLine = true;
                    
                }else{ //found a line only with whitespaces
                    currLine = new FastStringBuffer();
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


    public static String removeComments(String line) {
        int i = line.indexOf('#');
        if(i != -1){
            return line.substring(0, i);
        }
        return line;
    }




}
