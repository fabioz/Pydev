/*
 * Created on 12/06/2005
 */
package org.python.pydev.core.docutils;

import java.util.StringTokenizer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

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
     * Field representing a semicolon.
     */
    public static final char SEMICOLON = ';';

    /**
     * Field representing a colon;
     */
    public static final char COLON = ':';

    /**
     * Field representing a comma.
     */
    public static final char COMMA = ',';

    /**
     * Field representing a space.
     */
    public static final char SPACE = ' ';

    /**
     * Field representing a beginning parenthesis, i.e., (
     */
    public static final char BEGIN_PARENTHESIS = '(';

    /**
     * Field representing an ending parenthesis, i.e., )
     */
    public static final char END_PARENTHESIS = ')';
    
    /**
     * Field representing a character tab.
     */
    public static final char TAB = '\t';
    
    /**
     * Field representing a tab as a String.
     */
    public static final String TAB_STRING = "\t";
    
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
                if (src.charAt(i) != ' ') {
                    break;
                }
                spaces += SPACE;
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
                            if (tok.charAt(i) != SPACE) {
                                break;
                            }
                            tokSpaces += SPACE;
                        }
                        break;
                    }
                }
                
                if(tokSpaces != null && tokSpaces.length() > spaces.length()){
	                if(src.indexOf(BEGIN_PARENTHESIS) != -1){
	                    src = src.substring(0, src.indexOf(BEGIN_PARENTHESIS))+Character.toString(COLON);
	                }else{
	                    src = "class COMPLETION_HELPER_CLASS:";
	                }
                }else{
	                if(src.indexOf(BEGIN_PARENTHESIS) != -1){
	                    src = src.substring(0, src.indexOf(BEGIN_PARENTHESIS))+":pass";
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
            return null;
        }
        return newDoc;
    }
    

    /**
     * Creates a string of spaces of the designated length.
     * @param width number of spaces you want to create a string of
     * @return the created string
     */
    public static String createSpaceString(int width) {
        StringBuffer b = new StringBuffer(width);
        while (width-- > 0)
            b.append(SPACE);
        return b.toString();
    }

    public static char getPeer(char c){
        for (int i = 0; i < BRACKETS.length; i++) {
            if(c == BRACKETS[i]){
                int mod = i % 2;
                return BRACKETS[i - mod];
            }
        }
        
        throw new RuntimeException("Unable to find peer for :"+c);
        
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
}
