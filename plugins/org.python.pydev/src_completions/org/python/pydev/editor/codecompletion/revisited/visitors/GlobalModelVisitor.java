/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.HashSet;
import java.util.Iterator;

import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.List;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.exprType;

/**
 * This class visits only the global context. Other visitors should visit contexts inside of this one.
 * 
 * @author Fabio Zadrozny
 */
public class GlobalModelVisitor extends AbstractVisitor {

    private int visitWhat;
    private SourceToken __all__;
    private Assign __all__Assign;
    private exprType[] __all__AssignTargets;
    private Assign lastAssign;
    private boolean onlyAllowTokensIn__all__;

    public GlobalModelVisitor(int visitWhat, String moduleName, boolean onlyAllowTokensIn__all__) {
        this(visitWhat, moduleName, onlyAllowTokensIn__all__, false);
    }
    
    /**
     * @param moduleName
     * @param global_tokens2
     */
    public GlobalModelVisitor(int visitWhat, String moduleName, boolean onlyAllowTokensIn__all__, boolean lookingInLocalContext) {
        this.visitWhat = visitWhat;
        this.moduleName = moduleName;
        this.onlyAllowTokensIn__all__ = onlyAllowTokensIn__all__;
        this.tokens.add(new SourceToken(new Name("__dict__", Name.Load, false), "__dict__", "", "", moduleName));
        if(moduleName != null && moduleName.endsWith("__init__")){
            this.tokens.add(new SourceToken(new Name("__path__", Name.Load, false), "__path__", "", "", moduleName));
        }
        if(!lookingInLocalContext && ((this.visitWhat & GLOBAL_TOKENS) != 0)){
            //__file__ is always available for any module
            this.tokens.add(new SourceToken(new Name("__file__", Name.Load, false), "__file__", "", "", moduleName));
            this.tokens.add(new SourceToken(new Name("__name__", Name.Load, false), "__name__", "", "", moduleName));
        }
    }

    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    public Object visitClassDef(ClassDef node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        if ((this.visitWhat & GLOBAL_TOKENS) != 0) {
            addToken(node);
        } 
        return null;
    }

    public Object visitFunctionDef(FunctionDef node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        if ((this.visitWhat & GLOBAL_TOKENS) != 0) {
            addToken(node);
        }
        return null;
    }

    /**
     * Name should be whithin assign.
     * 
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitAssign(org.python.pydev.parser.jython.ast.Assign)
     */
    public Object visitAssign(Assign node) throws Exception {
        lastAssign = node;
        node.traverse(this);
        return null;
    }

    /**
     * Visiting some name
     * 
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitName(org.python.pydev.parser.jython.ast.Name)
     */
    public Object visitName(Name node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        if ((this.visitWhat & GLOBAL_TOKENS) != 0) {
            if (node.ctx == Name.Store) {
                SourceToken added = addToken(node);
                if(added.getRepresentation().equals("__all__") && __all__Assign == null){
                    __all__ = added;
                    __all__Assign = lastAssign;
                    __all__AssignTargets = lastAssign.targets;
                }
            }else if(node.ctx == Name.Load){
                if(node.id.equals("__all__")){
                    //if we find __all__ more than once, let's clear it (we can only have __all__ = list of strings... if later
                    //an append, extend, etc is done in it, we have to skip this heuristic).
                    __all__AssignTargets = null;
                }
            }
        }
        return null;
    }

    /**
     * Visiting some import from
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitImportFrom(org.python.pydev.parser.jython.ast.ImportFrom)
     */
    public Object visitImportFrom(ImportFrom node) throws Exception {
        if ((this.visitWhat & WILD_MODULES) != 0) {
            makeWildImportToken(node, this.tokens, moduleName);
        } 
        
        if ((this.visitWhat & ALIAS_MODULES) != 0) {
            makeImportToken(node, this.tokens, moduleName, true);
        }
        return null;
    }



    /**
     * Visiting some import
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitImport(org.python.pydev.parser.jython.ast.Import)
     */
    public Object visitImport(Import node) throws Exception {
        if ((this.visitWhat & ALIAS_MODULES) != 0) {
            makeImportToken(node, this.tokens, moduleName, true);
        }
        return null;
    }

    
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitStr(org.python.pydev.parser.jython.ast.Str)
     */
    public Object visitStr(Str node) throws Exception {
        if((this.visitWhat & MODULE_DOCSTRING) != 0){
            this.tokens.add(new SourceToken(node, node.s, "", "", moduleName));
        }
        return null;
    }
    
    
    /**
     * Overridden to check __all__
     */
    @Override
    protected void finishVisit() {
        if(onlyAllowTokensIn__all__){
            filterAll(this.tokens);
        }
    }

    
    /**
     * This method will filter the passed tokens given the __all__ that was found when visiting.
     * 
     * @param tokens the tokens to be filtered (IN and OUT parameter)
     */
    public void filterAll(java.util.List<IToken> tokens) {
        if(__all__ != null){
            SimpleNode ast = __all__.getAst();
            //just checking it
            if(__all__AssignTargets != null && __all__AssignTargets.length == 1 && __all__AssignTargets[0] == ast){
                HashSet<String> validTokensInAll = new HashSet<String>();
                exprType value = __all__Assign.value;
                exprType[] elts = null;
                if(value instanceof List){
                    List valueList = (List) value;
                    if(valueList.elts != null){
                    	elts = valueList.elts;
                    }
                }else if(value instanceof Tuple){
                	Tuple valueList = (Tuple) value;
                	if(valueList.elts != null){
                		elts = valueList.elts;
                	}
                }
                
                if(elts != null){
	                for(exprType elt:elts){
	                    if(elt instanceof Str){
	                        Str str = (Str) elt;
	                        validTokensInAll.add(str.s);
	                    }
                    }
                }

                
                if(validTokensInAll.size() > 0){
                    for(Iterator<IToken> it = tokens.iterator();it.hasNext();){
                        IToken tok = it.next();
                        if(!validTokensInAll.contains(tok.getRepresentation())){
                            it.remove();
                        }
                    }
                }
            }
        }
    }
}