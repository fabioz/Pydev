/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.messages.IMessage;
import com.python.pydev.analysis.scopeanalysis.AbstractScopeAnalyzerVisitor;

/**
 * this visitor marks the used/ unused tokens and generates the messages related
 * 
 * @author Fabio
 */
public class OccurrencesVisitor extends AbstractScopeAnalyzerVisitor{

    
    /**
     * Used to manage the messages
     */
    protected MessagesManager messagesManager;


    /**
     * used to check for duplication in signatures
     */
    protected DuplicationChecker duplicationChecker;
    
    /**
     * used to check if a signature from a method starts with self (if it is not a staticmethod)
     */
    protected NoSelfChecker noSelfChecker;
    
    public OccurrencesVisitor(IPythonNature nature, String moduleName, IModule current, IAnalysisPreferences prefs, IDocument document, IProgressMonitor monitor) {
        super(nature, moduleName, current, document, monitor);
        this.messagesManager = new MessagesManager(prefs, moduleName, document);
        this.duplicationChecker = new DuplicationChecker(this);
        this.noSelfChecker = new NoSelfChecker(this, moduleName);
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
    	Object ret = super.visitAssign(node);
        noSelfChecker.visitAssign(node);
    	return ret;
    }
    
    /**
     * @return the generated messages.
     */
    public IMessage[] getMessages() {
        endScope(null); //have to end the scope that started when we created the class.
        
        return messagesManager.getMessages();
    }
    
    /**
     * @param foundTok
     */
    protected void onAddUndefinedVarInImportMessage(IToken foundTok) {
        messagesManager.addUndefinedVarInImportMessage(foundTok, foundTok.getRepresentation());
    }
    /**
     * @param token
     */
    protected void onAddUndefinedMessage(IToken token) {
        //global scope, so, even if it is defined later, this is an error...
        messagesManager.addUndefinedMessage(token);
    }
    /**
     * @param m
     */
    protected void onLastScope(ScopeItems m) {
        for(Found n : probablyNotDefined){
            String rep = n.getSingle().tok.getRepresentation();
            Map<String, IToken> lastInStack = m.namesToIgnore;
            if(!scope.findInNamesToIgnore(rep, lastInStack)){
                messagesManager.addUndefinedMessage(n.getSingle().tok);
            }
        }
        messagesManager.setLastScope(m);
    }
    /**
     * @param reportUnused
     * @param m
     */
    protected void onAfterEndScope(boolean reportUnused, ScopeItems m) {
        if(reportUnused){
            //so, now, we clear the unused
            int scopeType = m.getScopeType();
            for (Found f : m.values()) {
                if(!f.isUsed()){
                    // we don't get unused at the global scope or class definition scope unless it's an import
                    if(scopeType == Scope.SCOPE_TYPE_METHOD || f.isImport()){ //only within methods do we put things as unused 
                        messagesManager.addUnusedMessage(f);
                    }
                }
            }
        }
    }

    @Override
	protected void onAfterStartScope(int newScopeType, SimpleNode node) {
		if(newScopeType == Scope.SCOPE_TYPE_CLASS){
	        duplicationChecker.beforeClassDef((ClassDef) node);
	        noSelfChecker.beforeClassDef((ClassDef) node);
	        
        }else if(newScopeType == Scope.SCOPE_TYPE_METHOD){
	        duplicationChecker.beforeFunctionDef((FunctionDef) node); //duplication checker
	        noSelfChecker.beforeFunctionDef((FunctionDef) node);
        }
	}
    
	@Override
	protected void onBeforeEndScope(SimpleNode node) {
		if(node instanceof ClassDef){
	        noSelfChecker.afterClassDef((ClassDef) node);
	        duplicationChecker.afterClassDef((ClassDef) node);
	        
    	} else if(node instanceof FunctionDef){
            duplicationChecker.afterFunctionDef((FunctionDef) node);//duplication checker
            noSelfChecker.afterFunctionDef((FunctionDef) node);
    	}
	}

    @Override
    public void onAddUnusedMessage(Found found) {
        messagesManager.addUnusedMessage(found);
    }

    @Override
    public void onAddReimportMessage(Found newFound) {
        messagesManager.addReimportMessage(newFound);
    }

    @Override
    public void onAddUnresolvedImport(IToken token) {
        messagesManager.addMessage(IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT, token);
    }

    @Override
    public void onAddDuplicatedSignature(SourceToken token, String name) {
        messagesManager.addMessage(IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE, token, name );
    }

    @Override
    public void onAddNoSelf(SourceToken token, Object[] objects) {
        messagesManager.addMessage(IAnalysisPreferences.TYPE_NO_SELF, token, objects);
   }
}
