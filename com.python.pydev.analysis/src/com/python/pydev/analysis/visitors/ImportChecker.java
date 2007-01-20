/*
 * Created on 21/08/2005
 */
package com.python.pydev.analysis.visitors;

import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.Tuple3;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

import com.python.pydev.analysis.scopeanalysis.AbstractScopeAnalyzerVisitor;

/**
 * The import checker not only generates information on errors for unresolved modules, but also gathers
 * dependency information so that we can do incremental building of dependent modules.
 * 
 * @author Fabio
 */
public class ImportChecker {

    /**
     * this is the nature we are analyzing
     */
    private IPythonNature nature;

    /**
     * this is the name of the module that we are analyzing
     */
    private String moduleName;

    private AbstractScopeAnalyzerVisitor visitor;

    /**
     * This is the information stored about some import:
     * Contains the actual module, the representation in the current module and whether it was resolved or not.
     */
    public static class ImportInfo{
    	public IModule mod;
    	public String rep;
    	public boolean wasResolved;
	    	
    	public ImportInfo(){
    		this(null,null,false);
    	}
    	public ImportInfo(IModule mod, String rep){
    		this(mod, rep, false);
    	}
    	
    	public ImportInfo(IModule mod, String rep, boolean wasResolved){
    		this.mod = mod;
    		this.rep = rep;
    		this.wasResolved = wasResolved;
    	}
        
        @Override
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("ImportInfo(");
            buffer.append(" Resolved:");
            buffer.append(wasResolved);
            if(wasResolved){
                buffer.append(" Rep:");
                buffer.append(rep);
                buffer.append(" Mod:");
                buffer.append(mod.getName());
            }
            buffer.append(")");
            return buffer.toString();
        }
    }
    
    /**
     * constructor - will remove all dependency info on the project that we will start to analyze
     */
    public ImportChecker(AbstractScopeAnalyzerVisitor visitor, IPythonNature nature, String moduleName) {
        this.nature = nature;
        this.moduleName = moduleName;
        this.visitor = visitor;
    }

    /**
     * @param token MUST be an import token
     * @param reportUndefinedImports 
     * 
     * @return the module where the token was found and a String representing the way it was found 
     * in the module.
     * 
     * Note: it may return information even if the token was not found in the representation required. This is useful
     * to get dependency info, because it is actually dependent on the module, event though it does not have the
     * token we were looking for.
     */
    public ImportInfo visitImportToken(IToken token, boolean reportUndefinedImports) {
        //try to find it as a relative import
        boolean wasResolved = false;
        Tuple3<IModule, String, IToken> modTok = null;
		if(token instanceof SourceToken){
        	
        	ICodeCompletionASTManager astManager = nature.getAstManager();
            modTok = astManager.findOnImportedMods(new IToken[]{token}, nature, token.getRepresentation(), moduleName);
        	if(modTok != null && modTok.o1 != null){

        		if(modTok.o2.length() == 0){
        		    wasResolved = true;
                    
        		} else if( astManager.getRepInModule(modTok.o1, modTok.o2, nature) != null){
        		    wasResolved = true;
                }
        	}
        	
            
            //if it got here, it was not resolved
        	if(!wasResolved && reportUndefinedImports){
                visitor.onAddUnresolvedImport(token);
        	}
            
        }
        
        //might still return a modTok, even if the token we were looking for was not found.
        if(modTok != null){
        	return new ImportInfo(modTok.o1, modTok.o2, wasResolved);
        }else{
        	return new ImportInfo(null, null, wasResolved);
        }
    }

}
