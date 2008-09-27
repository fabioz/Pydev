package org.python.pydev.editor.codecompletion.revisited.visitors;

import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Call;

/**
 * A definition where we found things as a keyword parameter in a call.
 * 
 * It contains the access to the keyword paramater as its ast and an additional call attribute (and attribute
 * if the call was inside an attribute)
 * 
 * @author fabioz
 */
public class KeywordParameterDefinition extends Definition{

    public Call call;

    public KeywordParameterDefinition(int line, int col, String value, SimpleNode ast, ILocalScope scope, IModule module, Call call) {
        super(line, col, value, ast, scope, module, false);
        this.call = call;
    }

}
