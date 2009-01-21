/*
 * Created on 12/06/2005
 */
package org.python.pydev.core.docutils;

import java.util.StringTokenizer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.cache.Cache;
import org.python.pydev.core.cache.LRUCache;

/**
 * This class contains commonly used text- or document-related functions,
 * variables, etc. used throughout Pydev source.
 * 
 * This class is meant as a centralized location for them. It resulted as
 * extracting a variety of generic strings, characters, and functions from the
 * Pydev source.
 * 
 * @author Fabio
 */
public class DocUtils {
    
    /**
     * Field representing an empty string.
     */
    public static final String EMPTY_STRING = "";

    /**
     * @param document
     * @param i
     * @return
     */
    public static String getDocToParseFromLine(IDocument doc, int lineOfOffset) {
        String wholeDoc = doc.get();
        String newDoc = "";
        try {
            IRegion lineInformation = doc.getLineInformation(lineOfOffset);

            int docLength = doc.getLength();

            String before = wholeDoc.substring(0, lineInformation.getOffset());
            String after = wholeDoc.substring(lineInformation.getOffset()
                    + lineInformation.getLength(), docLength);
            
            String src = doc.get(lineInformation.getOffset(), lineInformation.getLength());

            String spaces = "";
            for (int i = 0; i < src.length(); i++) {
                if (src.charAt(i) == ' ') {
                    spaces += ' ';
                    
                } else if (src.charAt(i) == '\t') {
                    spaces += '\t';
                    
                }else {
                    break;
                }
            }


            src = src.trim();
            if (src.startsWith("class")){
                //let's discover if we should put a pass or not...
                //e.g if we are declaring the class and no methods are put, we have
                //to put a pass, otherwise, the pass would ruin the indentation, therefore,
                //we cannot put it.
                //
                //so, search for another class or def after this line and discover if it has another indentation 
                //or not.
                
                StringTokenizer tokenizer = new StringTokenizer(after, "\r\n");
                String tokSpaces = null;
                
                while(tokenizer.hasMoreTokens()){
                    String tok = tokenizer.nextToken();
                    String t = tok.trim();
                    if(t.startsWith("class") || t.startsWith("def") ){
                        tokSpaces = "";
                        for (int i = 0; i < tok.length(); i++) {
                            if (tok.charAt(i) == ' ') {
                                tokSpaces += ' ';
                                
                            } else if (tok.charAt(i) == '\t') {
                                tokSpaces += '\t';
                                
                            } else{
                                break;
                            }
                        }
                        break;
                    }
                }
                
                if(tokSpaces != null && tokSpaces.length() > spaces.length()){
                    if(src.indexOf('(') != -1){
                        src = src.substring(0, src.indexOf('('))+Character.toString(':');
                    }else{
                        src = "class COMPLETION_HELPER_CLASS:";
                    }
                }else{
                    if(src.indexOf('(') != -1){
                        src = src.substring(0, src.indexOf('('))+":pass";
                    }else{
                        src = "class COMPLETION_HELPER_CLASS:pass";
                    }
                }
                
                
            }else{
                src = "pass";
            }
            
            newDoc = before;
            newDoc += spaces + src;
            newDoc += after;

        } catch (BadLocationException e1) {
            //that's ok...
            //e1.printStackTrace();
            //System.out.println(lineOfOffset);
            return null;
        }
        return newDoc;
    }
    

    /**
     * Small cache to hold strings only with spaces (so that each width has a created string).
     */
    private static Cache<Integer, String> widthToSpaceString = new LRUCache<Integer, String>(8);
    
    /**
     * Creates a string of spaces of the designated length.
     * @param width number of spaces you want to create a string of
     * @return the created string
     */
    public static String createSpaceString(int width) {
        String existing = widthToSpaceString.getObj(width);
        if(existing != null){
            return existing;
        }
        StringBuffer b = new StringBuffer(width);
        while (width-- > 0){
            b.append(' ');
        }
        String newStr = b.toString();
        widthToSpaceString.add(width, newStr);
        return newStr;
    }
    
    /**
     * An array of Python pairs of characters that you will find in any Python code.
     * 
     * Currently, the set contains:
     * <ul>
     * <ol>left and right brackets: [, ]</ol>
     * <ol>right and right parentheses: (, )
     * </ul>
     */
    public static final char[] BRACKETS = { '{', '}', '(', ')', '[', ']' };

    public static char getPeer(char c){
        switch(c){
            case '{':return '}';
            case '}':return '{';
            case '(':return ')';
            case ')':return '(';
            case '[':return ']';
            case ']':return '[';
        }
        
        throw new NoPeerAvailableException("Unable to find peer for :"+c);
        
    }

    public static boolean isClosingPeer(char lastChar) {
        return lastChar == '}' || lastChar == ')' || lastChar == ']';
    }


    public static boolean hasOpeningBracket(String trimmedLine) {
        return trimmedLine.indexOf('{') != -1 || trimmedLine.indexOf('(') != -1 || trimmedLine.indexOf('[') != -1;
    }
    
    public static boolean hasClosingBracket(String trimmedLine) {
        return trimmedLine.indexOf('}') != -1 || trimmedLine.indexOf(')') != -1 || trimmedLine.indexOf(']') != -1;
    }
    
    /**
     * Tests whether each character in the given
     * string is a letter.
     *
     * @param str
     * @return <code>true</code> if the given string is a word
     */
    public static boolean isWord(String str) {
        if (str == null || str.length() == 0)
            return false;

        for (int i= 0; i < str.length(); i++) {
            if (!Character.isJavaIdentifierPart(str.charAt(i)))
                return false;
        }
        return true;
    }

}
