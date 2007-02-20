/*
 * Created on Aug 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
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
        return super.showPossibleCompletions();
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
        TriggerSequence binding= getIterationBinding();
        return binding != null ? binding.format(): "completion key";
    }

    private KeySequence getIterationBinding() {
        final IBindingService bindingSvc= (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
        TriggerSequence binding= bindingSvc.getBestActiveBindingFor(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        if (binding instanceof KeySequence)
            return (KeySequence) binding;
        return null;
    }

}
