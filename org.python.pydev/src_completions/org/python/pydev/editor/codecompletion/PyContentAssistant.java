/*
 * Created on Aug 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.docutils.StringUtils;

/**
 * @author Fabio Zadrozny
 */
public class PyContentAssistant extends ContentAssistant{

    private boolean lastAutoActivated;

    public PyContentAssistant(){
        this.enableAutoInsert(true);
        this.lastAutoActivated = true;
    }
    
    @Override
    public String showPossibleCompletions() {
        lastAutoActivated = false;
        try {
            return super.showPossibleCompletions();
        } catch (RuntimeException e) {
            Throwable e1 = e;
            while(e1.getCause() != null){
                e1 = e1.getCause();
            }
            if(e1 instanceof JDTNotAvailableException){
                return e1.getMessage();
            }
            throw e;
        }
    }
    
    public boolean getLastCompletionAutoActivated(){
        boolean r = lastAutoActivated;
        lastAutoActivated = true;
        return r;
    }

    public void setIterationStatusMessage(String string) {
        setStatusMessage(StringUtils.format(string, getIterationGesture()));
    }
    
    private String getIterationGesture() {
        TriggerSequence binding = getContentAssistProposalBinding();
        return binding != null ? binding.format(): "completion key";
    }

    /**
     * @return the keysequence that should be used for a content assist request.
     */
    public static KeySequence getContentAssistProposalBinding() {
        final IBindingService bindingSvc = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
        TriggerSequence binding = bindingSvc.getBestActiveBindingFor(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        if (binding instanceof KeySequence)
            return (KeySequence) binding;
        return null;
    }

    /**
     * @return true if the given event matches a content assistant keystroke (and false otherwise).
     */
    public static boolean matchesContentAssistKeybinding(KeyEvent event) {
        KeySequence keySequence = getContentAssistProposalBinding();
        KeyStroke[] keyStrokes = keySequence.getKeyStrokes();
        
        
        for (KeyStroke keyStroke : keyStrokes) {
            
            if(keyStroke.getNaturalKey() == event.keyCode && (keyStroke.getModifierKeys() & event.stateMask)!=0){
                return true;
            }
        }
        
        return false;
    }
    
    

}
