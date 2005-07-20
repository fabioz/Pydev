/*
 * Created on Dec 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.List;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Compare;
import org.python.parser.ast.If;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.Str;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.aliasType;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractVisitor extends VisitorBase{

    public static final int GLOBAL_TOKENS = 1;

    public static final int WILD_MODULES = 2;
    
    public static final int ALIAS_MODULES = 3;
    
    public static final int MODULE_DOCSTRING = 4;
    
    public static final int INNER_DEFS = 5;

    protected List tokens = new ArrayList();
    
    /**
     * Module being visited.
     */
    protected String moduleName;
    
    /**
     * Adds a token with a docstring.
     * 
     * @param node
     */
    protected void addToken(SimpleNode node) {
        //add the token
        SourceToken t = new SourceToken(node, NodeUtils.getRepresentationString(node), NodeUtils.getNodeArgs(node), NodeUtils.getNodeDocString(node), moduleName);
        this.tokens.add(t);
    }

    
    /**
     * This function creates source tokens from a wild import node.
     * 
     * @param node the import node
     * @param tokens OUT used to add the source token
     * @param moduleName the module name
     */
    public static void makeWildImportToken(ImportFrom node, List tokens, String moduleName) {
        if(isWildImport(node)){
            tokens.add(new SourceToken(node, node.module, "",  "", moduleName));
        }
    }


    /**
     * This function creates source tokens from an import node.
     * 
     * @param node the import node
     * @param moduleName the module name
     * @param tokens OUT used to add the source tokens (may create many from a single import)
     */
    public static void makeImportToken(Import node, List tokens, String moduleName) {
        aliasType[] names = node.names;
        makeImportToken(node, tokens, names, moduleName);
    }
    
    /**
     * The same as above but with ImportFrom
     */
    public static void makeImportToken(ImportFrom node, List tokens, String moduleName) {
        aliasType[] names = node.names;
        makeImportToken(node, tokens, names, node.module);
    }

    /**
     * The same as above
     */
    private static void makeImportToken(SimpleNode node, List tokens, aliasType[] names, String module) {
        for (int i = 0; i < names.length; i++) {
            String name = names[i].name;
            String original = names[i].name;
            if(names[i].asname != null){
                name = names[i].asname;
            }
            tokens.add(new SourceToken(node, name, "", "", module, original));
        }
    }

    
    /**
     * @param node
     * @return
     */
    public static boolean isWildImport(ImportFrom node) {
        return node.names.length == 0;
    }

    /**
     * @param node
     * @return
     */
    public static boolean isAliasImport(ImportFrom node) {
        return node.names.length > 0;
    }
    
    /**
     * This method transverses the ast and returns a list of found tokens.
     * 
     * @param ast
     * @param which
     * @param name
     * @return
     * @throws Exception
     */
    public static IToken[] getTokens(SimpleNode ast, int which, String moduleName) {
        AbstractVisitor modelVisitor;
        if(which == INNER_DEFS){
            modelVisitor = new InnerModelVisitor();
        }else{
            modelVisitor = new GlobalModelVisitor(which, moduleName);
        }
        
        if (ast != null){
            try {
                ast.accept(modelVisitor);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return (SourceToken[]) modelVisitor.tokens.toArray(new SourceToken[0]);
        }else{
            return new SourceToken[0];
        }
    }

    /**
     * @param node
     */
    public static boolean isIfMAinNode(If node) {
        if (node.test instanceof Compare) {
    		Compare compareNode = (Compare)node.test;
    		// handcrafted structure walking
    		if (compareNode.left instanceof Name 
    			&& ((Name)compareNode.left).id.equals("__name__")
    			&& compareNode.ops != null
    			&& compareNode.ops.length == 1 
    			&& compareNode.ops[0] == Compare.Eq)
    			if ( true
    			&& compareNode.comparators != null
    			&& compareNode.comparators.length == 1
    			&& compareNode.comparators[0] instanceof Str 
    			&& ((Str)compareNode.comparators[0]).s.equals("__main__"))
    		{
    			return true;
    		}
    	}
        return false;
    }
    
}
