/*
 * Created on Jan 20, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

/**
 * @author Fabio Zadrozny
 */
public class Scope {

    public Stack<SimpleNode> scope = new Stack<SimpleNode>();
    
    public int scopeEndLine = -1;

    public int ifMainLine = -1;
    
    public Scope(Stack<SimpleNode> scope){
        this.scope.addAll(scope);
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Scope)) {
            return false;
        }
        
        Scope s = (Scope) obj;
        
        if(this.scope.size() != s.scope.size()){
            return false;
        }
        
        return checkIfScopesMatch(s);
    }
    
    /**
     * Checks if this scope is an outer scope of the scope passed as a param (s).
     * Or if it is the same scope. 
     * 
     * @param s
     * @return
     */
    public boolean isOuterOrSameScope(Scope s){
        if(this.scope.size() > s.scope.size()){
            return false;
        }
 
        return checkIfScopesMatch(s);
    }

    /**
     * @param s
     * @return
     */
    private boolean checkIfScopesMatch(Scope s) {
        Iterator otIt = s.scope.iterator();
        
        for (Iterator iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = (SimpleNode) iter.next();
            SimpleNode otElement = (SimpleNode) otIt.next();
            
            if(element.beginColumn != otElement.beginColumn)
                return false;
            
            if(element.beginLine != otElement.beginLine)
                return false;
            
            if(! element.getClass().equals(otElement.getClass()))
                return false;
            
            if(! NodeUtils.getFullRepresentationString(element).equals( NodeUtils.getFullRepresentationString(otElement)))
                return false;
            
        }
        return true;
    }
    
    public IToken[] getAllLocalTokens(){
        return getLocalTokens(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    
    public List<ASTEntry> getOcurrences(String occurencesFor) {
        List<ASTEntry> ret = new ArrayList<ASTEntry>();
        
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(this.scope.get(this.scope.size()-1));
        Iterator<ASTEntry> iterator = visitor.getIterator(new Class[]{Name.class, NameTokType.class});
        while(iterator.hasNext()){
            ASTEntry entry = iterator.next();
            if (occurencesFor.equals(entry.getName())){
                ret.add(entry);
            }
        }
        return ret;
    }

    /**
     * @param endLine tokens will only be recognized if its beginLine is higher than this parameter.
     */
    public IToken[] getLocalTokens(int endLine, int col){
        Set<SourceToken> comps = new HashSet<SourceToken>();
        
        for (Iterator iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = (SimpleNode) iter.next();
            
            if (element instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) element;
                for (int i = 0; i < f.args.args.length; i++) {
                    String s = NodeUtils.getRepresentationString(f.args.args[i]);
                    comps.add(new SourceToken(f.args.args[i], s, "", "", "", PyCodeCompletion.TYPE_PARAM));
                }
                
                try {
                    for (int i = 0; i < f.body.length; i++) {
		                GlobalModelVisitor visitor = new GlobalModelVisitor(GlobalModelVisitor.GLOBAL_TOKENS, "");
                        f.body[i].accept(visitor);
                        List t = visitor.tokens;
                        for (Iterator iterator = t.iterator(); iterator.hasNext();) {
                            SourceToken tok = (SourceToken) iterator.next();
                            
                            //if it is found here, it is a local type
                            tok.type = PyCodeCompletion.TYPE_PARAM;
                            if(tok.getAst().beginLine <= endLine){
                                comps.add(tok);
                            }
                            
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        
        return (SourceToken[]) comps.toArray(new SourceToken[0]);
    }

    public List<IToken> getLocalImportedModules(int line, int col, String moduleName) {
        ArrayList<IToken> importedModules = new ArrayList<IToken>();
        for (Iterator iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = (SimpleNode) iter.next();
            
            if (element instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) element;
                for (int i = 0; i < f.body.length; i++) {

                    IToken[] tokens = GlobalModelVisitor.getTokens(f.body[i], GlobalModelVisitor.ALIAS_MODULES, moduleName);
                    for (IToken token : tokens) {
                        importedModules.add(token);
                    }
                }
            }
        }
        return importedModules;
    }

	public ClassDef getClassDef() {
		for(SimpleNode node : this.scope){
			if(node instanceof ClassDef){
				return (ClassDef) node;
			}
		}
		return null;
	}

}












