package org.python.pydev.editor.codecompletion.revisited;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.python.pydev.core.FindInfo;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IToken;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ReturnVisitor;

/**
 * This class is used to analyse the assigns in the code and bring actual completions for them.
 */
public class AssignAnalysis {

    /**
     * If we got here, either there really is no definition from the token
     * or it is not looking for a definition. This means that probably
     * it is something like.
     * 
     * It also can happen in many scopes, so, first we have to check the current
     * scope and then pass to higher scopes
     * 
     * e.g. foo = Foo()
     *      foo. | Ctrl+Space
     * 
     * so, first thing is discovering in which scope we are (Storing previous scopes so 
     * that we can search in other scopes as well).
     */
    public List<IToken> getAssignCompletions(ICodeCompletionASTManager manager, IModule module, ICompletionState state) {
        ArrayList<IToken> ret = new ArrayList<IToken>();
        if (module instanceof SourceModule) {
            SourceModule s = (SourceModule) module;
            
            try {
                Definition[] defs = s.findDefinition(state, state.getLine()+1, state.getCol()+1, state.getNature(), new ArrayList<FindInfo>());
                for (int i = 0; i < defs.length; i++) {
                    //go through all definitions found and make a merge of it...
                    Definition definition = defs[i];
                    
                    AssignDefinition assignDefinition = null;
                    if(definition instanceof AssignDefinition){
                        assignDefinition = (AssignDefinition) definition;
                    }
                    
                    if(!(definition.ast instanceof FunctionDef)){
                        addNonFunctionDefCompletionsFromAssign(manager, state, ret, s, definition, assignDefinition);
                    }else{
                        addFunctionDefCompletionsFromAssign(manager, state, ret, s, definition, assignDefinition);
                    }
                }
                
                
            } catch (CompletionRecursionException e) {
                //thats ok
            } catch (Exception e) {
                throw new RuntimeException(e);
            } catch (Throwable t) {
                throw new RuntimeException("A throwable exception has been detected "+t.getClass());
            }
        }
        return ret;
    }



    private void addFunctionDefCompletionsFromAssign(ICodeCompletionASTManager manager, ICompletionState state, ArrayList<IToken> ret, SourceModule s, Definition definition, AssignDefinition assignDefinition) throws CompletionRecursionException {
        FunctionDef functionDef = (FunctionDef) definition.ast;
        for(Return return1: ReturnVisitor.findReturns(functionDef)){
            ICompletionState copy = state.getCopy();
            copy.setActivationToken (NodeUtils.getFullRepresentationString(return1.value));
            copy.setLine(return1.value.beginLine-1);
            copy.setCol(return1.value.beginColumn-1);
            IModule module = definition.module;
  
            state.checkDefinitionMemory(module, definition);
                    
            IToken[] tks = manager.getCompletionsForModule(module, copy);
            if(tks.length > 0){
                ret.addAll(Arrays.asList(tks));
            }            
        }
    }



    /**
     * This method will look into the right side of an assign and its definition and will try to gather the tokens for
     * it, knowing that it is dealing with a non-function def token for the definition found.
     * 
     * @param ret the place where the completions should be added
     * @param assignDefinition may be null if it was not actually found as an assign
     */
    private void addNonFunctionDefCompletionsFromAssign(ICodeCompletionASTManager manager, ICompletionState state, ArrayList<IToken> ret, SourceModule s, Definition definition, AssignDefinition assignDefinition) throws CompletionRecursionException {
        IModule module;
        if(definition.ast instanceof ClassDef){
            state.setLookingFor(ICompletionState.LOOKING_FOR_UNBOUND_VARIABLE);
            ret.addAll(s.getClassToks(state, manager, definition.ast));
            
            
        }else{
            boolean lookForAssign = true;
            if(assignDefinition != null && assignDefinition.foundAsGlobal){
                //it may be declared as a global with a class defined in the local scope
                IToken[] allLocalTokens = assignDefinition.scope.getAllLocalTokens();
                for (IToken token : allLocalTokens) {
                    if(token.getRepresentation().equals(assignDefinition.value)){
                        if(token instanceof SourceToken){
                            SourceToken srcToken = (SourceToken) token;
                            if(srcToken.getAst() instanceof ClassDef){
                                List<IToken> classToks = s.getClassToks(state, manager, srcToken.getAst());
                                if(classToks.size() > 0){
                                    lookForAssign = false;
                                    ret.addAll(classToks);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            
            if(lookForAssign){
                //we might want to extend that later to check the return of some function...
                state.setLookingFor(ICompletionState.LOOKING_FOR_ASSIGN);
                ICompletionState copy = state.getCopy();
                copy.setActivationToken (definition.value);
                copy.setLine(definition.line);
                copy.setCol(definition.col);
                module = definition.module;
      
                state.checkDefinitionMemory(module, definition);
                        
                IToken[] tks = manager.getCompletionsForModule(module, copy);
                
                if(assignDefinition != null){
                    Collection<IToken> interfaceForLocal = assignDefinition.scope.getInterfaceForLocal(
                            FullRepIterable.getFirstPart(assignDefinition.target), assignDefinition.target);
                    ret.addAll(interfaceForLocal);
                }
                
                if(tks.length > 0){
                    ret.addAll(Arrays.asList(tks));
                }
            }
        }
    }

}
