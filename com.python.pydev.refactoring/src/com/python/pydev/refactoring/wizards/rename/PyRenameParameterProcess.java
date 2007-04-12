package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.util.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;
import com.python.pydev.refactoring.wizards.rename.visitors.FindCallVisitor;

/**
 * The rename parameter is based on the rename function, because it will basically:
 * 1- get the  function definition 
 * 2- get all the references 
 * 
 * 3- change the parameter in the function definition (as well as any references to the
 * parameter inside the function
 * 4- change the parameter in all function calls
 * 
 * 
 * This process will only be available if we can find the function definition
 * (otherwise, we'd have a standard rename any local here)
 * 
 * @author fabioz
 *
 */
public class PyRenameParameterProcess extends PyRenameFunctionProcess{

	private String functionName;

    public PyRenameParameterProcess(Definition definition) {
		super(); 
		//empty, because we'll actually supply a different definition for the superclass (the method 
		//definition, and not the parameter, which we receive here).
		
		Assert.isNotNull(definition.scope, "The scope for a rename parameter must always be provided.");
		
		FunctionDef node = (FunctionDef) definition.scope.getScopeStack().peek();
		super.definition = new Definition(node.beginLine, node.beginColumn, ((NameTok)node.name).id, node, definition.scope, definition.module);
		this.functionName = ((NameTok)node.name).id;
	}
	
	
	/**
	 * These are the methods that we need to override to change the function occurrences for parameter occurrences
	 */
	protected List<ASTEntry> getEntryOccurrencesInSameModule(RefactoringStatus status, String initialName, SimpleNode root) {
		List<ASTEntry> occurrences = super.getEntryOccurrencesInSameModule(status, this.functionName, root);
		return getParameterOccurences(occurrences, root);
	}
	
    protected List<ASTEntry> getOccurrencesInOtherModule(RefactoringStatus status, String initialName, SourceModule module, PythonNature nature) {
        List<ASTEntry> occurrences = super.getOccurrencesInOtherModule(status, this.functionName, module, nature);
        return getParameterOccurences(occurrences, module.getAst());
        
    }
	
    /**
     * This method changes function occurrences for parameter occurrences
     */
    private List<ASTEntry> getParameterOccurences(List<ASTEntry> occurrences, SimpleNode root) {
        List<ASTEntry> ret = new ArrayList<ASTEntry>();
    	for (ASTEntry entry : occurrences) {
            
    		if(entry.node instanceof Name){
				Name name = (Name) entry.node;
				if(name.ctx == Name.Artificial){
					continue;
				}
    		}
            if(entry.parent != null && entry.parent.node instanceof FunctionDef && 
            		entry.node instanceof NameTok && ((NameTok)entry.node).ctx == NameTok.FunctionName){
            	//process a function definition (get the parameters with the given name and
            	//references inside that function)
                processFunctionDef(ret, entry);
                
            }else if(entry.node instanceof Name){
                processFoundName(root, ret, (Name) entry.node);
                
            }else if(entry.node instanceof NameTok){
                processFoundNameTok(root, ret, (NameTok) entry.node);
                
            }
		}
		if(ret.size() > 0){
			//only add comments and strings if there's at least some other occurrence
			ret.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, root));
		}
    	return ret;
	}


    private void processFunctionDef(List<ASTEntry> ret, ASTEntry entry) {
        //this is the actual function definition, so, let's take a look at its arguments... 
        
        FunctionDef node = (FunctionDef) entry.parent.node;
        List<ASTEntry> found = ScopeAnalysis.getLocalOccurrences(request.initialName, node);
        ret.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, node));
        ret.addAll(found);
    }


    private void processFoundNameTok(SimpleNode root, List<ASTEntry> ret, NameTok name) {
        if(name.ctx == NameTok.Attrib){
            Call call = FindCallVisitor.findCall(name, root);
            processCall(ret, call);
        }
    }
    
    private void processFoundName(SimpleNode root, List<ASTEntry> ret, Name name) {
        if(name.ctx == Name.Load){
            Call call = FindCallVisitor.findCall(name, root);
            processCall(ret, call);
        }
    }


    private void processCall(List<ASTEntry> ret, Call call) {
    	if(call == null){
    		return;
    	}
        List<ASTEntry> found = ScopeAnalysis.getLocalOccurrences(request.initialName, call);
        for (ASTEntry entry2 : found) {
            if(entry2.node instanceof NameTok){
                NameTok name2 = (NameTok) entry2.node;
                if(name2.ctx == NameTok.KeywordName){
                    ret.add(entry2);
                }
            }
        }
    }


}
