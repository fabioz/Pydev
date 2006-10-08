/*
 * Created on Oct 8, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import org.python.pydev.outline.ParsedItem;

public class PythonNode implements Comparable {

    public Object parent;
    public ParsedItem entry;

    public PythonNode(Object parent, ParsedItem e) {
        this.parent = parent;
        this.entry = e;
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

}
