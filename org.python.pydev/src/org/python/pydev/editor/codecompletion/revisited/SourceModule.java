/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Str;

/**
 * The module should have all the information we need for code completion, find definition, and refactoring on a module.
 * 
 * Note: A module may be represented by a folder if it has an __init__.py file that represents the module or a python file.
 * 
 * Any of those must be a valid python token to be recognized (from the PYTHONPATH).
 * 
 * We don't reuse the ModelUtils already created as we still have to transport a lot of logic to it to make it workable, so, the attempt
 * here is to use a thin tier.
 * 
 * NOTE: When using it, don't forget to use the superclass abstraction.
 *  
 * @author Fabio Zadrozny
 */
public class SourceModule extends AbstractModule {

    /**
     * This is the abstract syntax tree based on the jython parser output.
     */
    private SimpleNode ast;

    /**
     * File that originated the syntax tree.
     */
    private File file;

    /**
     * @return a reference to all the modules that are imported from this one in the global context as a from xxx import *
     * 
     * This modules are treated specially, as we don't care which tokens were imported. When this is requested, the module is prompted for
     * its tokens.
     */
    public IToken[] getWildImportedModules() {
        return getTokens(GlobalModelVisitor.WILD_MODULES);
    }

    /**
     * @return a reference to all the modules that are imported from this one in the global context in the following constructions:
     * 
     * from xxx import xxx import xxx import xxx as ... from xxx import xxx as ....
     */
    public IToken[] getTokenImportedModules() {
        return getTokens(GlobalModelVisitor.ALIAS_MODULES);
    }

    /**
     * @return the tokens that are present in the global scope.
     * 
     * The tokens can be class definitions, method definitions and attributes.
     */
    public IToken[] getGlobalTokens() {
        return getTokens(GlobalModelVisitor.GLOBAL_TOKENS);
    }

    /**
     * @return a string representing the module docstring.
     */
    public String getDocString() {
        IToken[] l = getTokens(GlobalModelVisitor.MODULE_DOCSTRING);
        if (l.length > 0) {
            SimpleNode a = ((SourceToken) l[0]).getAst();

            return ((Str) a).s;
        }
        return "";
    }

    /**
     * @param which
     * @return a list of IToken
     */
    private IToken[] getTokens(int which) {
        try {
            return GlobalModelVisitor.getTokens(ast, which, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new IToken[0];
    }

    /**
     * 
     * @param name
     * @param f
     * @param n
     */
    public SourceModule(String name, File f, SimpleNode n) {
        super(name);
        this.ast = n;
        this.file = f;
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.AbstractModule#getGlobalTokens(java.lang.String)
     */
    public IToken[] getGlobalTokens(String token) {
        IToken[] t = getTokens(GlobalModelVisitor.GLOBAL_TOKENS);
        if(t instanceof SourceToken[]){
	        SourceToken[] tokens = (SourceToken[]) t;
	        for (int i = 0; i < tokens.length; i++) {
	            if(tokens[i].getRepresentation().equals(token)){
	                ast = tokens[i].getAst();
	                try {
	                    //TODO: COMPLETION: get the completions for the whole hierarchy if this is a class!!
	                    return GlobalModelVisitor.getTokens(ast, GlobalModelVisitor.INNER_DEFS, name);
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }
	        }
        }else{
            System.out.println("Expecting SourceToken, got: "+t.getClass().getName());
        }
        return new IToken[0];
    }

}