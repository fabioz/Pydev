/*
 * Created on Jan 20, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IToken;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

/**
 * @author Fabio Zadrozny
 */
public class LocalScope implements ILocalScope {

    //the first node from the stack is always the module itself (if it's not there, it means it is a compiled module scope)
    public FastStack<SimpleNode> scope = new FastStack<SimpleNode>();
    
    public int scopeEndLine = -1;

    public int ifMainLine = -1;
    
    public LocalScope(FastStack<SimpleNode> scope){
        this.scope.addAll(scope);
    }
    
    public FastStack<SimpleNode> getScopeStack(){
        return scope;
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof LocalScope)) {
            return false;
        }
        
        LocalScope s = (LocalScope) obj;
        
        if(this.scope.size() != s.scope.size()){
            return false;
        }
        
        return checkIfScopesMatch(s);
    }
    
    /** 
     * @see org.python.pydev.core.ILocalScope#isOuterOrSameScope(org.python.pydev.editor.codecompletion.revisited.visitors.LocalScope)
     */
    public boolean isOuterOrSameScope(ILocalScope s){
        if(this.scope.size() > s.getScopeStack().size()){
            return false;
        }
 
        return checkIfScopesMatch(s);
    }

    /**
     * @param s the scope we're checking for
     * @return if the scope passed as a parameter starts with the same scope we have here. It should not be
     * called if the size of the scope we're checking is bigger than the size of 'this' scope. 
     */
    private boolean checkIfScopesMatch(ILocalScope s) {
        Iterator otIt = s.getScopeStack().iterator();
        
        for (Iterator iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = (SimpleNode) iter.next();
            SimpleNode otElement = (SimpleNode) otIt.next();
            
            if(element.beginColumn != otElement.beginColumn)
                return false;
            
            if(element.beginLine != otElement.beginLine)
                return false;
            
            if(! element.getClass().equals(otElement.getClass()))
                return false;
            
            String rep1 = NodeUtils.getFullRepresentationString(element);
            String rep2 = NodeUtils.getFullRepresentationString(otElement);
            if(rep1 == null || rep2 == null){
                if(rep1 != rep2){
                    return false;
                }
                
            }else if(!rep1.equals(rep2))
                return false;
            
        }
        return true;
    }
    
    /** 
     * @see org.python.pydev.core.ILocalScope#getAllLocalTokens()
     */
    public IToken[] getAllLocalTokens(){
        return getLocalTokens(Integer.MAX_VALUE, Integer.MAX_VALUE, false);
    }
    
    
    /** 
     * @see org.python.pydev.core.ILocalScope#getLocalTokens(int, int, boolean)
     */
    public IToken[] getLocalTokens(int endLine, int col, boolean onlyArgs){
        Set<SourceToken> comps = new HashSet<SourceToken>();
        
        for (Iterator iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = (SimpleNode) iter.next();
            
            if (element instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) element;
                for (int i = 0; i < f.args.args.length; i++) {
                    String s = NodeUtils.getRepresentationString(f.args.args[i]);
                    comps.add(new SourceToken(f.args.args[i], s, "", "", "", IToken.TYPE_PARAM));
                }
                if(onlyArgs){
                    continue;
                }
                try {
                    for (int i = 0; i < f.body.length; i++) {
		                GlobalModelVisitor visitor = new GlobalModelVisitor(GlobalModelVisitor.GLOBAL_TOKENS, "");
                        f.body[i].accept(visitor);
                        List t = visitor.tokens;
                        for (Iterator iterator = t.iterator(); iterator.hasNext();) {
                            SourceToken tok = (SourceToken) iterator.next();
                            
                            //if it is found here, it is a local type
                            tok.type = IToken.TYPE_LOCAL;
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

    /**
     * 
     * @param argName this is the argument (cannot have dots)
     * @param activationToken this is the actual activation token we're looking for
     * (may have dots).
     * 
     * Note that argName == activationToken first part before the dot (they may be equal)
     * @return a list of tokens for the local 
     */
    public Collection<IToken> getInterfaceForLocal(String activationToken) {
        Set<SourceToken> comps = new HashSet<SourceToken>();

        Iterator<SimpleNode> it = this.scope.topDownIterator();
        if(it.hasNext());
        SimpleNode element = it.next();
        
        String dottedActTok = activationToken+'.';
        //ok, that's the scope we have to analyze
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(element);
        Iterator<ASTEntry> iterator = visitor.getIterator(Attribute.class);
        
        while(iterator.hasNext()){
            ASTEntry entry = iterator.next();
            String rep = NodeUtils.getFullRepresentationString(entry.node);
            if(rep.startsWith(dottedActTok)){
                rep = rep.substring(dottedActTok.length());
                comps.add(new SourceToken(entry.node, FullRepIterable.getFirstPart(rep), "", "", "", IToken.TYPE_OBJECT_FOUND_INTERFACE));
            }
        }
        return new ArrayList<IToken>(comps);
    }


    /** 
     * @see org.python.pydev.core.ILocalScope#getLocalImportedModules(int, int, java.lang.String)
     */
    public List<IToken> getLocalImportedModules(int line, int col, String moduleName) {
        ArrayList<IToken> importedModules = new ArrayList<IToken>();
        for (Iterator iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = (SimpleNode) iter.next();
            
            if (element instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) element;
                for (int i = 0; i < f.body.length; i++) {

                    importedModules.addAll(GlobalModelVisitor.getTokens(f.body[i], GlobalModelVisitor.ALIAS_MODULES, moduleName, null));
                }
            }
        }
        return importedModules;
    }

    /** 
     * @see org.python.pydev.core.ILocalScope#getClassDef()
     */
	public ClassDef getClassDef() {
		for(Iterator<SimpleNode> it = this.scope.topDownIterator(); it.hasNext();){
            SimpleNode node = it.next();
			if(node instanceof ClassDef){
				return (ClassDef) node;
			}
		}
		return null;
	}

	/** 
     * @see org.python.pydev.core.ILocalScope#isLastClassDef()
     */
	public boolean isLastClassDef() {
		if(this.scope.size() > 0 && this.scope.peek() instanceof ClassDef){
			return true;
		}
		return false;
	}

    public Iterator iterator() {
        return scope.topDownIterator();
    }

    public int getIfMainLine() {
        return ifMainLine;
    }

    public int getScopeEndLine() {
        return scopeEndLine;
    }

    public void setIfMainLine(int original) {
        this.ifMainLine = original;
    }

    public void setScopeEndLine(int beginLine) {
        this.scopeEndLine = beginLine;
    }


}












