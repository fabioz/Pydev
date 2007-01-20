/*
 * Created on Dec 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.aliasType;
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

    protected List<IToken> tokens = new ArrayList<IToken>();
    
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
        SourceToken t = makeToken(node, moduleName);
        this.tokens.add(t);
    }


    /**
     * @param node
     * @return
     */
    public static SourceToken makeToken(SimpleNode node, String moduleName) {
        return new SourceToken(node, NodeUtils.getRepresentationString(node), NodeUtils.getNodeArgs(node), NodeUtils.getNodeDocString(node), moduleName);
    }

    /**
     * same as make token, but returns the full representation for a token, instead of just a 'partial' name
     */
    public static SourceToken makeFullNameToken(SimpleNode node, String moduleName) {
        return new SourceToken(node, NodeUtils.getFullRepresentationString(node), NodeUtils.getNodeArgs(node), NodeUtils.getNodeDocString(node), moduleName);
    }
    
    
    /**
     * This function creates source tokens from a wild import node.
     * 
     * @param node the import node
     * @param tokens OUT used to add the source token
     * @param moduleName the module name
     * 
     * @return the tokens list passed in or the created one if it was null
     */
    public static IToken makeWildImportToken(ImportFrom node, List<IToken> tokens, String moduleName) {
        if(tokens == null){
            tokens = new ArrayList<IToken>();
        }
        SourceToken sourceToken = null;
        if(isWildImport(node)){
            sourceToken = new SourceToken(node, ((NameTok)node.module).id, "",  "", moduleName);
            tokens.add(sourceToken);
        }
        return sourceToken;
    }

    public static List<IToken> makeImportToken(SimpleNode node, List<IToken> tokens, String moduleName, boolean allowForMultiple) {
    	if(node instanceof Import){
    		return makeImportToken((Import)node, tokens, moduleName, allowForMultiple);
    	}
    	if(node instanceof ImportFrom){
    		ImportFrom i = (ImportFrom) node;
    		if(isWildImport(i)){
    			makeWildImportToken(i, tokens, moduleName);
    			return tokens;
    		}
    		return makeImportToken((ImportFrom)node, tokens, moduleName, allowForMultiple);
    	}
    	
    	throw new RuntimeException("Unable to create token for the passed import ("+node+")");
    }

    /**
     * This function creates source tokens from an import node.
     * 
     * @param node the import node
     * @param moduleName the module name where this token was found
     * @param tokens OUT used to add the source tokens (may create many from a single import)
     * @param allowForMultiple is used to indicate if an import in the format import os.path should generate one token for os
     * and another for os.path or just one for both with os.path
     * 
     * @return the tokens list passed in or the created one if it was null
     */
    public static List<IToken> makeImportToken(Import node, List<IToken> tokens, String moduleName, boolean allowForMultiple) {
        aliasType[] names = node.names;
        return makeImportToken(node, tokens, names, moduleName, "", allowForMultiple);
    }
    
    /**
     * The same as above but with ImportFrom
     */
    public static List<IToken> makeImportToken(ImportFrom node, List<IToken> tokens, String moduleName, boolean allowForMultiple) {
        aliasType[] names = node.names;
        String importName = ((NameTok)node.module).id;
        
        return makeImportToken(node, tokens, names, moduleName, importName, allowForMultiple);
    }

    /**
     * The same as above
     */
    private static List<IToken> makeImportToken(SimpleNode node, List<IToken> tokens, aliasType[] names, String module, String initialImportName, boolean allowForMultiple) {
        if(tokens == null){
            tokens = new ArrayList<IToken>();
        }
        
        if(initialImportName.length() > 0){
        	initialImportName = initialImportName+".";
        }
        
        for (int i = 0; i < names.length; i++) {
            aliasType aliasType = names[i];
            
            String name = null;
            String original = ((NameTok)aliasType.name).id;
            
            if(aliasType.asname != null){
                name = ((NameTok)aliasType.asname).id;
            }
            
            if(name == null){
                FullRepIterable iterator = new FullRepIterable(original);
                for (String rep : iterator) {
                    SourceToken sourceToken = new SourceToken(node, rep, "", "", module, initialImportName+rep);
                    tokens.add(sourceToken);
                }
            }else{
                SourceToken sourceToken = new SourceToken(node, name, "", "", module, initialImportName+original);
                tokens.add(sourceToken);
            }

        }
        return tokens;
    }

    
    public static boolean isWildImport(SimpleNode node) {
        if (node instanceof ImportFrom) {
            ImportFrom n = (ImportFrom) node;
            return isWildImport(n);
        }
        return false;
    }
    
    public static boolean isWildImport(IToken generator) {
        if (generator instanceof SourceToken) {
            SourceToken t = (SourceToken) generator;
            return isWildImport(t.getAst());
        }
        return false;
    }

    /**
     * @param node the node to analyze
     * @return whether it is a wild import
     */
    public static boolean isWildImport(ImportFrom node) {
        return node.names.length == 0;
    }

    /**
     * @param node the node to analyze
     * @return whether it is an alias import
     */
    public static boolean isAliasImport(ImportFrom node) {
        return node.names.length > 0;
    }
    
    public List<IToken> getTokens() {
        return this.tokens;
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
            modelVisitor = new InnerModelVisitor(moduleName);
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
    public static Str isIfMAinNode(If node) {
        if (node.test instanceof Compare) {
    		Compare compareNode = (Compare)node.test;
    		// handcrafted structure walking
    		if (compareNode.left instanceof Name 
    			&& ((Name)compareNode.left).id.equals("__name__")
    			&& compareNode.ops != null
    			&& compareNode.ops.length == 1 
    			&& compareNode.ops[0] == Compare.Eq){
                
    		    if ( compareNode.comparators != null
        			&& compareNode.comparators.length == 1
        			&& compareNode.comparators[0] instanceof Str 
        			&& ((Str)compareNode.comparators[0]).s.equals("__main__")){
        			return (Str)compareNode.comparators[0];
                }
    		}
    	}
        return null;
    }


    
}
