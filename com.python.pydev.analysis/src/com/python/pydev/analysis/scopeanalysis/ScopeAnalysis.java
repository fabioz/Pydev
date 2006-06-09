package com.python.pydev.analysis.scopeanalysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.IModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

public class ScopeAnalysis {

	/**
	 * @return the list of entries with the name parts of attributes (not taking into account its first
	 * part) that are equal to the occurencesFor string. 
	 */
	public static List<ASTEntry> getAttributeReferences(String occurencesFor, SimpleNode simpleNode) {
	    List<ASTEntry> ret = new ArrayList<ASTEntry>();

        AttributeReferencesVisitor visitor = AttributeReferencesVisitor.create(simpleNode);
        Iterator<ASTEntry> iterator = visitor.getNamesIterator();
        
        while(iterator.hasNext()){
            ASTEntry entry = iterator.next();
            String rep = NodeUtils.getFullRepresentationString(entry.node);
            if (rep.equals(occurencesFor)){
                ret.add(entry);
            }
        }
        return ret;
	}

	
	/**
	 * @param occurencesFor the string we're looking for
	 * @param module the module where we want to find the occurrences
	 * @param scope the scope we're in
	 * @return a list of entries with the occurrences
	 */
    public static List<ASTEntry> getLocalOcurrences(String occurencesFor, IModule module, Scope scope) {
    	SimpleNode simpleNode=null;
    	
    	if(scope.scope.size() > 0){
    		simpleNode = scope.scope.get(scope.scope.size()-1);
    		
    	}else if (module instanceof SourceModule){
    		SourceModule m = (SourceModule) module;
    		simpleNode = m.getAst();
    	}
    	
    	if (simpleNode == null){
    		return new ArrayList<ASTEntry>();
    	}
    	
        return ScopeAnalysis.getLocalOcurrences(occurencesFor, simpleNode);
    }


    /**
     * @param occurencesFor the string we're looking for
     * @param simpleNode we will want the occurences below this node
     * @return a list of entries with the occurrences
     */
	public static List<ASTEntry> getLocalOcurrences(String occurencesFor, SimpleNode simpleNode) {
		return ScopeAnalysis.getLocalOcurrences(occurencesFor, simpleNode, true);
	}


	/**
	 * @return a list of occurrences with the matches we're looking for.
	 * Does only return the first name in attributes if onlyFirstAttribPart is true (otherwise will check all attribute parts)
	 */
	public static List<ASTEntry> getLocalOcurrences(String occurencesFor, SimpleNode simpleNode, final boolean onlyFirstAttribPart) {
	    List<ASTEntry> ret = new ArrayList<ASTEntry>();
	    
	    SequencialASTIteratorVisitor visitor = new SequencialASTIteratorVisitor(){
	    	@Override
	    	public Object visitAttribute(Attribute node) throws Exception {
	    		if(onlyFirstAttribPart){
	    			//this will visit the attribute parts if call, subscript, etc.
	    			AbstractScopeAnalyzerVisitor.visitNeededAttributeParts(node, this);
	    			
	        		List<SimpleNode> attributeParts = NodeUtils.getAttributeParts(node);
	        		atomic(attributeParts.get(0)); //an attribute should always have many parts
	        		traverse(attributeParts.get(0));
	        		return null;
	    		}else{
	    			return super.visitAttribute(node);
	    		}
            }
	    };
        if(simpleNode instanceof FunctionDef){
            //all that because we don't want to visit the name of the function if we've started in a function scope
            FunctionDef d = (FunctionDef) simpleNode;
            try {
                d.args.accept(visitor);
                if(d.decs != null){
                    for(decoratorsType dec : d.decs){
                        if(dec != null){
                            dec.accept(visitor);
                        }
                    }
                }
                if(d.body != null){
                    for(stmtType exp: d.body){
                        if(exp != null){
                            exp.accept(visitor);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else{
    	    
    	    try {
    	    	simpleNode.accept(visitor);
    	    } catch (Exception e) {
    	        throw new RuntimeException(e);
    	    }
        }
	    
	    Iterator<ASTEntry> iterator = visitor.getNamesIterator();
	    while(iterator.hasNext()){
	        ASTEntry entry = iterator.next();
	        if (occurencesFor.equals(entry.getName())){
	            ret.add(entry);
	        }
	    }
	    return ret;
	}


	/**
     * Search for the attributes that start with the passed parameter.
     * 
     * @param occurencesFor has to be the full name of the attribute we're looking for in this case.
     * 
     * So, if you want something as self.aa, the occurencesFor must be 'self.aa'. If the attribute
     * is longer, it will still be returned (because when looking for self.aa.m1, we will
     * actually have 2 attributes returned, one for self.aa and another for aa.m1, in which case
     * we will return the one correspondent to self.aa)
     */
    public static List<ASTEntry> getAttributeOcurrences(String occurencesFor, SimpleNode simpleNode){
        List<ASTEntry> ret = new ArrayList<ASTEntry>();

        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(simpleNode);
        Iterator<ASTEntry> iterator = visitor.getIterator(Attribute.class);
        
        while(iterator.hasNext()){
            ASTEntry entry = iterator.next();
            String rep = NodeUtils.getFullRepresentationString(entry.node);
            if (rep.equals(occurencesFor)){
                ret.add(entry);
            }
        }
        return ret;
   }

}
