package org.python.pydev.core.docutils;

import java.util.Iterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class PyDocIterator implements Iterator<String> {

	private int offset;
	private IDocument doc;
	private boolean addNewLinesToRet = true;
	
	public PyDocIterator(IDocument doc, boolean addNewLinesToRet) {
		this(doc);
		this.addNewLinesToRet = addNewLinesToRet;
	}
	
	public PyDocIterator(IDocument doc) {
		this.doc = doc;
	}

	public boolean hasNext() {
		return offset < doc.getLength();
	}
	
	public int getLastReturnedLine(){
		try {
			return doc.getLineOfOffset(offset-1);
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 */
	public String next() {
        try {
        	StringBuffer buf = new StringBuffer();

        	char ch = 0;
        	
			while(ch != '\r' && ch != '\n' && offset < doc.getLength()){
				ch = doc.getChar(offset);
				if (ch == '#') {
	
					while (offset < doc.getLength() && ch != '\n' && ch != '\r') {
						ch = doc.getChar(offset);
						offset++;
					}
					
				}else if (ch == '\'' || ch == '"') {
					offset = ParsingUtils.getLiteralEnd(doc, offset, ch);
					offset++;
					
				}else if(ch != '\n' && ch != '\r'){
					//will be added later
					buf.append(ch);
					offset++;
				}else{
					offset++;
				}
			}			
			
			//handle the \r, \n or \r\n
			if(ch == '\n' || ch == '\r'){
				if(addNewLinesToRet){
					buf.append(ch);
				}
				if(ch == '\r'){
					if(offset < doc.getLength() && doc.getChar(offset) == '\n'){
						offset++;
						if(addNewLinesToRet){
							buf.append('\n');
						}
					}
				}
			}
			
			return buf.toString();
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void remove() {
		throw new RuntimeException("Not Impl.");
	}

}
