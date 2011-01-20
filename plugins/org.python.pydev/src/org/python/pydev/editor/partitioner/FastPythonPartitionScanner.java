/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.partitioner;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.core.IPythonPartitions;

/**
 * Based on org.eclipse.jdt.internal.ui.text.FastJavaPartitionScanner
 * 
 * Could become a replacement. Note that it's currently not used anywhere!
 * 
 * @author fabioz
 */
public class FastPythonPartitionScanner implements IPartitionTokenScanner, IPythonPartitions{
    

    // states
    private static final int PYTHON= 0;
    private static final int COMMENT= 1;
    private static final int SINGLE_LINE_STRING1= 2; //'
    private static final int SINGLE_LINE_STRING2= 3; //"
    private static final int MULTI_LINE_STRING1= 4;  //'
    private static final int MULTI_LINE_STRING2= 5;  //""
    private static final int BACKQUOTES= 6;  
    
    
    /** The scanner. */
    private final BufferedDocumentScanner fScanner= new BufferedDocumentScanner(1000);    // faster implementation

    private final IToken[] fTokens= new IToken[] {
            new Token(null),
            new Token(PY_COMMENT),
            new Token(PY_SINGLELINE_STRING1),
            new Token(PY_SINGLELINE_STRING2),
            new Token(PY_MULTILINE_STRING1),
            new Token(PY_MULTILINE_STRING2),
            new Token(PY_BACKQUOTES),
        };
    private int fTokenOffset;
    private int fTokenLength;
    private String currContentType;
    

    public void setPartialRange(IDocument document, int offset, int length, String contentType, int partitionOffset) {
        if(partitionOffset != -1 && partitionOffset < offset){
            fScanner.setRange(document, partitionOffset, length+(offset-partitionOffset));
            fTokenOffset= partitionOffset;
            fTokenLength= 0;
            currContentType = null;
            
        }else{
            fScanner.setRange(document, offset, length);
            fTokenOffset= offset;
            fTokenLength= 0;
            currContentType = contentType;
        }
    }

    public void setRange(IDocument document, int offset, int length) {
        currContentType = null;
        fScanner.setRange(document, offset, length);
        fTokenOffset= offset;
        fTokenLength= 0;
    }


    public int getTokenOffset() {
        return fTokenOffset;
    }

    public int getTokenLength() {
        return fTokenLength;
    }

    /*
     * @see org.eclipse.jface.text.rules.ITokenScanner#nextToken()
     */
    public IToken nextToken() {
        fTokenOffset += fTokenLength;
        fTokenLength= 0;
        
        int ch= fScanner.read();
        if(ch == ICharacterScanner.EOF){
            fTokenLength++;
            return Token.EOF;
        }

        if(currContentType != null){
            if(currContentType.equals(PY_COMMENT)){
                return handleComment(ch);
            }
            if(currContentType.equals(PY_SINGLELINE_STRING1)){
                return handleSingleQuotedString(ch);
                
            }
            if(currContentType.equals(PY_SINGLELINE_STRING2)){
                return handleSingleQuotedString(ch);
                
            }
            if(currContentType.equals(PY_MULTILINE_STRING1)){
                return handleSingleQuotedString(ch);
                
            }
            if(currContentType.equals(PY_MULTILINE_STRING2)){
                return handleSingleQuotedString(ch);
                
            }
            if(currContentType.equals(PY_BACKQUOTES)){
                
            }
            
        }
        

        
        // characters
         switch (ch) {
         case '#':
            return handleComment(ch);
             
         case '"':
         case '\'':
             return handleSingleQuotedString(ch);
             
         default:
             fTokenLength++;
             return fTokens[PYTHON];
         }
    }

    private IToken handleSingleQuotedString(int ch) {
        int initialChar = ch;
        int offsetEnd = fTokenOffset;
        
        if(isMultiLiteral(ch)){
            offsetEnd += 2; //ok, it is a multi-line with single quotes
            return handleMultiSingleQuotedString(ch, offsetEnd, initialChar);
        }
        
        //it is a single-line string
        ch = fScanner.read();
        offsetEnd++;
        
        while(ch!= '\n' && ch != '\r' && ch != initialChar && ch != ICharacterScanner.EOF){
            ch = fScanner.read();
            offsetEnd++;
        }
        
        offsetEnd++;
        fTokenLength = offsetEnd-fTokenOffset;
        if(initialChar == '\''){
            return fTokens[SINGLE_LINE_STRING1];
        }else{
            return fTokens[SINGLE_LINE_STRING2];
        }
    }

    
    private IToken handleMultiSingleQuotedString(int ch, int offsetEnd, int initialChar) {
        //it is a multi-line string
        ch = fScanner.read();
        offsetEnd++;
        
        while(ch != ICharacterScanner.EOF){
            if(ch == initialChar){
                if(isMultiLiteral(ch)){
                    offsetEnd+=2;
                    if(initialChar == '\''){
                        return fTokens[MULTI_LINE_STRING1];
                    }else{
                        return fTokens[MULTI_LINE_STRING2];
                    }
                }
            }
            ch = fScanner.read();
            offsetEnd++;
        }
        
        if(initialChar == '\''){
            return fTokens[SINGLE_LINE_STRING1];
        }else{
            return fTokens[SINGLE_LINE_STRING2];
        }
    }

    private boolean isMultiLiteral(int ch) {
        int c1 = fScanner.read();
        if(c1 == ch){
            int c2 = fScanner.read();
            if(c2 == ch){
                return true;
            }
            if(c2 != ICharacterScanner.EOF){
                fScanner.unread();
            }
        }
        if(c1 != ICharacterScanner.EOF){
            fScanner.unread();
        }
        return false;
    }

    private IToken handleComment(int ch) {
        int offsetEnd = fTokenOffset;
        
        while(ch!= '\n' && ch != '\r' && ch != ICharacterScanner.EOF){
            ch = fScanner.read();
            offsetEnd++;
        }
        fTokenLength = offsetEnd-fTokenOffset;
        return fTokens[COMMENT];
    }

}
