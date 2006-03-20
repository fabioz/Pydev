package org.python.pydev.parser.jython;

public class SpecialStr {
	public String str;
	public int beginLine;
	public int beginCol;

	public SpecialStr(String str, int beginLine, int beginCol){
		this.str = str;
		this.beginLine = beginLine;
		this.beginCol = beginCol;
	}
	
	@Override
	public String toString() {
		return str;
	}

	@Override
	public int hashCode() {
		return str.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof SpecialStr)){
			return false;
		}
		return str.equals(((SpecialStr)obj).str);
	}
}
