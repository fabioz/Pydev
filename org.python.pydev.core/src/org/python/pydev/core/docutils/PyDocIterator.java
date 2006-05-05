package org.python.pydev.core.docutils;

import java.util.Iterator;

import org.eclipse.jface.text.IDocument;

public class PyDocIterator implements Iterator {

	private int offset;
	private IDocument doc;
	
	public PyDocIterator(IDocument doc) {
		this.doc = doc;
	}

	public boolean hasNext() {
		return offset < doc.getLength();
	}

	/**
	 * 
	 */
	public Object next() {
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
				buf.append(ch);
				if(ch == '\r'){
					if(offset < doc.getLength() && doc.getChar(offset) == '\n'){
						offset++;
						buf.append('\n');
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
