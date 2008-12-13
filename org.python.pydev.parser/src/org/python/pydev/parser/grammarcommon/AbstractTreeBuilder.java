package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Exec;

public abstract class AbstractTreeBuilder implements ITreeBuilder, ITreeConstants {

    private SimpleNode lastOpened;
    public final SimpleNode openNode(int id) {
        SimpleNode ret;
        
        //The constant that comes in is the constant defined in PythonGrammar24TreeConstants
        //TODO: Translate JJT* constant to TREE_BUILDER** constant
        
        if(id == JJTEXEC_STMT){
            ret = new Exec(null, null, null);
            ret.setId(id);
        }else{
            ret = new IdentityNode(id);
        }
        lastOpened = ret;
        return ret;
    }
    
    public final SimpleNode getLastOpened() {
        return lastOpened;
    }

}
