/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import java.util.List;

import org.python.parser.SimpleNode;


/**
 * The module should have all the information we need for code completion, find definition, and
 * refactoring on a module.
 * 
 * Note: A module may be represented by a folder if it has an __init__.py file that represents the
 * module or a python file.
 * 
 * Any of those must be a valid python token to be recognized (from the PYTHONPATH).
 * 
 * We don't reuse the ModelUtils already created as we still have to transport a lot of logic to it
 * to make it workable, so, the attempt here is to use a thin tier.  
 * 
 * @author Fabio Zadrozny
 */
public class SourceModule extends AbstractModule{

    /**
     * This is the abstract syntax tree based on the jython parser output.
     */
    private SimpleNode ast;
    
    /**
     * @return a reference to all the modules that are imported from this one in the global context as a
     * from xxx import *
     * 
     * This modules are treated specially, as we don't care which tokens were imported. When this is requested,
     * the module is prompted for its tokens. 
     */
    public List getWildImportedModules(){
        return getTokens(GlobalModelVisitor.WILD_MODULES);
    }
    
    /**
     * @return a reference to all the modules that are imported from this one in the global context in the 
     * following constructions:
     * 
     * from xxx import xxx
     * import xxx
     * import xxx as ...
     * from xxx import xxx as .... 
     */
    public List getTokenImportedModules(){
        return getTokens(GlobalModelVisitor.ALIAS_MODULES);
    }
   
    /**
     * @return the tokens that are present in the global scope.
     * 
     * The tokens can be class definitions, method definitions and attributes. 
     */
    public List getGlobalTokens(){
        return getTokens(GlobalModelVisitor.GLOBAL_TOKENS);
    }
    
    /**
     * @param which
     * @return
     */
    private List getTokens(int which) {
        try {
            GlobalModelVisitor modelVisitor = new GlobalModelVisitor(which);
            ast.accept(modelVisitor);
            return modelVisitor.tokens;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    /**
     * @param n
     */
    public SourceModule(SimpleNode n) {
        this.ast = n;
    }
}
