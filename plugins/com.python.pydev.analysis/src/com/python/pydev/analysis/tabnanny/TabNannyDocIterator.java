/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.tabnanny;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;


/**
 * Class to help iterating through the document's indentation strings.
 * 
 * It will yield Tuples with Strings (whitespaces/tabs), starting offset, boolean (true if line has more contents than the spaces/tabs)
 * 
 * the indentations within literals, [, (, {, after \ are not considered 
 * (only the ones actually considered indentations are yielded through).
 */
public class TabNannyDocIterator{
    
    private int offset;
    private IDocument doc;
    private Tuple3<String, Integer, Boolean> nextString;
    private int docLen;
    private boolean firstPass = true;
    
    public TabNannyDocIterator(IDocument doc) throws BadLocationException{
        this.doc = doc;
        docLen = doc.getLength();
        buildNext();
    }

    public boolean hasNext() {
        return nextString != null;
    }

    public Tuple3<String, Integer, Boolean> next() throws BadLocationException {
        if(!hasNext()){
            throw new RuntimeException("Cannot iterate anymore.");
        }
        
        Tuple3<String, Integer, Boolean> ret = nextString;
        buildNext();
        return ret;
    }
    
    private void buildNext() throws BadLocationException {
        while(!internalBuildNext()){
            //just keep doing it... -- lot's of nothing ;-)
        }
    }

    private boolean internalBuildNext() throws BadLocationException {
        try {
            //System.out.println("buildNext");
            char c = '\0';

            ParsingUtils parsingUtils = ParsingUtils.create(doc);
            int initial = -1; 
            while(true){
                
                //safeguard... it must walk a bit every time...
                if(initial == -1){
                    initial = offset;
                }else{
                    if(initial == offset){
                        Log.log("Error: TabNannyDocIterator didn't walk.\n" +
                                "Curr char:"+c+"\n" +
                                "Curr char (as int):"+(int)c+"\n" +
                                "Offset:"+offset+"\n" +
                                "DocLen:"+docLen+"\n"
                                );
                        offset++;
                        return true;
                    }else{
                        initial = offset;
                    }
                }
                
                //keep in this loop until we finish the document or until we're able to find some indent string...
                if(offset >= docLen){
                    nextString = null;
                    return true;
                }
                c = doc.getChar(offset);
                
                
                if (firstPass){
                    //that could happen if we have comments in the 1st line...
                    if((c == ' ' || c == '\t')){
                        break;
                    }else{
                        firstPass = false;
                    }
                    
                }
                
                if (c == '#'){ 
                    //comment (doesn't consider the escape char)
                    offset = parsingUtils.eatComments(null, offset);
                    
                } else if (c == '{' || c == '[' || c == '(') {
                    //starting some call, dict, list, tuple... we're at the same indentation until it is finished
                    offset = parsingUtils.eatPar(offset, null, c);
    
                    
                } else if (c == '\r'){
                    //line end (time for a break to see if we have some indentation just after it...)
                    if(!continueAfterIncreaseOffset()){return true;}
                    c = doc.getChar(offset);
                    if(c == '\n'){
                        if(!continueAfterIncreaseOffset()){return true;}
                    }
                    break;
                    
                    
                } else if (c == '\n'){
                    //line end (time for a break to see if we have some indentation just after it...)
                    if(!continueAfterIncreaseOffset()){return  true;}
                    break;
                    
                } else if (c == '\\') {
                    //escape char found... if it's the last in the line, we don't have a break (we're still in the same line)
                    boolean lastLineChar = false;
                    
                    if(!continueAfterIncreaseOffset()){return true;}
                    
                    c = doc.getChar(offset);
                    if(c == '\r'){
                        if(!continueAfterIncreaseOffset()){return true;}
                        c = doc.getChar(offset);
                        lastLineChar = true;
                    }
                    
                    if(c == '\n'){
                        if(!continueAfterIncreaseOffset()){return true;}
                        lastLineChar = true;
                    }
                    if(!lastLineChar){
                        break;
                    }
                    
                } else if (c == '\'' || c == '\"') {
                    //literal found... skip to the end of the literal
                    offset = parsingUtils.getLiteralEnd(offset, c) + 1;
                    
                } else {
                    // ok, a char is found... go to the end of the line and gather
                    // the spaces to return
                    if(!continueAfterIncreaseOffset()){return true;}
                }

            }
            
            if(offset < docLen){
                c = doc.getChar(offset);
            }else{
                nextString = null;
                return true;
            }
            
            //ok, if we got here, we're in a position to get the indentation string as spaces and tabs...
            FastStringBuffer buf = new FastStringBuffer();
            int startingOffset = offset;
            while (c == ' ' || c == '\t') {
                buf.append(c);
                offset++;
                if(offset >= docLen){
                    break;
                }
                c = doc.getChar(offset);
            }
            //true if we are in a line that has more contents than only the whitespaces/tabs
            nextString = new Tuple3<String, Integer, Boolean>(buf.toString(), startingOffset, c != '\r' && c != '\n');
            
            //now, if we didn't have any indentation, try to make another build
            if(nextString.o1.length() == 0){
                return false;
            }
            
            
        } catch (BadLocationException e) {
            throw e;
            
        }catch(SyntaxErrorException e){
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * Increase the offset and see whether we should continue iterating in the document after that...
     * @return true if we should continue iterating and false otherwise.
     */
    private boolean continueAfterIncreaseOffset() {
        offset++;
        boolean ret = true;
        if(offset >= docLen){
            nextString = null;
            ret = false;
        }
        return ret;
    }
    
    public void remove() {
        throw new RuntimeException("Not implemented");
    }
}
