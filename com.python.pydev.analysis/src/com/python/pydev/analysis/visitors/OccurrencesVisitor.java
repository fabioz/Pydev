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

    public OccurrencesVisitor(IPythonNature nature, String moduleName, IModule current, IAnalysisPreferences prefs, IDocument document, IProgressMonitor monitor) {
        super(nature, moduleName, current, document, monitor);
        this.messagesManager = new MessagesManager(prefs, moduleName, document);

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
    protected void afterEndScope(boolean reportUnused, ScopeItems m) {
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
