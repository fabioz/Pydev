package org.python.pydev.parser.grammarcommon;

import java.util.ArrayList;
import java.util.Collections;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.comprehensionType;

public final class ComprehensionCollection extends SimpleNode{
    
    public ArrayList<Comprehension> added = new ArrayList<Comprehension>();

    public comprehensionType[] getGenerators() {
        ArrayList<Comprehension> f = added;
        added = null;
        Collections.reverse(f);
        return f.toArray(new comprehensionType[0]);
    }
}