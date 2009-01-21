package org.python.pydev.plugin;

import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * Helper for knowing about keybindings and related actions
 *
 * @author Fabio
 */
public class KeyBindingHelper {

    //pre-defined helpers
    /**
     * @return true if the given event matches a content assistant keystroke (and false otherwise).
     */
    public static boolean matchesContentAssistKeybinding(KeyEvent event) {
        return matchesKeybinding(event, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
    }
    

    /**
     * @return the key sequence that is the best match for a content assist request.
     */
    public static KeySequence getContentAssistProposalBinding() {
        return getCommandKeyBinding(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
    }
    
    /**
     * @return true if the given event matches a quick assistant keystroke (and false otherwise).
     */
    public static boolean matchesQuickAssistKeybinding(KeyEvent event) {
        return matchesKeybinding(event, ITextEditorActionDefinitionIds.QUICK_ASSIST);
    }
    
    
    /**
     * @return the key sequence that is the best match for a quick assist request.
     */
    public static KeySequence getQuickAssistProposalBinding() {
        return getCommandKeyBinding(ITextEditorActionDefinitionIds.QUICK_ASSIST);
    }
    //END pre-defined helpers
    
    
    
    /**
     * @param event the key event to be checked 
     * @param commandId the command to be checked
     * @return true if the given key event can trigger the passed command (and false otherwise).
     */
    public static boolean matchesKeybinding(KeyEvent event, String commandId) {
        final IBindingService bindingSvc = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
        TriggerSequence[] activeBindingsFor = bindingSvc.getActiveBindingsFor(commandId);
        
        for(TriggerSequence seq:activeBindingsFor){
            if(seq instanceof KeySequence){
                KeySequence keySequence = (KeySequence) seq;
                KeyStroke[] keyStrokes = keySequence.getKeyStrokes();
                
                for (KeyStroke keyStroke : keyStrokes) {
                    
                    if(keyStroke.getNaturalKey() == event.keyCode && (keyStroke.getModifierKeys() & event.stateMask)!=0){
                        
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    
    /**
     * @param commandId the command we want to know about
     * @return the 'best' key sequence that will activate the given command
     */
    public static KeySequence getCommandKeyBinding(String commandId) {
        final IBindingService bindingSvc = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
        TriggerSequence binding = bindingSvc.getBestActiveBindingFor(commandId);
        if (binding instanceof KeySequence){
            return (KeySequence) binding;
        }
        
        return null;
    }

}
