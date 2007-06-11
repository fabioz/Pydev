package org.python.pydev.parser.prettyprinter;

public interface IWriterEraser {

	public void write(String o);
	
	public void erase(String o);

    public void pushTempBuffer();

    public String popTempBuffer();
}
