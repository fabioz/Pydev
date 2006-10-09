/*
 * Created on Oct 8, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import org.python.pydev.outline.ParsedItem;

public class PythonNode implements Comparable, IChildResource {

    public Object parent;
    public ParsedItem entry;
	public PythonFile pythonFile;

    public PythonNode(PythonFile pythonFile, Object parent, ParsedItem e) {
        this.parent = parent;
        this.entry = e;
        this.pythonFile = pythonFile;
    }
    
    @Override
    public String toString() {
        return entry.toString();
    }

    public int compareTo(Object o) {
        if(!(o instanceof PythonNode)){
            return 0;
        }
        return entry.compareTo(((PythonNode)o).entry);
    }

	public Object getParent() {
		return parent;
	}

	public Object getActualObject() {
		return entry;
	}

	public PythonSourceFolder getSourceFolder() {
		return pythonFile.getSourceFolder();
	}

}
